package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;


/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public ShopTypeServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Result queryTypeList() {
        //1、从redis里面查询店铺类型
        String key = CACHE_SHOP_TYPE_KEY+UUID.randomUUID().toString();
        String shopTypeJson = stringRedisTemplate.opsForValue().get(key);


        //2、判断是否存在
        if(StrUtil.isNotEmpty(shopTypeJson))
        {
            //3、存在的话直接返回缓存数据
            List<ShopType> typeList = JSONUtil.toList(shopTypeJson, ShopType.class);
            return Result.ok(typeList);
        }

        //4、如果不存在查询数据库
        List<ShopType> typeList =  query().orderByAsc("sort").list();

        //5、如果数据库中也不存在直接返回错误
        if(Objects.isNull(typeList))
        {
            return Result.fail("店铺类型不存在");
        }

        //6、如果数据库中存在，写入redis缓存
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(typeList),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //7、返回结果

        return Result.ok(typeList);
    }
}
