package com.comment.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comment.dto.Result;
import com.comment.dto.UserDTO;
import com.comment.entity.Follow;
import com.comment.entity.User;
import com.comment.service.IFollowService;
import com.comment.service.IUserService;
import com.comment.utils.UserHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author plumsnow
 * @since 2024
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private IFollowService followService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IUserService userService;

    @PutMapping("/{id}/{isFollower}")
    public Result follower(@PathVariable Long id, @PathVariable Boolean isFollower){
        Long userId = UserHolder.getUser().getId();

        if(isFollower){
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(id);

            boolean isSuccess = followService.save(follow);

            if(isSuccess){
                stringRedisTemplate.opsForSet().add("follow:"+userId, id.toString());
            }

        }else {
            LambdaQueryWrapper<Follow> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Follow::getUserId,userId);
            lambdaQueryWrapper.eq(Follow::getFollowUserId,id);

            boolean ifSuccess = followService.remove(lambdaQueryWrapper);

            if(ifSuccess){
                stringRedisTemplate.opsForSet().remove("follow:"+userId, id.toString());
            }

        }

        return Result.ok();
    }


    @GetMapping("/or/not/{id}")
    public Result follower(@PathVariable Long id){
        Long userId = UserHolder.getUser().getId();

        LambdaQueryWrapper<Follow> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Follow::getUserId,userId);
        lambdaQueryWrapper.eq(Follow::getFollowUserId,id);

        Follow one = followService.getOne(lambdaQueryWrapper);

        if(one == null){
            return Result.ok(false);
        }

        return Result.ok(true);
    }


    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable Long id){
        Long userId = UserHolder.getUser().getId();

        //求set交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect("follow:" + userId, "follow:" + id);

        //解析出Long型id
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        List<Long> collect = intersect.stream().map(Long::valueOf).collect(Collectors.toList());

        List<User> users = userService.listByIds(collect);
        List<UserDTO> userDTOS = new ArrayList<>();

        for (User user : users) {
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(user, userDTO);
            userDTOS.add(userDTO);
        }

        return Result.ok(userDTOS);
    }
}
