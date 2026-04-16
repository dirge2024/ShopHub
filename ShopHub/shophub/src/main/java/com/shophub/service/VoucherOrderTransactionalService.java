package com.shophub.service;

import com.shophub.messaging.event.SeckillOrderEvent;

public interface VoucherOrderTransactionalService {

    void createSeckillOrder(SeckillOrderEvent event);
}
