package com.plumsnow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plumsnow.entity.User;
import com.plumsnow.mapper.UserMapper;
import com.plumsnow.service.UserService;
import org.springframework.stereotype.Service;


@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {}
