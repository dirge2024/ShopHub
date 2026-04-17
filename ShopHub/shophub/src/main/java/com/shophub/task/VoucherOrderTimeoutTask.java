package com.shophub.task;

import com.shophub.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class VoucherOrderTimeoutTask {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Value("${shophub.order.timeout-minutes:15}")
    private Integer timeoutMinutes;

    @Value("${shophub.order.scan-batch-size:20}")
    private Integer scanBatchSize;

    @Scheduled(fixedDelayString = "${shophub.order.scan-fixed-delay-ms:30000}")
    public void closeTimeoutOrders() {
        int count = voucherOrderService.scanTimeoutOrders(timeoutMinutes, scanBatchSize);
        if (count > 0) {
            log.info("本次超时关单完成, count={}", count);
        }
    }
}
