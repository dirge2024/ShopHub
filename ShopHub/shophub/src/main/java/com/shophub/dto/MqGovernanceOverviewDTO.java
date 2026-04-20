package com.shophub.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MqGovernanceOverviewDTO {

    private Map<String, Long> counters;

    private List<MqGovernanceRecordDTO> recentRetries;

    private List<MqGovernanceRecordDTO> recentDeadLetters;
}
