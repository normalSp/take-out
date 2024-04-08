package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.entity.OrderDetail;
import com.sky.mapper.OrderDetailMapper;
import com.sky.service.OrdersDetailService;
import com.sky.service.OrdersService;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailService extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrdersDetailService {
}
