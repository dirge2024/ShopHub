package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    //id全局一致性，高可用性，高性能，安全性，递增性
    /**
     * 在这里得到开始时间戳 2023-09-01 00:00:00
     * 1693526400L
    */
    static final long BEGIN_TIMESTAMP = 1693526400L;

    /**
     * 序列化位数
     */
    private static final long COUNT_BITS = 32L;
    /**
     * 生成分布式ID
     * @param keyPrefix 业务前缀
     * @return 分布式ID
     */
    public long nextID(String keyPrefix){

        //1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond;

        //2.生成序列号
        //将当前时间按格式转化为字符串用来拼接，用这个作为key放置一直自增导致超时
        String data = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //自增序列号
        Long count = stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix+data);

        //3.拼接并返回
        //先向左做位运算，左移32位，右边置0，在和count做或运算，保留count的值
        return timestamp << COUNT_BITS | count;
    }

}
;