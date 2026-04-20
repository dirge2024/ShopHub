package com.shophub.consultant;

import com.shophub.consultant.pojo.Reservation;
import com.shophub.consultant.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    /**
     * 预留的插入测试样例，联调数据库写入时可以按需打开。
     */
    @Test
    void testInsert() {
//        Reservation reservation = new Reservation(null, "小王", "13800000001", LocalDateTime.now(), "上海", 580);
//        reservationService.insert(reservation);
    }

    /**
     * 根据手机号查询预约记录。
     */
    @Test
    void testFindByPhone() {
        String phone = "13800000001";
        List<Reservation> reservation = reservationService.findByPhone(phone);
        System.out.println(reservation);
    }
}
