package com.plumsnow.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plumsnow.constant.JwtClaimsConstant;
import com.plumsnow.constant.MessageConstant;
import com.plumsnow.constant.RedisKeyConstant;
import com.plumsnow.context.BaseContext;
import com.plumsnow.dto.DishDTO;
import com.plumsnow.dto.UserLoginDTO;
import com.plumsnow.entity.User;
import com.plumsnow.exception.CustomException;
import com.plumsnow.exception.LoginFailedException;
import com.plumsnow.properties.JwtProperties;
import com.plumsnow.result.Result;
import com.plumsnow.service.UserService;
import com.plumsnow.utils.JwtUtil;
import com.plumsnow.utils.SMSUtils;
import com.plumsnow.utils.ValidateCodeUtils;
import com.plumsnow.vo.UserLoginVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/user/user")
@ApiOperation("用户模块相关接口")
public class UserController {
    //阿里云密钥
    @Value("${reggie.accessKeyId}")
    private String accessKeyId;
    @Value("${reggie.accessKeySecret}")
    private String accessKeySecret;

    @Value("${reggie.accessKeyId_}")
    private String accessKeyId_;
    @Value("${reggie.accessKeySecret_}")
    private String accessKeySecre_;
    //阿里云密钥

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

    /**
     * 发送手机验证码
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/sendMsg")
    @ApiOperation("发送手机验证码")
    public Result<String> sendMsg(@RequestBody UserLoginDTO userLoginDTO) {
        //0.获取配置文件中的accessKeyId和accessKeySecret
        String accessKeyId1 = accessKeyId + accessKeyId_;
        String accessKeySecret1 = accessKeySecret + accessKeySecre_;

        log.info("accessKeyId:{}", accessKeyId1);
        log.info("accessKeySecret:{}", accessKeySecret1);

        //1.获取手机号
        String phoneNumber = userLoginDTO.getPhone();

        //2.生成随机4位验证码
        if (null != phoneNumber) {
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("手机号为：{}，验证码为：{}", phoneNumber, code);

            //3.发送验证码
            try {
                SMSUtils.sendMessage(phoneNumber, code, accessKeyId1, accessKeySecret1);
            } catch (Exception e) {
                throw new CustomException("发送验证码失败");
            }


            //4. 将验证码保存到redis，设置超时时间为5分钟
            stringRedisTemplate.opsForValue().set(phoneNumber, code, 5 * 60, TimeUnit.SECONDS);
            //redisTemplate.opsForValue().set(phoneNumber, code, 5 * 60, TimeUnit.SECONDS);

            return Result.success("手机验证码发送成功");
        }

        return Result.error("手机号不能为空");
    }

    @PostMapping("/loginMsg")
    @ApiOperation("用户手机登录")
    public Result<UserLoginVO> loginMsg(@RequestBody UserLoginDTO userLoginDTO){
        log.info("手机号: {}", userLoginDTO.getPhone());
        log.info("用户输入的验证码：{}", userLoginDTO.getCode());

        //手机号登录
        //1.从map中获取手机号和验证码
        String phoneNumber = (String) userLoginDTO.getPhone();
        String code = (String) userLoginDTO.getCode();

        //2.reids中获取验证码
        String sessionCode = (String) stringRedisTemplate.opsForValue().get(phoneNumber);
        log.info("redis中存储的正确的验证码:{}", sessionCode);

        //3.判断验证码是否正确
        if (null != sessionCode && sessionCode.equals(code)){
            //如果正确则返回登录成功
            //如果用户是首次登录，进行注册
            LambdaQueryWrapper<User> lambdaQueryWrapper  = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(User::getPhone, phoneNumber);
            User user = userService.getOne(lambdaQueryWrapper);

            if (null == user) {
                user = new User();
                user.setPhone(phoneNumber);
                user.setCreateTime(LocalDateTime.now());
                userService.save(user);
            }


            //生成jwt令牌
            Map<String, Object> claims = new HashMap<>();
            claims.put(JwtClaimsConstant.USER_ID,user.getId());
            String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

            UserLoginVO userLoginVO = UserLoginVO.builder()
                    .id(user.getId())
                    .phone(user.getPhone())
                    .token(token)
                    .build();

            return Result.success(userLoginVO);
        }

        //5.验证码错误
        throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
    }
}
