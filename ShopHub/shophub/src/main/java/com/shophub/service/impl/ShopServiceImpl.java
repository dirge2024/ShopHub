package com.shophub.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shophub.dto.Result;
import com.shophub.entity.Shop;
import com.shophub.mapper.ShopMapper;
import com.shophub.messaging.event.CacheEvictEvent;
import com.shophub.messaging.producer.ShopEventProducer;
import com.shophub.service.CacheEvictService;
import com.shophub.service.IShopService;
import com.shophub.service.LocalCacheService;
import com.shophub.utils.CacheClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.shophub.service.impl.LocalCacheServiceImpl.SHOP_CACHE;
import static com.shophub.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.shophub.utils.RedisConstants.CACHE_SHOP_TTL;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private CacheClient cacheClient;

    @Resource
    private LocalCacheService localCacheService;

    @Resource
    private CacheEvictService cacheEvictService;

    @Resource
    private ShopEventProducer shopEventProducer;

    /**
     * 根据店铺 id 查询数据：
     * 优先命中本地缓存，未命中再走 Redis 逻辑过期方案，最后把结果回填到本地缓存。
     */
    @Override
    public Result queryById(Long id) {
        Shop localShop = localCacheService.getShop(id);
        if (localShop != null) {
            return Result.ok(localShop);
        }

        Shop shop = cacheClient.queryLogicalExpire(
                CACHE_SHOP_KEY,
                id,
                Shop.class,
                this::getById,
                CACHE_SHOP_TTL,
                TimeUnit.MINUTES
        );
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        localCacheService.putShop(shop);
        return Result.ok(shop);
    }

    /**
     * 更新店铺信息：
     * 先更新数据库，再在事务提交后删除缓存，避免脏数据提前暴露。
     */
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("点评id不存在");
        }
        updateById(shop);
        registerCacheEvictAfterCommit(id);
        return Result.ok();
    }

    /**
     * 如果当前处于事务中，就把删缓存动作延迟到提交后执行；
     * 如果没有事务，直接执行删除，保证调用方不丢缓存清理动作。
     */
    private void registerCacheEvictAfterCommit(Long shopId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evictShopCache(shopId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictShopCache(shopId);
            }
        });
    }

    /**
     * 删除店铺缓存失败时，发送 Kafka 补偿消息给后续消费者重试。
     */
    private void evictShopCache(Long shopId) {
        boolean deleted = cacheEvictService.evictShopCache(shopId);
        if (!deleted) {
            shopEventProducer.sendCacheEvict(new CacheEvictEvent(
                    SHOP_CACHE,
                    CACHE_SHOP_KEY + shopId,
                    String.valueOf(shopId),
                    1,
                    LocalDateTime.now()
            ));
        }
    }
}

