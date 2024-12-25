package com.plumsnow.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plumsnow.constant.MessageConstant;
import com.plumsnow.dto.UserLoginDTO;
import com.plumsnow.entity.User;
import com.plumsnow.exception.LoginFailedException;
import com.plumsnow.mapper.UserMapper;
import com.plumsnow.properties.WeChatProperties;
import com.plumsnow.service.UserService;
import com.plumsnow.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;

    /**
     * 用户微信登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public User login(UserLoginDTO userLoginDTO) {
        //获取openid
        String openid = getOpenid(userLoginDTO.getCode());

        //判断openid是否为空
        if(null == openid){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        //判断是否为新用户
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getOpenid, openid);

        User user = this.getOne(lambdaQueryWrapper);
        
        if(null == user){
            user = new User();
            user.setOpenid(openid);
            user.setCreateTime(LocalDateTime.now());
            this.save(user);
        }
        

        
        return user;
    }

    /**
     * 获取微信openid
     * @param code
     * @return
     */
    private String getOpenid(String code){
        //调用微信接口服务，获取当前用户openid
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        //解析出回传JSON数据中的openid
        JSONObject jsonObject = JSON.parseObject(json);

        return jsonObject.getString("openid");
    }


}
