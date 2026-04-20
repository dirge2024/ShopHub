package com.shophub.utils;



import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.shophub.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.shophub.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {

    // 这里使用构造器注入，既能保留 final，又方便工具类在测试中单独创建。
    private final StringRedisTemplate stringRedisTemplate;

    // 逻辑过期重建走独立线程池，避免查询线程长时间阻塞。
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 尝试获取缓存重建锁，避免热点 key 在同一时间被多个线程同时重建。
     */
    private boolean tryLock(String key)
    {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",LOCK_SHOP_TTL,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放缓存重建锁。
     */
    private void unlock(String key)
    {
        stringRedisTemplate.delete(key);
    }

    public CacheClient(StringRedisTemplate stringRedisTemplate)
    {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 普通写缓存方法，支持传入任意对象和 TTL。
     */
    public void set(String key, Object value, Long timeout, TimeUnit unit)
    {
        // StringRedisTemplate 只能写字符串，这里统一序列化成 JSON。
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),timeout,unit);
    }

    /**
     * 写入逻辑过期数据，用于热点数据过期后返回旧值并异步重建，降低缓存击穿风险。
     */
    public void setWithLogicalExpire(String key,Object value,Long timeout,TimeUnit unit)
    {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(timeout)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    /**
     * 通过缓存空值的方式防止缓存穿透。
     * T 表示返回值类型，ID 表示查询参数类型。
     */
    public <T,ID> T queryWithPassThrough(String keyPrefix,  ID id,Class<T> type,
                                         Function<ID,T> dbFallback, Long timeout, TimeUnit unit) {
        String key = keyPrefix+id;
        // 1. 先查 Redis。
        String json = stringRedisTemplate.opsForValue().get(key);
        T t = null;
        if(StrUtil.isNotBlank(json))
        {
            return JSONUtil.toBean(json,type);
        }
        // 2. 命中空字符串，说明之前已经确认数据库不存在。
        if (json != null){
            return null;
        }
        // 3. Redis 未命中时回源数据库。
        t = dbFallback.apply(id);
        if(Objects.isNull(t))
        {
            this.set(key,"",timeout,unit);
            return null;
        }
        // 4. 数据存在则写回缓存，供后续请求直接命中。
        this.set(key,t,timeout,unit);
        return t;
    }

    /**
     * 逻辑过期查询：
     * 命中未过期数据直接返回；命中过期数据返回旧值并尝试异步重建。
     */
    public <R,ID> R queryLogicalExpire(String keyPrefix,ID id,Class<R> type,
                                       Function<ID,R> dbFallback,Long timeout, TimeUnit unit) {
        String key = keyPrefix+id;
        // 1. 先查 Redis。
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isBlank(json))
        {
            R r = dbFallback.apply(id);
            if (Objects.isNull(r)) {
                return null;
            }
            this.setWithLogicalExpire(key, r, timeout, unit);
            return r;
        }
        // 2. 命中后反序列化出业务数据和逻辑过期时间。
        RedisData redisData = JSONUtil.toBean(json,RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        R r  = JSONUtil.toBean(data,type);
        LocalDateTime expireTime = redisData.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now()))
        {
            return r;
        }

        // 3. 已过期时尝试获取重建锁，避免多个线程同时查库。
        String lockKey = LOCK_SHOP_KEY+id;
        boolean isLock = tryLock(lockKey);
        if(isLock)
        {
            CACHE_REBUILD_EXECUTOR.submit(()-> {
                        try {
                            R r1 = dbFallback.apply(id);
                            this.setWithLogicalExpire(key,r1,timeout,unit);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            unlock(lockKey);
                        }
                    }
            );
        }

        // 4. 无论是否拿到锁，都先返回旧值，保证热点请求快速响应。
        return r;
    }
}

