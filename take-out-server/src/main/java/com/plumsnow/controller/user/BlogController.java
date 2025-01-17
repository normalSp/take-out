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
import com.plumsnow.service.IBlogService;
import com.plumsnow.service.IFollowService;
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
    private IBlogService blogService;
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IFollowService followService;

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


        //查询所有粉丝
        LambdaQueryWrapper<Follow> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Follow::getFollowUserId, user.getId());
        List<Follow> fanList = followService.list(lambdaQueryWrapper);

        if(fanList == null || fanList.isEmpty()){
            return Result.success(blog.getId());
        }

        //将blog推给所有粉丝
        for(Follow follow : fanList){
            String key = RedisConstants.FEED_KEY + follow.getUserId();

            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        }

        // 返回id
        return Result.success(blog.getId());
    }

    @PutMapping("/like/{id}")
    @ApiOperation("点赞blog")
    public Result likeBlog(@PathVariable("id") Long id) {
        String key = LIKE_KEY + id;
        //1. 获取当前登录用户
        Long userId = BaseContext.getCurrentId();

        //2. 判断当前用户是否点赞
        Double member = stringRedisTemplate.opsForZSet().score(key, userId.toString());

        //3. 如果未点赞
        if(member == null) {
            //3.1 数据库点赞数+1
            LambdaUpdateWrapper<Blog> lambdaUpdateWrapper =  new LambdaUpdateWrapper<>();
            lambdaUpdateWrapper.eq(Blog::getId, id);
            Blog blog = blogService.getOne(lambdaUpdateWrapper);

            blog.setLiked(blog.getLiked() + 1);

            boolean ifSuccess = blogService.update(blog, lambdaUpdateWrapper);

            if(BooleanUtil.isTrue(ifSuccess)){
                //3.2 保存用户到redis集合
                stringRedisTemplate.opsForZSet().add(LIKE_KEY + id, userId.toString(), System.currentTimeMillis());
            }
        }else {
            //4. 如果已点赞，取消点赞
            //4.1 数据库点赞数-1
            LambdaUpdateWrapper<Blog> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            lambdaUpdateWrapper.eq(Blog::getId, id);
            Blog blog = blogService.getOne(lambdaUpdateWrapper);

            blog.setLiked(blog.getLiked() - 1);

            boolean ifSuccess = blogService.update(blog, lambdaUpdateWrapper);

            if (BooleanUtil.isTrue(ifSuccess)) {
                //4.2 redis集合删除用户
                stringRedisTemplate.opsForZSet().remove(LIKE_KEY + id, userId.toString());
            }

        }
        return Result.success();
    }

    @GetMapping("/of/me")
    @ApiOperation("查询我的blog")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = new UserDTO();
        user.setId(BaseContext.getCurrentId());
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.success(records);
    }

    @GetMapping("/hot")
    @ApiOperation("查询最热blog")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getName());
            blog.setIcon(user.getAvatar());

            isBlogBeLike(blog);
        });
        return Result.success(records);
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

            isBlogBeLike(blog);
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

        isBlogBeLike(blog);

        return Result.success(blog);
    }

    private void isBlogBeLike(Blog blog) {
        String key = LIKE_KEY + blog.getId();
        //1. 获取当前登录用户id
        Long userId = BaseContext.getCurrentId();

        if(userId == null){
            return;
        }

        //2. 判断当前用户是否点赞
        Double member = stringRedisTemplate.opsForZSet().score(key, userId.toString());

        blog.setIsLike(member != null);
    }


    @GetMapping("/likes/{id}")
    @ApiOperation("新增地址")
    public Result queryBlogLikes(@PathVariable Long id){
        Set<String> range = stringRedisTemplate.opsForZSet().range(LIKE_KEY + id, 0, 4);

        if (range == null || range.isEmpty()) {
            return Result.success();
        }

        List<Long> collect = range.stream().map(Long::valueOf).collect(Collectors.toList());


        //因为直接用listById查的sql用的是in，导致用户是按id排序展示的，所以用query拼接字符串自定义排序
        //where id in (5, 1) order by field(id, 5 ,1)
        List<User> users = userService.query().
                in("id", collect).
                last("ORDER BY FIELD(id," +
                        collect.stream().map(Object::toString).collect(Collectors.joining(","))
                        + ")")
                .list();

        List<UserDTO> userDTOS = new ArrayList<>();

        for(User user : users){
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(user, userDTO);

            userDTOS.add(userDTO);
        }

        return Result.success(userDTOS);
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



    @GetMapping("/of/follow")
    @ApiOperation("查询关注者的blog")
    public Result queryBlogOfFollow(@RequestParam("lastId") Long max, @RequestParam(value = "offset", defaultValue = "0") Integer offset){
        Long userId = BaseContext.getCurrentId();

        //1. 查询收件箱
        String key = RedisConstants.FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);

        if(typedTuples == null || typedTuples.isEmpty()){
            return Result.success();
        }

        //2. 解析数据：blogId、minTime、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0L;
        int offsetGetFromRedis = 1;
        for(ZSetOperations.TypedTuple<String> typedTuple : typedTuples){
            String blogId = typedTuple.getValue();
            if (blogId != null) {
                ids.add(Long.valueOf(blogId));
            }

            long time = Objects.requireNonNull(typedTuple.getScore()).longValue();
            if(time == minTime){
                offsetGetFromRedis += 1;
            }else {
                minTime = time;
                offsetGetFromRedis = 1;
            }

        }

        //3. 根据id查blog
        //List<Blog> blogs = blogService.listByIds(ids);不能这样查
        //因为这样用的是in查询，顺序乱了
        List<Blog> blogs = new ArrayList<>(ids.size());
        for(Long id : ids){
            Blog blog = blogService.getById(id);

            if(blog != null){
                User user = userService.getById(blog.getUserId());
                blog.setName(user.getName());
                blog.setIcon(user.getAvatar());

                isBlogBeLike(blog);

                blogs.add(blog);
            }
        }

        //4. 封装返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(offsetGetFromRedis);
        scrollResult.setMinTime(minTime);

        return Result.success(scrollResult);
    }
}
