package com.shophub.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shophub.entity.Shop;
import com.shophub.entity.ShopType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineCacheConfig {

    @Bean
    public Cache<Long, Shop> shopLocalCache(
            @Value("${shophub.cache.local-shop-ttl-minutes:3}") long ttlMinutes) {
        return Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public Cache<String, List<ShopType>> shopTypeLocalCache(
            @Value("${shophub.cache.local-shop-type-ttl-minutes:10}") long ttlMinutes) {
        return Caffeine.newBuilder()
                .maximumSize(32)
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .build();
    }
}
