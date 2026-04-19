package com.shophub.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shophub.dto.Result;
import com.shophub.entity.VoucherOrder;
import com.shophub.entity.SeckillVoucher;
import com.shophub.mapper.VoucherOrderMapper;
import com.shophub.messaging.event.SeckillOrderEvent;
import com.shophub.messaging.producer.ShopEventProducer;
import com.shophub.service.ISeckillVoucherService;
import com.shophub.service.IVoucherOrderService;
import com.shophub.service.VoucherOrderTransactionalService;
import com.shophub.utils.RedisIdWorker;
import com.shophub.utils.UserHolder;
import com.shophub.utils.VoucherOrderStatus;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.shophub.utils.RedisConstants.SECKILL_STOCK_KEY;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    private static final DefaultRedisScript<Long> SECKILL_ROLLBACK_SCRIPT;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ShopEventProducer shopEventProducer;

    @Resource
    private VoucherOrderTransactionalService voucherOrderTransactionalService;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);

        SECKILL_ROLLBACK_SCRIPT = new DefaultRedisScript<>();
        SECKILL_ROLLBACK_SCRIPT.setLocation(new ClassPathResource("seckill_rollback.lua"));
        SECKILL_ROLLBACK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 兜底初始化 Redis 库存，保证直接插入数据库的测试秒杀券也能复用同一套 Lua 抢购链路。
        Result initResult = initSeckillStockIfAbsent(voucherId);
        if (initResult != null) {
            return initResult;
        }

        Long userId = UserHolder.getUser().getId();
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
        int r = result.intValue();
        if (r == 1) {
            return Result.fail("库存不足");
        }
        if (r == 2) {
            return Result.fail("用户已经购买过一次了");
        }

        Long orderId = redisIdWorker.nextID("order");
        boolean sent = shopEventProducer.sendSeckillOrder(new SeckillOrderEvent(
                orderId,
                userId,
                voucherId,
                0,
                LocalDateTime.now().toString()
        ));
        if (!sent) {
            rollbackSeckillReservation(voucherId, userId);
            return Result.fail("下单人数较多，请稍后重试");
        }
        return Result.ok(orderId);
    }

    @Override
    @Transactional
    public Result paySuccess(Long orderId, Integer payType) {
        VoucherOrder voucherOrder = getById(orderId);
        if (voucherOrder == null) {
            return Result.fail("订单不存在");
        }
        VoucherOrderStatus currentStatus = VoucherOrderStatus.of(voucherOrder.getStatus());
        if (VoucherOrderStatus.PAID == currentStatus) {
            return Result.ok("订单已支付");
        }
        if (VoucherOrderStatus.UNPAID != currentStatus) {
            return Result.fail("当前订单状态不允许支付");
        }
        return updateOrderStatus(voucherOrder, VoucherOrderStatus.PAID, payType, "支付成功");
    }

    @Override
    @Transactional
    public Result closeTimeoutOrder(Long orderId, Integer timeoutMinutes) {
        VoucherOrder voucherOrder = getById(orderId);
        if (voucherOrder == null) {
            return Result.fail("订单不存在");
        }
        VoucherOrderStatus currentStatus = VoucherOrderStatus.of(voucherOrder.getStatus());
        if (VoucherOrderStatus.CANCELED == currentStatus) {
            return Result.ok("订单已取消");
        }
        if (VoucherOrderStatus.UNPAID != currentStatus) {
            return Result.fail("当前订单状态不允许关单");
        }
        if (voucherOrder.getCreateTime() == null ||
                voucherOrder.getCreateTime().plusMinutes(timeoutMinutes).isAfter(LocalDateTime.now())) {
            return Result.fail("订单未超时");
        }
        return updateOrderStatus(voucherOrder, VoucherOrderStatus.CANCELED, null, "超时关单成功");
    }

    @Override
    public int scanTimeoutOrders(Integer timeoutMinutes, Integer batchSize) {
        LocalDateTime timeoutLine = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<VoucherOrder> timeoutOrders = query()
                .eq("status", VoucherOrderStatus.UNPAID.getCode())
                .le("create_time", timeoutLine)
                .last("limit " + batchSize)
                .list();
        if (timeoutOrders == null || timeoutOrders.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (VoucherOrder voucherOrder : timeoutOrders) {
            Result result = closeTimeoutOrder(voucherOrder.getId(), timeoutMinutes);
            if (Boolean.TRUE.equals(result.getSuccess())) {
                successCount++;
            }
        }
        return successCount;
    }

    private Result updateOrderStatus(VoucherOrder voucherOrder, VoucherOrderStatus targetStatus, Integer payType, String successMessage) {
        VoucherOrder updateOrder = new VoucherOrder();
        updateOrder.setId(voucherOrder.getId());
        updateOrder.setStatus(targetStatus.getCode());
        updateOrder.setVersion(voucherOrder.getVersion());
        if (payType != null) {
            updateOrder.setPayType(payType);
            updateOrder.setPayTime(LocalDateTime.now());
        }
        boolean success = updateById(updateOrder);
        if (!success) {
            return Result.fail("订单状态已变更，请刷新后重试");
        }
        return Result.ok(successMessage);
    }

    private void rollbackSeckillReservation(Long voucherId, Long userId) {
        stringRedisTemplate.execute(
                SECKILL_ROLLBACK_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
    }

    private Result initSeckillStockIfAbsent(Long voucherId) {
        String stockKey = SECKILL_STOCK_KEY + voucherId;
        Boolean hasKey = stringRedisTemplate.hasKey(stockKey);
        if (Boolean.TRUE.equals(hasKey)) {
            return null;
        }

        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher == null) {
            return Result.fail("秒杀券不存在");
        }
        if (seckillVoucher.getStock() == null || seckillVoucher.getStock() < 1) {
            return Result.fail("库存不足");
        }

        // 这里只做一次性库存预热，后续扣减全部交给 Lua 保证原子性。
        stringRedisTemplate.opsForValue().setIfAbsent(
                stockKey,
                seckillVoucher.getStock().toString(),
                30,
                TimeUnit.DAYS
        );
        return null;
    }
}


