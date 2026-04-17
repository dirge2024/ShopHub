package com.shophub.controller;


import com.shophub.dto.Result;
import com.shophub.service.IVoucherOrderService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {

        return voucherOrderService.seckillVoucher(voucherId);
    }

    @PostMapping("pay-success/{id}")
    public Result paySuccess(@PathVariable("id") Long orderId,
                             @RequestParam(value = "payType", defaultValue = "2") Integer payType) {
        return voucherOrderService.paySuccess(orderId, payType);
    }

    @PostMapping("timeout-close/{id}")
    public Result closeTimeoutOrder(@PathVariable("id") Long orderId,
                                    @RequestParam(value = "timeoutMinutes", defaultValue = "15") Integer timeoutMinutes) {
        return voucherOrderService.closeTimeoutOrder(orderId, timeoutMinutes);
    }
}

