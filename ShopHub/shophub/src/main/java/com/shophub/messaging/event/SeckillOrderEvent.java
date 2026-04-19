package com.shophub.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderEvent {

    // 保持事件载荷结构简单，方便本地排查旧消息和重建测试 Topic。
    private Long orderId;
    private Long userId;
    private Long voucherId;
    private Integer retryCount;
    private String createdAt;
}
