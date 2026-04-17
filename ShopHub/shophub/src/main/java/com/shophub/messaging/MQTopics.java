package com.shophub.messaging;

public final class MQTopics {

    private MQTopics() {
    }

    public static final String SECKILL_ORDER_TOPIC = "shophub.seckill.order";
    public static final String CACHE_EVICT_TOPIC = "shophub.cache.evict";
    public static final String SECKILL_ORDER_DLT_TOPIC = "shophub.seckill.order.dlt";
    public static final String CACHE_EVICT_DLT_TOPIC = "shophub.cache.evict.dlt";
}
