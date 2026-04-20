package com.shophub.service;

import com.shophub.dto.Result;
import com.shophub.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IShopTypeService extends IService<ShopType> {

    Result queryTypeList();
}

