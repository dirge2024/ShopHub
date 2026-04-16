package com.shophub.service;

import com.shophub.messaging.event.CacheEvictEvent;

public interface CacheEvictService {

    boolean evictShopCache(Long shopId);

    boolean retryEvict(CacheEvictEvent event);
}
