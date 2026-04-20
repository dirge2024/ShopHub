package com.shophub.consultant.service;

import com.shophub.consultant.mapper.ReservationMapper;
import com.shophub.consultant.pojo.Reservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservationService {

    @Autowired
    private ReservationMapper reservationMapper;

    /**
     * 写入预约记录。
     */
    public void insert(Reservation reservation) {
        reservationMapper.insert(reservation);
    }

    /**
     * 根据手机号查询预约记录。
     */
    public List<Reservation> findByPhone(String phone) {
        return reservationMapper.findByPhone(phone);
    }
}
