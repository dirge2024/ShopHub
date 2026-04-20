package com.shophub.controller;


import com.shophub.dto.Result;
import com.shophub.service.IVoucherOrderService;
import org.springframework.web.bind.annotation.GetMapping;
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

    /**
     * 秒杀下单入口，真正的资格校验和库存扣减逻辑在服务层通过 Lua 完成。
     */
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {

        return voucherOrderService.seckillVoucher(voucherId);
    }

    /**
     * 查询订单状态，方便前后端联调时确认支付、关单、扫描任务的执行结果。
     */
    @GetMapping("{id}/status")
    public Result queryOrderStatus(@PathVariable("id") Long orderId) {
        return voucherOrderService.queryOrderStatus(orderId);
    }

    /**
     * 模拟支付成功回调接口，当前阶段主要用于订单状态闭环联调。
     */
    @PostMapping("pay-success/{id}")
    public Result paySuccess(@PathVariable("id") Long orderId,
                             @RequestParam(value = "payType", defaultValue = "2") Integer payType) {
        return voucherOrderService.paySuccess(orderId, payType);
    }

    /**
     * 手动执行单笔超时关单，便于验证超时规则和状态机判断是否符合预期。
     */
    @PostMapping("timeout-close/{id}")
    public Result closeTimeoutOrder(@PathVariable("id") Long orderId,
                                    @RequestParam(value = "timeoutMinutes", defaultValue = "15") Integer timeoutMinutes) {
        return voucherOrderService.closeTimeoutOrder(orderId, timeoutMinutes);
    }

    /**
     * 手动触发批量超时扫描，主要用于联调和回归验证。
     */
    @PostMapping("timeout-scan")
    public Result manualScanTimeoutOrders(
            @RequestParam(value = "timeoutMinutes", defaultValue = "15") Integer timeoutMinutes,
            @RequestParam(value = "batchSize", defaultValue = "20") Integer batchSize) {
        return voucherOrderService.manualScanTimeoutOrders(timeoutMinutes, batchSize);
    }
}

