package com.shophub.consultant;

import com.shophub.consultant.pojo.Reservation;
import com.shophub.consultant.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class ReservationServiceTest {
    @Autowired
    private ReservationService reservationService;
    //娴嬭瘯娣诲姞
    @Test
    void testInsert(){
//        Reservation reservation = new Reservation(null, "灏忕帇", "鐢?, "13800000001", LocalDateTime.now(), "涓婃捣", 580);
//        reservationService.insert(reservation);
    }
    //娴嬭瘯鏌ヨ
    @Test
    void testFindByPhone(){
        String phone = "13800000001";
        List<Reservation> reservation = reservationService.findByPhone(phone);
        System.out.println(reservation);
    }
}
