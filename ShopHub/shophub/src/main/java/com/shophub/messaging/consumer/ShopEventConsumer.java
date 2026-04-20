package com.shophub.messaging.consumer;

import com.shophub.messaging.MQTopics;
import com.shophub.messaging.event.CacheEvictEvent;
import com.shophub.messaging.event.SeckillOrderEvent;
import com.shophub.messaging.producer.ShopEventProducer;
import com.shophub.service.CacheEvictService;
import com.shophub.service.MqGovernanceService;
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

    private static final int INITIAL_RETRY_COUNT = 0;
    private static final String SCENE_SECKILL_ORDER = "seckill_order";
    private static final String SCENE_CACHE_EVICT = "cache_evict";

    @Resource
    private VoucherOrderTransactionalService voucherOrderTransactionalService;

    @Resource
    private CacheEvictService cacheEvictService;

    @Resource
    private ShopEventProducer shopEventProducer;

    @Resource
    private MqGovernanceService mqGovernanceService;

    @Value("${shophub.mq.cache-evict-max-retry:3}")
    private int cacheEvictMaxRetry;

    @Value("${shophub.mq.seckill-order-max-retry:3}")
    private int seckillOrderMaxRetry;

    /**
     * 处理秒杀下单消息：
     * 业务落库失败时先记录治理信息，再按重试阈值决定重发还是投死信。
     */
    @KafkaListener(topics = MQTopics.SECKILL_ORDER_TOPIC, groupId = "shophub-seckill-order-group")
    public void handleSeckillOrder(SeckillOrderEvent event) {
        try {
            voucherOrderTransactionalService.createSeckillOrder(event);
        } catch (Exception e) {
            int currentRetry = event.getRetryCount() == null ? INITIAL_RETRY_COUNT : event.getRetryCount();
            log.error("秒杀订单消费失败, orderId={}, retry={}", event.getOrderId(), currentRetry, e);
            if (currentRetry >= seckillOrderMaxRetry) {
                mqGovernanceService.recordDeadLetter(
                        SCENE_SECKILL_ORDER,
                        String.valueOf(event.getOrderId()),
                        event,
                        currentRetry,
                        e.getMessage()
                );
                boolean deadLetterSent = shopEventProducer.sendSeckillOrderDeadLetter(event);
                if (!deadLetterSent) {
                    log.error("秒杀订单死信投递失败, orderId={}", event.getOrderId());
                }
                return;
            }

            // 重试前刷新重试次数和时间，便于后续消费链路识别当前消息状态。
            event.setRetryCount(currentRetry + 1);
            event.setCreatedAt(LocalDateTime.now().toString());
            mqGovernanceService.recordRetry(
                    SCENE_SECKILL_ORDER,
                    String.valueOf(event.getOrderId()),
                    event,
                    event.getRetryCount(),
                    e.getMessage()
            );
            boolean retrySent = shopEventProducer.sendSeckillOrder(event);
            if (!retrySent) {
                mqGovernanceService.recordDeadLetter(
                        SCENE_SECKILL_ORDER,
                        String.valueOf(event.getOrderId()),
                        event,
                        event.getRetryCount(),
                        "重试发送失败"
                );
                boolean deadLetterSent = shopEventProducer.sendSeckillOrderDeadLetter(event);
                if (!deadLetterSent) {
                    log.error("秒杀订单重试与死信投递均失败, orderId={}", event.getOrderId());
                }
            }
        }
    }

    /**
     * 处理缓存补偿消息：
     * 删除成功即结束，删除失败则进入有限次重试，超过阈值后写入死信。
     */
    @KafkaListener(topics = MQTopics.CACHE_EVICT_TOPIC, groupId = "shophub-cache-evict-group")
    public void handleCacheEvict(CacheEvictEvent event) {
        boolean success = cacheEvictService.retryEvict(event);
        if (success) {
            return;
        }

        int currentRetry = event.getRetryCount() == null ? INITIAL_RETRY_COUNT : event.getRetryCount();
        if (currentRetry >= cacheEvictMaxRetry) {
            mqGovernanceService.recordDeadLetter(
                    SCENE_CACHE_EVICT,
                    event.getRedisKey(),
                    event,
                    currentRetry,
                    "缓存补偿达到最大重试次数"
            );
            boolean deadLetterSent = shopEventProducer.sendCacheEvictDeadLetter(event);
            if (!deadLetterSent) {
                log.error("缓存补偿死信投递失败, key={}", event.getRedisKey());
            }
            return;
        }

        // 缓存补偿先做有限次重试，超过阈值后再转入死信队列。
        event.setRetryCount(currentRetry + 1);
        event.setCreatedAt(LocalDateTime.now());
        mqGovernanceService.recordRetry(
                SCENE_CACHE_EVICT,
                event.getRedisKey(),
                event,
                event.getRetryCount(),
                "缓存补偿删除失败"
        );
        boolean retrySent = shopEventProducer.sendCacheEvict(event);
        if (!retrySent) {
            mqGovernanceService.recordDeadLetter(
                    SCENE_CACHE_EVICT,
                    event.getRedisKey(),
                    event,
                    event.getRetryCount(),
                    "缓存补偿重试发送失败"
            );
            boolean deadLetterSent = shopEventProducer.sendCacheEvictDeadLetter(event);
            if (!deadLetterSent) {
                log.error("缓存补偿重试与死信投递均失败, key={}", event.getRedisKey());
            }
        }
    }

    /**
     * 死信监听只负责记录日志，真正的人工排查和重放入口交给治理接口处理。
     */
    @KafkaListener(topics = MQTopics.SECKILL_ORDER_DLT_TOPIC, groupId = "shophub-seckill-order-dlt-group")
    public void handleSeckillOrderDeadLetter(SeckillOrderEvent event) {
        log.error("秒杀订单进入死信队列, orderId={}, userId={}, voucherId={}, retry={}",
                event.getOrderId(), event.getUserId(), event.getVoucherId(), event.getRetryCount());
    }

    /**
     * 缓存补偿死信入口，便于后续联调时快速定位失败 key。
     */
    @KafkaListener(topics = MQTopics.CACHE_EVICT_DLT_TOPIC, groupId = "shophub-cache-evict-dlt-group")
    public void handleCacheEvictDeadLetter(CacheEvictEvent event) {
        log.error("缓存补偿进入死信队列, key={}, retry={}", event.getRedisKey(), event.getRetryCount());
    }
}
