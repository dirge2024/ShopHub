package com.shophub.messaging.producer;

import com.shophub.messaging.MQTopics;
import com.shophub.messaging.event.CacheEvictEvent;
import com.shophub.messaging.event.SeckillOrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class ShopEventProducer {

    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendSeckillOrder(SeckillOrderEvent event) {
        kafkaTemplate.send(MQTopics.SECKILL_ORDER_TOPIC, String.valueOf(event.getOrderId()), event);
    }

    public void sendCacheEvict(CacheEvictEvent event) {
        kafkaTemplate.send(MQTopics.CACHE_EVICT_TOPIC, event.getRedisKey(), event);
        log.info("已发送缓存补偿消息, key={}, retry={}", event.getRedisKey(), event.getRetryCount());
    }
}
