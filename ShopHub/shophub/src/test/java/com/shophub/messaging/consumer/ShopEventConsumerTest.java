package com.shophub.messaging.consumer;

import com.shophub.messaging.event.CacheEvictEvent;
import com.shophub.messaging.event.SeckillOrderEvent;
import com.shophub.messaging.producer.ShopEventProducer;
import com.shophub.service.CacheEvictService;
import com.shophub.service.VoucherOrderTransactionalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopEventConsumerTest {

    @InjectMocks
    private ShopEventConsumer shopEventConsumer;

    @Mock
    private VoucherOrderTransactionalService voucherOrderTransactionalService;

    @Mock
    private CacheEvictService cacheEvictService;

    @Mock
    private ShopEventProducer shopEventProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(shopEventConsumer, "cacheEvictMaxRetry", 3);
        ReflectionTestUtils.setField(shopEventConsumer, "seckillOrderMaxRetry", 3);
    }

    @Test
    void handleSeckillOrderShouldRetryWhenRetryCountNotReached() {
        SeckillOrderEvent event = new SeckillOrderEvent(1L, 2L, 3L, 1, "old-time");
        doThrow(new RuntimeException("consume failed"))
                .when(voucherOrderTransactionalService)
                .createSeckillOrder(event);
        when(shopEventProducer.sendSeckillOrder(event)).thenReturn(true);

        shopEventConsumer.handleSeckillOrder(event);

        assertEquals(Integer.valueOf(2), event.getRetryCount());
        assertNotNull(event.getCreatedAt());
        verify(shopEventProducer).sendSeckillOrder(event);
        verify(shopEventProducer, never()).sendSeckillOrderDeadLetter(event);
    }

    @Test
    void handleSeckillOrderShouldSendDeadLetterWhenRetryCountReached() {
        SeckillOrderEvent event = new SeckillOrderEvent(1L, 2L, 3L, 3, "old-time");
        doThrow(new RuntimeException("consume failed"))
                .when(voucherOrderTransactionalService)
                .createSeckillOrder(event);
        when(shopEventProducer.sendSeckillOrderDeadLetter(event)).thenReturn(true);

        shopEventConsumer.handleSeckillOrder(event);

        verify(shopEventProducer).sendSeckillOrderDeadLetter(event);
        verify(shopEventProducer, never()).sendSeckillOrder(event);
    }

    @Test
    void handleCacheEvictShouldRetryWhenRetryCountNotReached() {
        CacheEvictEvent event = new CacheEvictEvent("shop", "shop:1", "1", 1, null);
        when(cacheEvictService.retryEvict(event)).thenReturn(false);
        when(shopEventProducer.sendCacheEvict(event)).thenReturn(true);

        shopEventConsumer.handleCacheEvict(event);

        assertEquals(Integer.valueOf(2), event.getRetryCount());
        assertNotNull(event.getCreatedAt());
        verify(shopEventProducer).sendCacheEvict(event);
        verify(shopEventProducer, never()).sendCacheEvictDeadLetter(event);
    }

    @Test
    void handleCacheEvictShouldSendDeadLetterWhenRetryCountReached() {
        CacheEvictEvent event = new CacheEvictEvent("shop", "shop:1", "1", 3, LocalDateTime.now());
        when(cacheEvictService.retryEvict(event)).thenReturn(false);
        when(shopEventProducer.sendCacheEvictDeadLetter(event)).thenReturn(true);

        shopEventConsumer.handleCacheEvict(event);

        verify(shopEventProducer).sendCacheEvictDeadLetter(event);
        verify(shopEventProducer, never()).sendCacheEvict(event);
    }

    @Test
    void handleCacheEvictShouldReturnWhenRetrySucceeds() {
        CacheEvictEvent event = new CacheEvictEvent("shop", "shop:1", "1", 1, LocalDateTime.now());
        when(cacheEvictService.retryEvict(event)).thenReturn(true);

        shopEventConsumer.handleCacheEvict(event);

        verify(shopEventProducer, never()).sendCacheEvict(event);
        verify(shopEventProducer, never()).sendCacheEvictDeadLetter(event);
    }
}
