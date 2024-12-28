package com.plumsnow.controller.user;

import com.plumsnow.constant.RedisKeyConstant;
import com.plumsnow.context.BaseContext;
import com.plumsnow.entity.Category;
import com.plumsnow.result.Result;
import com.plumsnow.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userCategoryController")
@RequestMapping("/user/category")
@Api(tags = "C端-分类接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询分类
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询分类")
    @Cacheable(value = "category")
    public Result<List<Category>> list(Integer type) {
        long shopId = RedisKeyConstant.getShopId(stringRedisTemplate, BaseContext.getCurrentId());

        List<Category> list = categoryService.list(type, shopId);

        return Result.success(list);
    }

    /**
     * 附加shopId查询分类
     * @param type
     * @return
     */
    @GetMapping("/listByShopId")
    @ApiOperation("查询分类")
    @Cacheable(value = "category")
    public Result<List<Category>> listByShopId(Integer type, Long shopId) {
        List<Category> list = categoryService.list(type, shopId);

        return Result.success(list);
    }
}
