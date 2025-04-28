package com.plumsnow.controller.user;


import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plumsnow.context.BaseContext;
import com.plumsnow.dto.BlogPageQueryDTO;
import com.plumsnow.result.PageResult;
import com.plumsnow.result.Result;
import com.plumsnow.dto.ScrollResult;
import com.plumsnow.dto.UserDTO;
import com.plumsnow.entity.Blog;
import com.plumsnow.entity.Follow;
import com.plumsnow.entity.User;
import com.plumsnow.service.BlogService;
import com.plumsnow.service.FollowService;
import com.plumsnow.service.UserService;
import com.plumsnow.utils.RedisConstants;
import com.plumsnow.utils.SystemConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
@RequestMapping("/user/blog")
@Api(tags = "C端-博客相关接口")
public class BlogController {

    private static final String LIKE_KEY = "blog:like:";

    @Resource
    private BlogService blogService;
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private FollowService followService;

    @PostMapping("/add")
    @ApiOperation("新增blog")
    public Result saveBlog(@RequestBody Blog blog) {
        // 获取登录用户
        UserDTO user = new UserDTO();
        user.setId(BaseContext.getCurrentId());
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean ifSuccess = blogService.save(blog);

        if(BooleanUtil.isFalse(ifSuccess)){
            return Result.error("保存失败");
        }

        // 返回id
        return Result.success(blog.getId());
    }

    @GetMapping("/getByShopId")
    @ApiOperation("分页查询店铺的blog")
    public Result queryByShopId(BlogPageQueryDTO blogPageQueryDTO) {
        // 根据用户查询
        Page<Blog> page = new Page<>(blogPageQueryDTO.getPage(), blogPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Blog> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Blog::getShopId, blogPageQueryDTO.getShopId());
        lambdaQueryWrapper.orderByDesc(Blog::getLiked);
        blogService.page(page, lambdaQueryWrapper);

        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        for(Blog blog : records){
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            if(user.getName() != null){
                blog.setName(user.getName());
            }
            if(user.getAvatar() != null){
                blog.setIcon(user.getAvatar());
            }

        }


        return Result.success(new PageResult(page.getTotal(), page.getRecords()));
    }


    @Transactional
    @GetMapping("/{id}")
    @ApiOperation("根据id查询blog")
    public Result queryBlogById(@PathVariable Long id){
        LambdaQueryWrapper<Blog> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Blog::getId, id);
        Blog blog = blogService.getOne(lambdaQueryWrapper);

        if (blog == null) {
            return Result.error("博文不存在");
        }

        User user = userService.getById(blog.getUserId());
        blog.setName(user.getName());
        blog.setIcon(user.getAvatar());

        return Result.success(blog);
    }


    @GetMapping("/of/user")
    @ApiOperation("根据用户id查询blog")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {

        LambdaQueryWrapper<Blog> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Blog::getUserId, id);

        Page<Blog> page1 = new Page<>(current, SystemConstants.MAX_PAGE_SIZE);

        blogService.page(page1, lambdaQueryWrapper);

        // 获取当前页数据
        List<Blog> records = page1.getRecords();
        return Result.success(records);
    }
}
