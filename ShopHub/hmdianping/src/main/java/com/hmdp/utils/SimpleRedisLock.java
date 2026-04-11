package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;


import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    private String name;
    private StringRedisTemplate stringRedisTempalte;
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true)+"-";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTempalte = stringRedisTemplate;
    }

    //Lua脚本
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }


    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标识：ID_PREFIX一般是UUID，用于保证不同JVM下线程ID也唯一
        // Thread.currentThread().getId() 获取当前线程ID
        // 加 "" 是为了让 long 自动转成 String
        String threadId = ID_PREFIX + Thread.currentThread().getId();

        // 尝试获取锁
        // setIfAbsent 对应 Redis 的 SETNX
        // 如果 key 不存在则写入 key=value，并设置过期时间
        // key：锁的名称（KEY_PREFIX + name）
        // value：线程ID，用于标识是谁加的锁
        Boolean success = stringRedisTempalte.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);

        // success 可能为 null，因此使用 Boolean.TRUE.equals 防止空指针
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        // 调用lua脚本，传入锁的key和线程ID
        stringRedisTempalte.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX+name),
                ID_PREFIX + Thread.currentThread().getId()
        );
    }



//    @Override
//    public void unlock() {
//        // 获取当前线程标识
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//
//        // 从 Redis 中获取当前锁的线程标识
//        String id = stringRedisTempalte.opsForValue().get(KEY_PREFIX + name);
//
//        // 判断锁是否是当前线程持有
//        // 防止删除其他线程的锁
//        if (id.equals(threadId)) {
//            // 删除锁（释放锁）
//            stringRedisTempalte.delete(KEY_PREFIX + name);
//        }
//    }
}

