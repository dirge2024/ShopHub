package com.shophub.consultant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shophub.consultant.mapper.ShopMapper;
import com.shophub.consultant.mapper.VoucherMapper;
import com.shophub.consultant.mapper.VoucherOrderMapper;
import com.shophub.consultant.pojo.Shop;
import com.shophub.consultant.pojo.Voucher;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

@Service
public class VoucherService {

    @Autowired
    private VoucherMapper voucherMapper;

    @Autowired
    private VoucherOrderMapper voucherOrderMapper;

    @Autowired
    private ShopMapper shopMapper;

    //1.鏌ヨ鍟嗗淇℃伅
    public List<Voucher> findVoucherByShopName(String shopName) {
        Shop shop = shopMapper.findShop(shopName);
        return voucherMapper.findVoucherByShopId(shop.getId());
    }

    //2.鏌ヨ鐢ㄦ埛鎷ユ湁鐨勪紭鎯犲埜
    public List<Voucher> findVoucherByUserPhone(String userPhone) {
        List<Long> voucherIds = voucherOrderMapper.findByPhone(userPhone);
        String ids = StringUtils.join(voucherIds, ",");
        // System.out.println("鐢ㄦ埛鎷ユ湁鐨勪紭鎯犲埜id鏄? " + ids);
        List<Voucher> vouchers = new ArrayList<>();
        for (Long voucherId : voucherIds) {
            vouchers.add(voucherMapper.findByIds(voucherId));
        }
        // System.out.println("鏌ヨ鐢ㄦ埛鎷ユ湁鐨勪紭鎯犲埜: " + vouchers);
        return vouchers;
    }
}