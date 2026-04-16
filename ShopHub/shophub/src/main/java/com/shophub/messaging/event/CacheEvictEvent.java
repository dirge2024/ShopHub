package com.shophub.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheEvictEvent {

    private String cacheName;
    private String redisKey;
    private String localKey;
    private Integer retryCount;
    private LocalDateTime createdAt;
}
