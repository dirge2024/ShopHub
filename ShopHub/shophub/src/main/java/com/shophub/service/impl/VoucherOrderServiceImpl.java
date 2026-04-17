package com.shophub.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shophub.dto.Result;
import com.shophub.entity.VoucherOrder;
import com.shophub.mapper.VoucherOrderMapper;
import com.shophub.messaging.event.SeckillOrderEvent;
import com.shophub.messaging.producer.ShopEventProducer;
import com.shophub.service.IVoucherOrderService;
import com.shophub.service.VoucherOrderTransactionalService;
import com.shophub.utils.RedisIdWorker;
import com.shophub.utils.UserHolder;
import com.shophub.utils.VoucherOrderStatusConstants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;

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
                LocalDateTime.now()
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
        if (VoucherOrderStatusConstants.PAID == voucherOrder.getStatus()) {
            return Result.ok("订单已支付");
        }
        if (VoucherOrderStatusConstants.UNPAID != voucherOrder.getStatus()) {
            return Result.fail("当前订单状态不允许支付");
        }

        VoucherOrder updateOrder = new VoucherOrder();
        updateOrder.setId(orderId);
        updateOrder.setStatus(VoucherOrderStatusConstants.PAID);
        updateOrder.setPayType(payType);
        updateOrder.setPayTime(LocalDateTime.now());
        updateOrder.setVersion(voucherOrder.getVersion());
        boolean success = updateById(updateOrder);
        if (!success) {
            return Result.fail("订单状态已变更，请刷新后重试");
        }
        return Result.ok("支付成功");
    }

    private void rollbackSeckillReservation(Long voucherId, Long userId) {
        stringRedisTemplate.execute(
                SECKILL_ROLLBACK_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
    }
}


