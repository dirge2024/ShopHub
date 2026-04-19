package com.shophub.consultant.service;

import com.shophub.consultant.mapper.ShopMapper;
import com.shophub.consultant.pojo.Shop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShopService {

    @Autowired
    private ShopMapper shopMapper;

    //1.鏌ヨ鍟嗗淇℃伅
    public Shop findShop(String shopName) {
        return shopMapper.findShop(shopName);
    }

}
