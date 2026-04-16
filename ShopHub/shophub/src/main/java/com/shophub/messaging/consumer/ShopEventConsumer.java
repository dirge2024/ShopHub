package com.shophub.messaging.consumer;

import com.shophub.messaging.MQTopics;
import com.shophub.messaging.event.CacheEvictEvent;
import com.shophub.messaging.event.SeckillOrderEvent;
import com.shophub.messaging.producer.ShopEventProducer;
import com.shophub.service.CacheEvictService;
import com.shophub.service.VoucherOrderTransactionalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Slf4j
@Component
public class ShopEventConsumer {

    @Resource
    private VoucherOrderTransactionalService voucherOrderTransactionalService;

    @Resource
    private CacheEvictService cacheEvictService;

    @Resource
    private ShopEventProducer shopEventProducer;

    @Value("${shophub.mq.cache-evict-max-retry:3}")
    private int cacheEvictMaxRetry;

    @KafkaListener(topics = MQTopics.SECKILL_ORDER_TOPIC, groupId = "shophub-seckill-order-group")
    public void handleSeckillOrder(SeckillOrderEvent event) {
        voucherOrderTransactionalService.createSeckillOrder(event);
    }

    @KafkaListener(topics = MQTopics.CACHE_EVICT_TOPIC, groupId = "shophub-cache-evict-group")
    public void handleCacheEvict(CacheEvictEvent event) {
        boolean success = cacheEvictService.retryEvict(event);
        if (success) {
            return;
        }
        int currentRetry = event.getRetryCount() == null ? 0 : event.getRetryCount();
        if (currentRetry >= cacheEvictMaxRetry) {
            log.error("缓存补偿超过最大重试次数, key={}", event.getRedisKey());
            return;
        }
        event.setRetryCount(currentRetry + 1);
        event.setCreatedAt(LocalDateTime.now());
        shopEventProducer.sendCacheEvict(event);
    }
}
