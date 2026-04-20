package com.shophub.service.impl;

import com.shophub.messaging.event.CacheEvictEvent;
import com.shophub.service.CacheEvictService;
import com.shophub.service.LocalCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.shophub.utils.RedisConstants.CACHE_SHOP_KEY;

@Slf4j
@Service
public class CacheEvictServiceImpl implements CacheEvictService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private LocalCacheService localCacheService;

    /**
     * 删除单个店铺缓存时，先清理本地缓存，再删除 Redis，尽量缩短双层缓存不一致窗口。
     */
    @Override
    public boolean evictShopCache(Long shopId) {
        if (shopId == null) {
            return true;
        }
        localCacheService.evictShop(shopId);
        return deleteRedisKey(CACHE_SHOP_KEY + shopId);
    }

    /**
     * 消费缓存补偿消息时，按事件里的缓存名称和 key 重新执行一次删除。
     */
    @Override
    public boolean retryEvict(CacheEvictEvent event) {
        if (event == null) {
            return true;
        }
        localCacheService.evict(event.getCacheName(), event.getLocalKey());
        return deleteRedisKey(event.getRedisKey());
    }

    /**
     * Redis 删除失败时返回 false，由上游决定是重试还是转入死信。
     */
    private boolean deleteRedisKey(String key) {
        try {
            stringRedisTemplate.delete(key);
            return true;
        } catch (Exception e) {
            log.warn("删除 Redis 缓存失败, key={}", key, e);
            return false;
        }
    }
}
