package com.shophub.consultant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shophub.consultant.pojo.Reservation;
import com.shophub.consultant.pojo.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 鎺ュ彛
 * </p>
 *
 * @author 铏庡摜
 * @since 2021-12-22
 */
@Mapper
public interface ShopMapper extends BaseMapper<Shop> {
    @Select("select * from tb_shop where name=#{shopName}")
    Shop findShop(String shopName);
}
