package com.shophub.consultant.mapper;

import com.shophub.consultant.pojo.Reservation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReservationMapper {

    //1.娣诲姞棰勭害淇℃伅
    @Insert("insert into reservation(name,phone,communication_time,shop_name) values(#{name},#{phone},#{communicationTime},#{shopName})")
    void insert(Reservation reservation);
    //2.鏍规嵁鎵嬫満鍙锋煡璇㈤绾︿俊鎭?
    @Select("select * from reservation where phone=#{phone}")
    List<Reservation> findByPhone(String phone);

}
