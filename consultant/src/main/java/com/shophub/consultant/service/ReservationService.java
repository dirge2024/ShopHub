package com.shophub.consultant.service;

import com.shophub.consultant.mapper.ReservationMapper;
import com.shophub.consultant.mapper.ShopMapper;
import com.shophub.consultant.pojo.Reservation;
import com.shophub.consultant.pojo.Shop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservationService {
    @Autowired
    private ReservationMapper reservationMapper;

    //1.娣诲姞棰勭害淇℃伅鐨勬柟娉?
    public void insert(Reservation reservation) {
        reservationMapper.insert(reservation);
    }

    //2.鏌ヨ棰勭害淇℃伅鐨勬柟娉?鏍规嵁鎵嬫満鍙锋煡璇?
    public List<Reservation> findByPhone(String phone) {
        return reservationMapper.findByPhone(phone);
    }
}
