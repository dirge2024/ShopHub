package com.shophub.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.shophub.entity.Shop;
import com.shophub.entity.ShopType;
import com.shophub.service.LocalCacheService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class LocalCacheServiceImpl implements LocalCacheService {

    public static final String SHOP_CACHE = "shop";
    public static final String SHOP_TYPE_CACHE = "shopType";

    @Resource
    private Cache<Long, Shop> shopLocalCache;

    @Resource
    private Cache<String, List<ShopType>> shopTypeLocalCache;

    @Override
    public Shop getShop(Long shopId) {
        return shopLocalCache.getIfPresent(shopId);
    }

    @Override
    public void putShop(Shop shop) {
        if (shop == null || shop.getId() == null) {
            return;
        }
        shopLocalCache.put(shop.getId(), shop);
    }

    @Override
    public void evictShop(Long shopId) {
        if (shopId == null) {
            return;
        }
        shopLocalCache.invalidate(shopId);
    }

    @Override
    public List<ShopType> getShopTypeList() {
        return shopTypeLocalCache.getIfPresent(SHOP_TYPE_LIST_KEY);
    }

    @Override
    public void putShopTypeList(List<ShopType> shopTypes) {
        if (shopTypes == null || shopTypes.isEmpty()) {
            return;
        }
        shopTypeLocalCache.put(SHOP_TYPE_LIST_KEY, shopTypes);
    }

    @Override
    public void evictShopTypeList() {
        shopTypeLocalCache.invalidate(SHOP_TYPE_LIST_KEY);
    }

    @Override
    public void evict(String cacheName, String localKey) {
        if (SHOP_CACHE.equals(cacheName) && localKey != null) {
            evictShop(Long.valueOf(localKey));
            return;
        }
        if (SHOP_TYPE_CACHE.equals(cacheName)) {
            evictShopTypeList();
        }
    }
}
