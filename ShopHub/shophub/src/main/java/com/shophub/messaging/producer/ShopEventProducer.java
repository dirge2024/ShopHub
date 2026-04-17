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
        return send(
                MQTopics.SECKILL_ORDER_TOPIC,
                String.valueOf(event.getOrderId()),
                event,
                "发送秒杀下单消息失败, orderId={}",
                event.getOrderId()
        );
    }

    public boolean sendSeckillOrderDeadLetter(SeckillOrderEvent event) {
        return send(
                MQTopics.SECKILL_ORDER_DLT_TOPIC,
                String.valueOf(event.getOrderId()),
                event,
                "发送秒杀死信消息失败, orderId={}",
                event.getOrderId()
        );
    }

    public boolean sendCacheEvict(CacheEvictEvent event) {
        return send(
                MQTopics.CACHE_EVICT_TOPIC,
                event.getRedisKey(),
                event,
                "发送缓存补偿消息失败, key={}",
                event.getRedisKey()
        );
    }

    public boolean sendCacheEvictDeadLetter(CacheEvictEvent event) {
        return send(
                MQTopics.CACHE_EVICT_DLT_TOPIC,
                event.getRedisKey(),
                event,
                "发送缓存补偿死信消息失败, key={}",
                event.getRedisKey()
        );
    }

    private boolean send(String topic, String key, Object event, String errorMessage, Object logKey) {
        try {
            kafkaTemplate.send(topic, key, event).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error(errorMessage, logKey, e);
            return false;
        }
    }
}
