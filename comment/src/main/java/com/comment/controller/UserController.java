package com.comment.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comment.dto.LoginFormDTO;
import com.comment.dto.Result;
import com.comment.dto.UserDTO;
import com.comment.entity.User;
import com.comment.entity.UserInfo;
import com.comment.service.IUserInfoService;
import com.comment.service.IUserService;
import com.comment.utils.RegexUtils;
import com.comment.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.comment.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author plumsnow
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {

        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }

        String code = RandomUtil.randomNumbers(6);

        //session.setAttribute("code", code);

        stringRedisTemplate.opsForValue().set("login:code:" + phone, code, 3, TimeUnit.MINUTES);


        log.info("手机验证码：{}", code);

        return Result.ok();
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){

        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("手机号格式错误");
        }

        //1. 从redis获取验证码并校验
        String code = stringRedisTemplate.opsForValue().get("login:code:" + loginForm.getPhone());

        if(code == null || !code.equals(loginForm.getCode())){
            return Result.fail("验证码错误");
        }


        LambdaQueryWrapper<User> lambdaQueryWrapper  = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getPhone, loginForm.getPhone());

        User user = userService.getOne(lambdaQueryWrapper);

        if(user == null){
            user = new User();
            user.setPassword(loginForm.getPassword());
            user.setPhone(loginForm.getPhone());
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(6));

            userService.save(user);
        }

        //保存用户信息到redis
        //2. 随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString();

        //3. 将User对象转为HashMap存储
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO);

        String id = String.valueOf(userMap.get("id"));

        userMap.put("id", id);

        //4. 存储
        stringRedisTemplate.opsForHash().putAll("login:token:" + token, userMap);
        //设置有效期
        stringRedisTemplate.expire("login:token:" + token, 30, TimeUnit.MINUTES);

        //5. 返回token到前端
        return Result.ok(token);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    @GetMapping("/me")
    public Result me(){

        UserDTO userDTO = UserHolder.getUser();

        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getId, userDTO.getId());

        User user = userService.getOne(lambdaQueryWrapper);

        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }


    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }


    @PostMapping("/sign")
    public Result sign(){
        Long userId = UserHolder.getUser().getId();

        //获取日期
        LocalDateTime now = LocalDateTime.now();
        String yyyyMM = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));

        String key = "sign:" + userId + yyyyMM;

        int dayOfMonth = now.getDayOfMonth() - 1;

        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth, true);

        return Result.ok();
    }


    @GetMapping("/sign/count")
    public Result singCount(){
        Long userId = UserHolder.getUser().getId();

        //获取日期
        LocalDateTime now = LocalDateTime.now();
        String yyyyMM = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));

        String key = "sign:" + userId + yyyyMM;

        int dayOfMonth = now.getDayOfMonth() - 1;

        //获取本月截止今天为止的所有签到记录，返回的是十进制
        //BITFIELD sign:userId:yyyMM GET u今天几号 0
        //因为这个命令是可以同时存在多个子命令（get、set。。）（所以返回一个list包含以上子命令的所有结果）
        List<Long> results = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)

                );
        if (results == null || results.isEmpty()) {
            return Result.ok(0);
        }

        //获取get子命令的结果
        Long num = results.get(0);
        if(num == null || num == 0){
            return Result.ok(0);
        }

        //将结果和1按位做与运算，count就是本月连续签到数
        int count = 0;
        for(; (num & 1) == 1; count++){
            //num无符号位右移一位
            num = num >>> 1;
        }

        return Result.ok(count);
    }

}
