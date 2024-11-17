package com.sky.controller.user;

import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.RedisKeyConstant;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user/user")
@ApiOperation("用户模块相关接口")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/login")
    @ApiOperation("用户微信登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
        log.info("用户授权码: {}", userLoginDTO.getCode());

        //微信登录
        User user = userService.login(userLoginDTO);

        //生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID,user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .build();

        return Result.success(userLoginVO);
    }

    @PostMapping("/selectShop")
    @ApiOperation("选择商店")
    public Result<Long> selectShop(@RequestBody DishDTO dishDTO){
        Long shopId = dishDTO.getShopId();
        log.info("当前选择的商店id：{}", shopId);

        //key 为 shopId + userId
        stringRedisTemplate.opsForValue().set(RedisKeyConstant.SHOP_ID + BaseContext.getCurrentId(), String.valueOf(shopId));

        return Result.success(shopId);
    }
}
