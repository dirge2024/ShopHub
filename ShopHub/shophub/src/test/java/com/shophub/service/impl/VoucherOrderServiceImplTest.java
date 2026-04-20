package com.shophub.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.shophub.dto.Result;
import com.shophub.entity.VoucherOrder;
import com.shophub.utils.VoucherOrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherOrderServiceImplTest {

    @Spy
    @InjectMocks
    private VoucherOrderServiceImpl voucherOrderService;

    @Mock
    private QueryChainWrapper<VoucherOrder> queryChainWrapper;

    @Test
    void paySuccessShouldFailWhenOrderMissing() {
        doReturn(null).when(voucherOrderService).getById(1L);

        Result result = voucherOrderService.paySuccess(1L, 2);

        assertFalse(result.getSuccess());
        assertEquals("订单不存在", result.getErrorMsg());
    }

    @Test
    void paySuccessShouldReturnOkWhenOrderAlreadyPaid() {
        VoucherOrder order = buildOrder(1L, VoucherOrderStatus.PAID, LocalDateTime.now().minusMinutes(20));
        doReturn(order).when(voucherOrderService).getById(1L);

        Result result = voucherOrderService.paySuccess(1L, 2);

        assertTrue(result.getSuccess());
        assertEquals("订单已支付", result.getData());
        verify(voucherOrderService, never()).updateById(any(VoucherOrder.class));
    }

    @Test
    void paySuccessShouldUpdateStatusForUnpaidOrder() {
        VoucherOrder order = buildOrder(1L, VoucherOrderStatus.UNPAID, LocalDateTime.now().minusMinutes(20));
        doReturn(order).when(voucherOrderService).getById(1L);
        doReturn(true).when(voucherOrderService).updateById(any(VoucherOrder.class));

        Result result = voucherOrderService.paySuccess(1L, 3);

        assertTrue(result.getSuccess());
        assertEquals("支付成功", result.getData());

        ArgumentCaptor<VoucherOrder> captor = ArgumentCaptor.forClass(VoucherOrder.class);
        verify(voucherOrderService).updateById(captor.capture());
        VoucherOrder updateOrder = captor.getValue();
        assertEquals(VoucherOrderStatus.PAID.getCode(), updateOrder.getStatus());
        assertEquals(Integer.valueOf(3), updateOrder.getPayType());
        assertEquals(order.getVersion(), updateOrder.getVersion());
        assertNotNull(updateOrder.getPayTime());
    }

    @Test
    void closeTimeoutOrderShouldRejectUnexpiredOrder() {
        VoucherOrder order = buildOrder(1L, VoucherOrderStatus.UNPAID, LocalDateTime.now().minusMinutes(5));
        doReturn(order).when(voucherOrderService).getById(1L);

        Result result = voucherOrderService.closeTimeoutOrder(1L, 15);

        assertFalse(result.getSuccess());
        assertEquals("订单未超时", result.getErrorMsg());
        verify(voucherOrderService, never()).updateById(any(VoucherOrder.class));
    }

    @Test
    void closeTimeoutOrderShouldCancelExpiredOrder() {
        VoucherOrder order = buildOrder(1L, VoucherOrderStatus.UNPAID, LocalDateTime.now().minusMinutes(30));
        doReturn(order).when(voucherOrderService).getById(1L);
        doReturn(true).when(voucherOrderService).updateById(any(VoucherOrder.class));

        Result result = voucherOrderService.closeTimeoutOrder(1L, 15);

        assertTrue(result.getSuccess());
        assertEquals("超时关单成功", result.getData());

        ArgumentCaptor<VoucherOrder> captor = ArgumentCaptor.forClass(VoucherOrder.class);
        verify(voucherOrderService).updateById(captor.capture());
        VoucherOrder updateOrder = captor.getValue();
        assertEquals(VoucherOrderStatus.CANCELED.getCode(), updateOrder.getStatus());
        assertEquals(order.getVersion(), updateOrder.getVersion());
        assertEquals(null, updateOrder.getPayType());
    }

    @Test
    void scanTimeoutOrdersShouldReturnZeroWhenNoOrderFound() {
        doReturn(queryChainWrapper).when(voucherOrderService).query();
        when(queryChainWrapper.eq(anyString(), any())).thenReturn(queryChainWrapper);
        when(queryChainWrapper.le(anyString(), any())).thenReturn(queryChainWrapper);
        when(queryChainWrapper.last(anyString())).thenReturn(queryChainWrapper);
        when(queryChainWrapper.list()).thenReturn(Collections.emptyList());

        int count = voucherOrderService.scanTimeoutOrders(15, 20);

        assertEquals(0, count);
    }

    @Test
    void scanTimeoutOrdersShouldCountSuccessfulClosures() {
        VoucherOrder first = buildOrder(1L, VoucherOrderStatus.UNPAID, LocalDateTime.now().minusMinutes(30));
        VoucherOrder second = buildOrder(2L, VoucherOrderStatus.UNPAID, LocalDateTime.now().minusMinutes(40));
        doReturn(queryChainWrapper).when(voucherOrderService).query();
        when(queryChainWrapper.eq(eq("status"), eq(VoucherOrderStatus.UNPAID.getCode()))).thenReturn(queryChainWrapper);
        when(queryChainWrapper.le(eq("create_time"), any())).thenReturn(queryChainWrapper);
        when(queryChainWrapper.last(eq("limit 20"))).thenReturn(queryChainWrapper);
        when(queryChainWrapper.list()).thenReturn(Arrays.asList(first, second));
        doReturn(Result.ok("超时关单成功")).when(voucherOrderService).closeTimeoutOrder(1L, 15);
        doReturn(Result.fail("当前订单状态不允许关单")).when(voucherOrderService).closeTimeoutOrder(2L, 15);

        int count = voucherOrderService.scanTimeoutOrders(15, 20);

        assertEquals(1, count);
        verify(voucherOrderService).closeTimeoutOrder(1L, 15);
        verify(voucherOrderService).closeTimeoutOrder(2L, 15);
    }

    private VoucherOrder buildOrder(Long orderId, VoucherOrderStatus status, LocalDateTime createTime) {
        VoucherOrder order = new VoucherOrder();
        order.setId(orderId);
        order.setStatus(status.getCode());
        order.setCreateTime(createTime);
        order.setVersion(0);
        return order;
    }
}
