package com.shophub.consultant.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_voucher")
public class Voucher implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 涓婚敭
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 鍟嗛摵id
     */
    private Long shopId;

    /**
     * 浠ｉ噾鍒告爣棰?
     */
    private String title;

    /**
     * 鍓爣棰?
     */
    private String subTitle;

    /**
     * 浣跨敤瑙勫垯
     */
    private String rules;

    /**
     * 鏀粯閲戦
     */
    private Long payValue;

    /**
     * 鎶垫墸閲戦
     */
    private Long actualValue;

    /**
     * 浼樻儬鍒哥被鍨?
     */
    private Integer type;

    /**
     * 浼樻儬鍒哥被鍨?
     */
    private Integer status;
    /**
     * 搴撳瓨
     */
    @TableField(exist = false)
    private Integer stock;

    /**
     * 鐢熸晥鏃堕棿
     */
    @TableField(exist = false)
    private LocalDateTime beginTime;

    /**
     * 澶辨晥鏃堕棿
     */
    @TableField(exist = false)
    private LocalDateTime endTime;

    /**
     * 鍒涘缓鏃堕棿
     */
    private LocalDateTime createTime;


    /**
     * 鏇存柊鏃堕棿
     */
    private LocalDateTime updateTime;


}
