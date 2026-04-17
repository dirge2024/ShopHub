package com.shophub.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderEvent {

    private Long orderId;
    private Long userId;
    private Long voucherId;
    private Integer retryCount;
    private LocalDateTime createdAt;
}
