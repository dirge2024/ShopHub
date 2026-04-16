package com.shophub.messaging.producer;

import com.shophub.messaging.MQTopics;
import com.shophub.messaging.event.CacheEvictEvent;
import com.shophub.messaging.event.SeckillOrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ShopEventProducer {

    private static final long SEND_TIMEOUT_SECONDS = 3L;

    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;

    public boolean sendSeckillOrder(SeckillOrderEvent event) {
        try {
            kafkaTemplate.send(MQTopics.SECKILL_ORDER_TOPIC, String.valueOf(event.getOrderId()), event)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("发送秒杀下单消息失败, orderId={}", event.getOrderId(), e);
            return false;
        }
    }

    public boolean sendCacheEvict(CacheEvictEvent event) {
        try {
            kafkaTemplate.send(MQTopics.CACHE_EVICT_TOPIC, event.getRedisKey(), event)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("已发送缓存补偿消息, key={}, retry={}", event.getRedisKey(), event.getRetryCount());
            return true;
        } catch (Exception e) {
            log.error("发送缓存补偿消息失败, key={}", event.getRedisKey(), e);
            return false;
        }
    }
}
