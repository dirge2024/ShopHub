package com.shophub.service.impl;

import com.shophub.dto.Result;
import com.shophub.entity.Shop;
import com.shophub.messaging.event.CacheEvictEvent;
import com.shophub.messaging.producer.ShopEventProducer;
import com.shophub.service.CacheEvictService;
import com.shophub.service.LocalCacheService;
import com.shophub.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.shophub.service.impl.LocalCacheServiceImpl.SHOP_CACHE;
import static com.shophub.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.shophub.utils.RedisConstants.CACHE_SHOP_TTL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @Spy
    @InjectMocks
    private ShopServiceImpl shopService;

    @Mock
    private CacheClient cacheClient;

    @Mock
    private LocalCacheService localCacheService;

    @Mock
    private CacheEvictService cacheEvictService;

    @Mock
    private ShopEventProducer shopEventProducer;

    @Test
    void queryByIdShouldReturnLocalCacheDirectly() {
        Shop localShop = buildShop(1L);
        when(localCacheService.getShop(1L)).thenReturn(localShop);

        Result result = shopService.queryById(1L);

        assertTrue(result.getSuccess());
        assertSame(localShop, result.getData());
        verify(cacheClient, never()).queryLogicalExpire(any(), any(), any(), any(), anyLong(), any());
        verify(localCacheService, never()).putShop(any(Shop.class));
    }

    @Test
    void queryByIdShouldFallbackToCacheClientAndRefreshLocalCache() {
        Shop dbShop = buildShop(2L);
        when(localCacheService.getShop(2L)).thenReturn(null);
        when(cacheClient.queryLogicalExpire(
                eq(CACHE_SHOP_KEY),
                eq(2L),
                eq(Shop.class),
                any(),
                eq(CACHE_SHOP_TTL),
                any()
        )).thenReturn(dbShop);

        Result result = shopService.queryById(2L);

        assertTrue(result.getSuccess());
        assertSame(dbShop, result.getData());
        verify(localCacheService).putShop(dbShop);
    }

    @Test
    void queryByIdShouldFailWhenShopMissing() {
        when(localCacheService.getShop(3L)).thenReturn(null);
        when(cacheClient.queryLogicalExpire(
                eq(CACHE_SHOP_KEY),
                eq(3L),
                eq(Shop.class),
                any(),
                eq(CACHE_SHOP_TTL),
                any()
        )).thenReturn(null);

        Result result = shopService.queryById(3L);

        assertFalse(result.getSuccess());
        assertEquals("店铺不存在", result.getErrorMsg());
        verify(localCacheService, never()).putShop(any(Shop.class));
    }

    @Test
    void updateShouldFailWhenShopIdMissing() {
        Shop shop = new Shop();

        Result result = shopService.update(shop);

        assertFalse(result.getSuccess());
        assertEquals("点评id不存在", result.getErrorMsg());
        verify(shopService, never()).updateById(any(Shop.class));
        verify(cacheEvictService, never()).evictShopCache(anyLong());
    }

    @Test
    void updateShouldEvictCacheWithoutSendingCompensationWhenDeleteSucceeds() {
        Shop shop = buildShop(4L);
        doReturn(true).when(shopService).updateById(shop);
        when(cacheEvictService.evictShopCache(4L)).thenReturn(true);

        Result result = shopService.update(shop);

        assertTrue(result.getSuccess());
        verify(cacheEvictService).evictShopCache(4L);
        verify(shopEventProducer, never()).sendCacheEvict(any(CacheEvictEvent.class));
    }

    @Test
    void updateShouldSendCompensationWhenCacheEvictFails() {
        Shop shop = buildShop(5L);
        doReturn(true).when(shopService).updateById(shop);
        when(cacheEvictService.evictShopCache(5L)).thenReturn(false);
        when(shopEventProducer.sendCacheEvict(any(CacheEvictEvent.class))).thenReturn(true);

        Result result = shopService.update(shop);

        assertTrue(result.getSuccess());

        ArgumentCaptor<CacheEvictEvent> captor = ArgumentCaptor.forClass(CacheEvictEvent.class);
        verify(shopEventProducer).sendCacheEvict(captor.capture());

        CacheEvictEvent event = captor.getValue();
        assertEquals(SHOP_CACHE, event.getCacheName());
        assertEquals(CACHE_SHOP_KEY + "5", event.getRedisKey());
        assertEquals("5", event.getLocalKey());
        assertEquals(Integer.valueOf(1), event.getRetryCount());
        assertNotNull(event.getCreatedAt());
    }

    private Shop buildShop(Long id) {
        Shop shop = new Shop();
        shop.setId(id);
        shop.setName("shop-" + id);
        return shop;
    }
}
