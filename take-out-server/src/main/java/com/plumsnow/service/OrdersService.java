package com.plumsnow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plumsnow.dto.OrdersPaymentDTO;
import com.plumsnow.entity.Orders;
import com.plumsnow.vo.OrderPaymentVO;

public interface OrdersService extends IService<Orders> {
    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);
}
