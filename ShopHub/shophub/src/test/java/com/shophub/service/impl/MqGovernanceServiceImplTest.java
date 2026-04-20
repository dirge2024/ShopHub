package com.shophub.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.dto.MqGovernanceOverviewDTO;
import com.shophub.dto.MqGovernanceRecordDTO;
import com.shophub.dto.Result;
import com.shophub.messaging.event.SeckillOrderEvent;
import com.shophub.messaging.producer.ShopEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqGovernanceServiceImplTest {

    @InjectMocks
    private MqGovernanceServiceImpl mqGovernanceService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ShopEventProducer shopEventProducer;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mqGovernanceService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(mqGovernanceService, "recordRetentionDays", 7L);
        ReflectionTestUtils.setField(mqGovernanceService, "recentLimit", 100L);

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void recordDeadLetterShouldPersistRecordAndCounter() {
        SeckillOrderEvent event = new SeckillOrderEvent(1L, 2L, 3L, 3, "created-at");

        mqGovernanceService.recordDeadLetter("seckill_order", "1", event, 3, "消费失败");

        verify(valueOperations).set(anyString(), anyString(), eq(7L), any());
        verify(listOperations).leftPush(eq("mq:governance:recent"), anyString());
        verify(hashOperations).increment("mq:governance:counters", "seckill_order:dead_letter", 1L);
    }

    @Test
    void getOverviewShouldLoadCountersAndRecentRecords() throws Exception {
        MqGovernanceRecordDTO retryRecord = buildRecord("retry-1", "seckill_order", "retry");
        MqGovernanceRecordDTO deadLetterRecord = buildRecord("dead-1", "cache_evict", "dead_letter");

        when(listOperations.size("mq:governance:recent")).thenReturn(2L);
        when(listOperations.range("mq:governance:recent", 0, 14)).thenReturn(Arrays.asList("retry-1", "dead-1"));
        when(valueOperations.multiGet(Arrays.asList(
                "mq:governance:record:retry-1",
                "mq:governance:record:dead-1"
        ))).thenReturn(Arrays.asList(
                new ObjectMapper().writeValueAsString(retryRecord),
                new ObjectMapper().writeValueAsString(deadLetterRecord)
        ));

        Map<Object, Object> counters = new LinkedHashMap<>();
        counters.put("seckill_order:retry", "2");
        counters.put("cache_evict:dead_letter", "1");
        when(hashOperations.entries("mq:governance:counters")).thenReturn(counters);

        MqGovernanceOverviewDTO overview = mqGovernanceService.getOverview(5);

        assertEquals(Long.valueOf(2L), overview.getCounters().get("seckill_order:retry"));
        assertEquals(1, overview.getRecentRetries().size());
        assertEquals(1, overview.getRecentDeadLetters().size());
    }

    @Test
    void replayDeadLetterShouldResendEventAndUpdateReplayStatus() throws Exception {
        MqGovernanceRecordDTO record = buildRecord("dead-1", "seckill_order", "dead_letter");
        record.setPayloadJson(new ObjectMapper().writeValueAsString(new SeckillOrderEvent(1L, 2L, 3L, 3, "created-at")));

        when(valueOperations.get("mq:governance:record:dead-1"))
                .thenReturn(new ObjectMapper().writeValueAsString(record));
        when(shopEventProducer.sendSeckillOrder(any(SeckillOrderEvent.class))).thenReturn(true);

        Result result = mqGovernanceService.replayDeadLetter("dead-1");

        assertTrue(result.getSuccess());
        verify(shopEventProducer).sendSeckillOrder(any(SeckillOrderEvent.class));
        verify(hashOperations).increment("mq:governance:counters", "seckill_order:replay_success", 1L);
        verify(valueOperations).set(eq("mq:governance:record:dead-1"), anyString(), eq(7L), any());
    }

    @Test
    void replayDeadLetterShouldRejectNonDeadLetterRecord() throws Exception {
        MqGovernanceRecordDTO record = buildRecord("retry-1", "seckill_order", "retry");
        when(valueOperations.get("mq:governance:record:retry-1"))
                .thenReturn(new ObjectMapper().writeValueAsString(record));

        Result result = mqGovernanceService.replayDeadLetter("retry-1");

        assertFalse(result.getSuccess());
        assertEquals("当前记录不是死信记录", result.getErrorMsg());
    }

    private MqGovernanceRecordDTO buildRecord(String id, String scene, String stage) {
        MqGovernanceRecordDTO record = new MqGovernanceRecordDTO();
        record.setId(id);
        record.setScene(scene);
        record.setStage(stage);
        record.setBusinessKey("1");
        record.setRetryCount(1);
        record.setReplayCount(0);
        return record;
    }
}
