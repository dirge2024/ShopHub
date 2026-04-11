package com.shophub.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 核心连接配置
        config.useSingleServer()
                .setAddress("redis://192.168.12.100:6379")
                .setPassword("wsy2642")
                .setDatabase(0)
                // 网络超时与重试配置（保留核心容错参数）
                .setConnectTimeout(15000)
                .setTimeout(10000)
                .setRetryAttempts(5)
                .setRetryInterval(2000)
                .setPingConnectionInterval(60000)
                // 连接池参数（与SpringBoot lettuce配置对齐）
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(1);

        // 创建客户端（保留基础异常抛出，便于启动时发现问题）
        return Redisson.create(config);
    }
}

