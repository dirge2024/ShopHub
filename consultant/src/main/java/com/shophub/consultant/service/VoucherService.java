package com.shophub.consultant.service;

import com.shophub.consultant.mapper.ShopMapper;
import com.shophub.consultant.mapper.VoucherMapper;
import com.shophub.consultant.mapper.VoucherOrderMapper;
import com.shophub.consultant.pojo.Shop;
import com.shophub.consultant.pojo.Voucher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class VoucherService {

    @Autowired
    private VoucherMapper voucherMapper;

    @Autowired
    private VoucherOrderMapper voucherOrderMapper;

    @Autowired
    private ShopMapper shopMapper;

    /**
     * 根据商家名称查询优惠券。
     */
    public List<Voucher> findVoucherByShopName(String shopName) {
        Shop shop = shopMapper.findShop(shopName);
        if (shop == null) {
            return Collections.emptyList();
        }
        return voucherMapper.findVoucherByShopId(shop.getId());
    }

    /**
     * 根据手机号查询用户已拥有的优惠券。
     */
    public List<Voucher> findVoucherByUserPhone(String userPhone) {
        List<Long> voucherIds = voucherOrderMapper.findByPhone(userPhone);
        if (voucherIds == null || voucherIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Voucher> vouchers = new ArrayList<>();
        for (Long voucherId : voucherIds) {
            vouchers.add(voucherMapper.findByIds(voucherId));
        }
        return vouchers;
    }
}
