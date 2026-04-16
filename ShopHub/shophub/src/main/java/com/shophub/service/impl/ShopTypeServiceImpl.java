package com.shophub.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shophub.dto.Result;
import com.shophub.entity.ShopType;
import com.shophub.mapper.ShopTypeMapper;
import com.shophub.service.IShopTypeService;
import com.shophub.service.LocalCacheService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.shophub.utils.RedisConstants.CACHE_SHOP_TTL;
import static com.shophub.utils.RedisConstants.CACHE_SHOP_TYPE_LIST_KEY;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private LocalCacheService localCacheService;

    @Override
    public Result queryTypeList() {
        List<ShopType> localTypeList = localCacheService.getShopTypeList();
        if (localTypeList != null && !localTypeList.isEmpty()) {
            return Result.ok(localTypeList);
        }

        String shopTypeJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_LIST_KEY);
        if (StrUtil.isNotEmpty(shopTypeJson)) {
            List<ShopType> typeList = JSONUtil.toList(shopTypeJson, ShopType.class);
            localCacheService.putShopTypeList(typeList);
            return Result.ok(typeList);
        }

        List<ShopType> typeList = query().orderByAsc("sort").list();
        if (typeList == null || typeList.isEmpty()) {
            return Result.fail("店铺类型不存在");
        }

        stringRedisTemplate.opsForValue().set(
                CACHE_SHOP_TYPE_LIST_KEY,
                JSONUtil.toJsonStr(typeList),
                CACHE_SHOP_TTL,
                TimeUnit.MINUTES
        );
        localCacheService.putShopTypeList(typeList);
        return Result.ok(typeList);
    }
}

