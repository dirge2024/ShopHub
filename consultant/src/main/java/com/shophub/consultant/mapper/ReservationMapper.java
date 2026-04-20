package com.shophub.consultant.mapper;

import com.shophub.consultant.pojo.Reservation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReservationMapper {

    /**
     * 新增预约记录。
     */
    @Insert("insert into reservation(name,phone,communication_time,shop_name) values(#{name},#{phone},#{communicationTime},#{shopName})")
    void insert(Reservation reservation);

    /**
     * 根据手机号查询预约记录。
     */
    @Select("select * from reservation where phone=#{phone}")
    List<Reservation> findByPhone(String phone);
}
