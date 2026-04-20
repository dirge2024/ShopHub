package com.shophub.consultant.service;

import com.shophub.consultant.mapper.ShopMapper;
import com.shophub.consultant.pojo.Shop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShopService {

    @Autowired
    private ShopMapper shopMapper;

    /**
     * 根据商家名称查询商家信息。
     */
    public Shop findShop(String shopName) {
        return shopMapper.findShop(shopName);
    }
}
