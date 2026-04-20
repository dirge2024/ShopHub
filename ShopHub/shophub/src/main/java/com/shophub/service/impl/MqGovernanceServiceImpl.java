package com.shophub.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.dto.MqGovernanceOverviewDTO;
import com.shophub.dto.MqGovernanceRecordDTO;
import com.shophub.dto.Result;
import com.shophub.messaging.MQTopics;
import com.shophub.messaging.event.CacheEvictEvent;
import com.shophub.messaging.event.SeckillOrderEvent;
import com.shophub.messaging.producer.ShopEventProducer;
import com.shophub.service.MqGovernanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MqGovernanceServiceImpl implements MqGovernanceService {

    private static final String STAGE_RETRY = "retry";
    private static final String STAGE_DEAD_LETTER = "dead_letter";
    private static final String STAGE_REPLAY_SUCCESS = "replay_success";
    private static final String STAGE_REPLAY_FAILED = "replay_failed";

    private static final String SCENE_SECKILL_ORDER = "seckill_order";
    private static final String SCENE_CACHE_EVICT = "cache_evict";

    private static final String COUNTER_KEY = "mq:governance:counters";
    private static final String RECENT_RECORDS_KEY = "mq:governance:recent";
    private static final String RECORD_KEY_PREFIX = "mq:governance:record:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ShopEventProducer shopEventProducer;

    @Value("${shophub.mq.governance.record-retention-days:7}")
    private long recordRetentionDays;

    @Value("${shophub.mq.governance.recent-limit:100}")
    private long recentLimit;

    @Override
    public void recordRetry(String scene, String businessKey, Object payload, Integer retryCount, String errorMessage) {
        saveRecord(scene, STAGE_RETRY, businessKey, payload, retryCount, errorMessage);
    }

    @Override
    public void recordDeadLetter(String scene, String businessKey, Object payload, Integer retryCount, String errorMessage) {
        saveRecord(scene, STAGE_DEAD_LETTER, businessKey, payload, retryCount, errorMessage);
    }

    @Override
    public MqGovernanceOverviewDTO getOverview(int limit) {
        MqGovernanceOverviewDTO overview = new MqGovernanceOverviewDTO();
        overview.setCounters(loadCounters());
        overview.setRecentRetries(listRecords(null, STAGE_RETRY, limit));
        overview.setRecentDeadLetters(listRecords(null, STAGE_DEAD_LETTER, limit));
        return overview;
    }

    @Override
    public List<MqGovernanceRecordDTO> listRecords(String scene, String stage, int limit) {
        int normalizedLimit = normalizeLimit(limit);
        List<String> recordIds = loadRecentRecordIds(normalizedLimit * 3);
        if (recordIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> keys = new ArrayList<>(recordIds.size());
        for (String recordId : recordIds) {
            keys.add(buildRecordKey(recordId));
        }
        List<String> payloads = stringRedisTemplate.opsForValue().multiGet(keys);
        if (payloads == null || payloads.isEmpty()) {
            return Collections.emptyList();
        }

        List<MqGovernanceRecordDTO> records = new ArrayList<>();
        for (String payload : payloads) {
            if (payload == null) {
                continue;
            }
            MqGovernanceRecordDTO record = deserializeRecord(payload);
            if (record == null) {
                continue;
            }
            if (scene != null && !scene.equals(record.getScene())) {
                continue;
            }
            if (stage != null && !stage.equals(record.getStage())) {
                continue;
            }
            records.add(record);
            if (records.size() >= normalizedLimit) {
                break;
            }
        }
        return records;
    }

    @Override
    public Result replayDeadLetter(String recordId) {
        MqGovernanceRecordDTO record = loadRecord(recordId);
        if (record == null) {
            return Result.fail("治理记录不存在");
        }
        if (!STAGE_DEAD_LETTER.equals(record.getStage())) {
            return Result.fail("当前记录不是死信记录");
        }

        try {
            boolean success = replayRecord(record);
            updateReplayStatus(record, success, success ? "重放成功" : "重放失败");
            return success ? Result.ok("重放成功") : Result.fail("重放失败");
        } catch (Exception e) {
            log.error("重放死信记录失败, recordId={}", recordId, e);
            updateReplayStatus(record, false, e.getMessage());
            return Result.fail("重放失败: " + e.getMessage());
        }
    }

    private void saveRecord(String scene, String stage, String businessKey, Object payload, Integer retryCount, String errorMessage) {
        MqGovernanceRecordDTO record = new MqGovernanceRecordDTO();
        record.setId(UUID.randomUUID().toString());
        record.setScene(scene);
        record.setStage(stage);
        record.setOriginalTopic(resolveOriginalTopic(scene));
        record.setDeadLetterTopic(resolveDeadLetterTopic(scene));
        record.setBusinessKey(businessKey);
        record.setRetryCount(retryCount);
        record.setErrorMessage(errorMessage);
        record.setPayloadJson(writePayload(payload));
        record.setPayloadClass(payload == null ? null : payload.getClass().getName());
        record.setCreatedAt(LocalDateTime.now());
        record.setReplayCount(0);
        persistRecord(record);
        incrementCounter(scene, stage);
    }

    private void persistRecord(MqGovernanceRecordDTO record) {
        String serialized = serializeRecord(record);
        if (serialized == null) {
            return;
        }

        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();

        valueOperations.set(buildRecordKey(record.getId()), serialized, recordRetentionDays, TimeUnit.DAYS);
        listOperations.leftPush(RECENT_RECORDS_KEY, record.getId());
        listOperations.trim(RECENT_RECORDS_KEY, 0, recentLimit - 1);
    }

    private void updateReplayStatus(MqGovernanceRecordDTO record, boolean success, String message) {
        record.setReplayCount(record.getReplayCount() == null ? 1 : record.getReplayCount() + 1);
        record.setLastReplaySuccess(success);
        record.setLastReplayMessage(message);
        record.setLastReplayTime(LocalDateTime.now());
        persistRecord(record);
        incrementCounter(record.getScene(), success ? STAGE_REPLAY_SUCCESS : STAGE_REPLAY_FAILED);
    }

    private boolean replayRecord(MqGovernanceRecordDTO record) throws JsonProcessingException {
        if (SCENE_SECKILL_ORDER.equals(record.getScene())) {
            SeckillOrderEvent event = objectMapper.readValue(record.getPayloadJson(), SeckillOrderEvent.class);
            return shopEventProducer.sendSeckillOrder(event);
        }
        if (SCENE_CACHE_EVICT.equals(record.getScene())) {
            CacheEvictEvent event = objectMapper.readValue(record.getPayloadJson(), CacheEvictEvent.class);
            return shopEventProducer.sendCacheEvict(event);
        }
        throw new IllegalArgumentException("不支持的治理场景: " + record.getScene());
    }

    private MqGovernanceRecordDTO loadRecord(String recordId) {
        if (recordId == null || recordId.trim().isEmpty()) {
            return null;
        }
        String payload = stringRedisTemplate.opsForValue().get(buildRecordKey(recordId));
        return deserializeRecord(payload);
    }

    private List<String> loadRecentRecordIds(int size) {
        Long listSize = stringRedisTemplate.opsForList().size(RECENT_RECORDS_KEY);
        if (listSize == null || listSize == 0) {
            return Collections.emptyList();
        }
        long end = Math.max(size - 1, 0);
        List<String> ids = stringRedisTemplate.opsForList().range(RECENT_RECORDS_KEY, 0, end);
        return ids == null ? Collections.emptyList() : ids;
    }

    private Map<String, Long> loadCounters() {
        HashOperations<String, Object, Object> hashOperations = stringRedisTemplate.opsForHash();
        Map<Object, Object> entries = hashOperations.entries(COUNTER_KEY);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Long> counters = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            long value = 0L;
            if (entry.getValue() != null) {
                value = Long.parseLong(entry.getValue().toString());
            }
            counters.put(String.valueOf(entry.getKey()), value);
        }
        return counters;
    }

    private void incrementCounter(String scene, String stage) {
        stringRedisTemplate.opsForHash().increment(COUNTER_KEY, scene + ":" + stage, 1L);
    }

    private String resolveOriginalTopic(String scene) {
        if (SCENE_SECKILL_ORDER.equals(scene)) {
            return MQTopics.SECKILL_ORDER_TOPIC;
        }
        if (SCENE_CACHE_EVICT.equals(scene)) {
            return MQTopics.CACHE_EVICT_TOPIC;
        }
        return null;
    }

    private String resolveDeadLetterTopic(String scene) {
        if (SCENE_SECKILL_ORDER.equals(scene)) {
            return MQTopics.SECKILL_ORDER_DLT_TOPIC;
        }
        if (SCENE_CACHE_EVICT.equals(scene)) {
            return MQTopics.CACHE_EVICT_DLT_TOPIC;
        }
        return null;
    }

    private String serializeRecord(MqGovernanceRecordDTO record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            log.error("序列化治理记录失败, scene={}, stage={}", record.getScene(), record.getStage(), e);
            return null;
        }
    }

    private MqGovernanceRecordDTO deserializeRecord(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, MqGovernanceRecordDTO.class);
        } catch (JsonProcessingException e) {
            log.error("反序列化治理记录失败", e);
            return null;
        }
    }

    private String writePayload(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("序列化消息载荷失败, payloadClass={}", payload.getClass().getName(), e);
            return null;
        }
    }

    private String buildRecordKey(String recordId) {
        return RECORD_KEY_PREFIX + recordId;
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            return 10;
        }
        return Math.min(limit, 100);
    }
}
