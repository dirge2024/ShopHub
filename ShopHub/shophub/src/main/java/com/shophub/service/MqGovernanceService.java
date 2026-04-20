package com.shophub.service;

import com.shophub.dto.MqGovernanceOverviewDTO;
import com.shophub.dto.MqGovernanceRecordDTO;
import com.shophub.dto.Result;

import java.util.List;

public interface MqGovernanceService {

    void recordRetry(String scene, String businessKey, Object payload, Integer retryCount, String errorMessage);

    void recordDeadLetter(String scene, String businessKey, Object payload, Integer retryCount, String errorMessage);

    MqGovernanceOverviewDTO getOverview(int limit);

    List<MqGovernanceRecordDTO> listRecords(String scene, String stage, int limit);

    Result replayDeadLetter(String recordId);
}
