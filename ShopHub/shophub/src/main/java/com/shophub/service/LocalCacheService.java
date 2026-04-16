package com.shophub.service;

import com.shophub.entity.Shop;
import com.shophub.entity.ShopType;

import java.util.List;

public interface LocalCacheService {

    String SHOP_TYPE_LIST_KEY = "shop:type:list";

    Shop getShop(Long shopId);

    void putShop(Shop shop);

    void evictShop(Long shopId);

    List<ShopType> getShopTypeList();

    void putShopTypeList(List<ShopType> shopTypes);

    void evictShopTypeList();

    void evict(String cacheName, String localKey);
}
