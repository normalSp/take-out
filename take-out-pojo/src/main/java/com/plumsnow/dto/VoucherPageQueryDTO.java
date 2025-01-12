package com.plumsnow.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class VoucherPageQueryDTO implements Serializable {
    //商家id
    private Long shopId;

    //优惠券类型 0,普通券；1,秒杀券
    private Integer type;

    //优惠券状态 1,上架; 2,下架; 3,过期
    private Integer status;

    //页码
    private int page;

    //每页显示记录数
    private int pageSize;
}
