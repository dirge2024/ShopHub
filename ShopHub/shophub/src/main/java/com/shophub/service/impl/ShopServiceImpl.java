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

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("点评id不存在");
        }
        updateById(shop);

        boolean deleted = cacheEvictService.evictShopCache(id);
        if (!deleted) {
            shopEventProducer.sendCacheEvict(new CacheEvictEvent(
                    SHOP_CACHE,
                    CACHE_SHOP_KEY + id,
                    String.valueOf(id),
                    1,
                    LocalDateTime.now()
            ));
        }
        return Result.ok();
    }
}

