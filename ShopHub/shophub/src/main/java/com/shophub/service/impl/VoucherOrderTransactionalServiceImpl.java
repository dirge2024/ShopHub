package com.shophub.service.impl;

import com.shophub.entity.VoucherOrder;
import com.shophub.messaging.event.SeckillOrderEvent;
import com.shophub.service.ISeckillVoucherService;
import com.shophub.service.IVoucherOrderService;
import com.shophub.service.VoucherOrderTransactionalService;
import com.shophub.utils.VoucherOrderStatusConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Slf4j
@Service
public class VoucherOrderTransactionalServiceImpl implements VoucherOrderTransactionalService {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Override
    @Transactional
    public void createSeckillOrder(SeckillOrderEvent event) {
        int count = voucherOrderService.query()
                .eq("user_id", event.getUserId())
                .eq("voucher_id", event.getVoucherId())
                .count();
        if (count > 0) {
            log.warn("秒杀订单重复, userId={}, voucherId={}", event.getUserId(), event.getVoucherId());
            return;
        }

        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", event.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!success) {
            log.warn("秒杀订单落库失败, 库存不足, voucherId={}", event.getVoucherId());
            return;
        }

        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(event.getOrderId());
        voucherOrder.setUserId(event.getUserId());
        voucherOrder.setVoucherId(event.getVoucherId());
        voucherOrder.setStatus(VoucherOrderStatusConstants.UNPAID);
        voucherOrder.setPayType(2);
        voucherOrder.setVersion(0);
        try {
            voucherOrderService.save(voucherOrder);
        } catch (DuplicateKeyException e) {
            log.warn("秒杀订单重复落库被唯一索引拦截, userId={}, voucherId={}", event.getUserId(), event.getVoucherId());
            throw e;
        }
    }
}
