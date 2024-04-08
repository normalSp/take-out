package com.sky.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import com.sky.service.OrdersService;
import org.springframework.stereotype.Service;

@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {
}
