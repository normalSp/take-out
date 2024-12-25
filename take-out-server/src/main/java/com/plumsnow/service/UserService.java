package com.plumsnow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plumsnow.dto.UserLoginDTO;
import com.plumsnow.entity.User;

public interface UserService extends IService<User> {
    public User login(UserLoginDTO userLoginDTO);
}
