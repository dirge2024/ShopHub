package com.shophub.consultant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shophub.consultant.pojo.Shop;
import com.shophub.consultant.pojo.Voucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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
public interface VoucherMapper extends BaseMapper<Voucher> {

    @Select("select * from tb_voucher where shop_id=#{shopId}")
    List<Voucher> findVoucherByShopId(Long shopId);

    /*@Select("<script>" +
            "SELECT * FROM tb_voucher " +
            "WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "   #{id}" +
            "</foreach>" +
            "</script>")*/
    @Select("select * from tb_voucher where id=#{id}")
    Voucher findByIds(Long id);
}
