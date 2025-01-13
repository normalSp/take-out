package com.plumsnow.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class VoucherOrderDTO implements Serializable {

    /**
     * 商铺id
     */
    private Long shopId;

    /**
     * 支付金额
     */
    private Long payValue;

    /**
     * 抵扣金额
     */
    private Long actualValue;

    /**
     * 优惠券类型
     */
    private Integer type;

    /**
     * 优惠券状态
     */
    private Integer payStatus;

    /**
     * 代金券标题
     */
    private String title;

    /**
     * 副标题
     */
    private String subTitle;

    /**
     * 使用规则
     */
    private String rules;
}
