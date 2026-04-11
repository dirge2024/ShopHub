package com.shophub.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.shophub.dto.Result;
import com.shophub.entity.Shop;
import com.shophub.mapper.ShopMapper;
import com.shophub.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shophub.utils.CacheClient;
import com.shophub.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.shophub.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    //Impl实现层
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        //缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,
//                id2->getById(id2),CACHE_SHOP_TTL,TimeUnit.MINUTES);
        //id2->getById(id2) 表示使用id2调用getById方法查询数据库返回结果
        //可以写成this::getById，表示使用当前对象调用getById方法查询数据库返回结果

        //互斥锁解决缓存击穿问题
        //Shop shop = queryWithMutex(id);

        //逻辑过期解决缓存击穿问题
        Shop shop = cacheClient.queryLogicalExpire(CACHE_SHOP_KEY,id,Shop.class,this::getById,
                CACHE_SHOP_TTL,TimeUnit.MINUTES);
        return Result.ok(shop);
    }



    @Override
    //注解，放在一个事务里面只能一起成功或者失败
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null)
        {
            return Result.fail("点评id不存在");
        }
        //1、先更新数据库
        updateById(shop);
        //2、在删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);

        return Result.ok();
    }
}

