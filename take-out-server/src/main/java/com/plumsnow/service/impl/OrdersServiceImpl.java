package com.plumsnow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plumsnow.entity.Orders;
import com.plumsnow.mapper.OrdersMapper;
import com.plumsnow.service.OrdersService;
import org.springframework.stereotype.Service;


@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService { }
