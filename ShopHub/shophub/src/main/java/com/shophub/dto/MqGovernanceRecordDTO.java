package com.shophub.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MqGovernanceRecordDTO {

    private String id;

    private String scene;

    private String stage;

    private String originalTopic;

    private String deadLetterTopic;

    private String businessKey;

    private Integer retryCount;

    private String errorMessage;

    private String payloadJson;

    private String payloadClass;

    private LocalDateTime createdAt;

    private Integer replayCount;

    private Boolean lastReplaySuccess;

    private String lastReplayMessage;

    private LocalDateTime lastReplayTime;
}
