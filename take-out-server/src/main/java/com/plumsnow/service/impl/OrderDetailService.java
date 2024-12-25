package com.plumsnow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plumsnow.entity.OrderDetail;
import com.plumsnow.mapper.OrderDetailMapper;
import com.plumsnow.service.OrdersDetailService;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailService extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrdersDetailService {
}
