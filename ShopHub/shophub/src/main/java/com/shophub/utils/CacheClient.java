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

    //不适用@Resource注解自动注入，这里右final关键字，使用构造器注入
    private final StringRedisTemplate stringRedisTemplate;

    //线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    //获取锁
    private boolean tryLock(String key)
    {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",LOCK_SHOP_TTL,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    // 释放锁
    private void unlock(String key)
    {
        stringRedisTemplate.delete(key);
    }

    public CacheClient(StringRedisTemplate stringRedisTemplate)
    {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //1.将数据加入redis，设置有效期，通用的写缓存方法
    //解决靠策略 缓存空对象/空字符串
    public void set(String key, Object value, Long timeout, TimeUnit unit)
    {
        //stringRedisTemplate 传入的是字符串，所以需要将对象转换成json字符串
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),timeout,unit);
    }

    //2.将数据加入redis，设置逻辑过期时间，解决缓存击穿问题
    public void setWithLogicalExpire(String key,Object value,Long timeout,TimeUnit unit)
    {
        //设置逻辑过期时间,不知道传入的数据类型，转换为RedisData，里面包好数据和过期时间
        RedisData redisData = new RedisData();
        //调用两个set函数就行了
        redisData.setData(value);
        //unit.toSeconds(timeout)确保计时单位是秒
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(timeout)));
        //写入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    //缓存空值解决穿透，使用泛型
    //泛型<T,ID> T 代表返回值类型，ID代表传入参数类型,
    public <T,ID> T queryWithPassThrough(String keyPrefix,  ID id,Class<T> type,
                                         Function<ID,T> dbFallback, Long timeout, TimeUnit unit) {
        String key = keyPrefix+id;
        //1、从redis查询商铺缓存,获取json字符串，不知道说明类型等下在抓换
        String json = stringRedisTemplate.opsForValue().get(key);
        T t = null;
        //2、要判断是否存在
        if(StrUtil.isNotBlank(json))
        {
            //3、如果存在直接返回结果
            /*为什么要转换的类型要卸载括号里，因为不知道传入的是什么类型，
              所以在调用这个方法的时候传入一个Class<T> type参数，来指定转换的类型*/
            return JSONUtil.toBean(json,type);
        }
        //还要判断一下是否为空值 即 ""
        if (json != null){
            // 此时缓存数据为空值,""，说明之前查询过数据库不存在这个商铺，所以直接返回错误信息
            return null;
        }
        //4、如果不存在根据id去数据库里面查询
        //根本不知道具体怎么查询，所以这里要传入一个查询方法
        t = dbFallback.apply(id);
        //5、如果数据库也不存在直接返回错误
        if(Objects.isNull(t))
        {
            //将空值写入redis
            this.set(key,"",timeout,unit);
            //返回错误信息
            return null;
        }
        //6、数据库里面存在，写入redis缓存, 上面写过了这里传参数就行
        this.set(key,t,timeout,unit);
        //7、返回结果
        return t;
    }

    public <R,ID> R queryLogicalExpire(String keyPrefix,ID id,Class<R> type,
                                       Function<ID,R> dbFallback,Long timeout, TimeUnit unit) {
        String key = keyPrefix+id;
        //1、从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2、要判断是否存在
        if(StrUtil.isBlank(json))
        {
            //3、如果存在直接返回结果
            return null;
        }
        //4、命中，需要吧json反序列化为对象、
        RedisData redisData = JSONUtil.toBean(json,RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        R r  = JSONUtil.toBean(data,type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //5、判断是否过期
        if(expireTime.isAfter(LocalDateTime.now()))
        {
            //5.1、如果没过期，直接返回点评信息
            return r;
        }

        //5.2、过期了，缓存重建
        //6、缓存重建
        //6.1、获取互斥锁
        String lockKey = LOCK_SHOP_KEY+id;
        boolean isLock = tryLock(lockKey);
        //6.2、判断是否获取成功
        if(isLock)
        {
            //6.3、成功，开启独立线程，实现缓存重建
            //在这里用使用线程池创建就好了，缓存重建就用saveShop2Redis
            CACHE_REBUILD_EXECUTOR.submit(()-> {
                        //在这里面执行缓存重建
                        try {
                            //先查数据库，还是要传入function
                            R r1 = dbFallback.apply(id);
                            //再写数据库
                            this.setWithLogicalExpire(key,r1,timeout,unit);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            //释放锁
                            unlock(lockKey);
                        }
                    }
            );
        }

        //6.4、失败，返回过期的商铺信息
        return r;
    }
}

