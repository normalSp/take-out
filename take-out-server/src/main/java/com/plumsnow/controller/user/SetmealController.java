package com.plumsnow.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plumsnow.constant.RedisKeyConstant;
import com.plumsnow.context.BaseContext;
import com.plumsnow.entity.Setmeal;
import com.plumsnow.result.Result;
import com.plumsnow.service.SetmealService;
import com.plumsnow.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "C端-套餐浏览接口")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 条件查询
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询套餐")
    //@Cacheable(value = "setmeal")
    public Result<List<Setmeal>> list(Long categoryId) {
        long shopId = RedisKeyConstant.getShopId(stringRedisTemplate, BaseContext.getCurrentId());

        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Setmeal::getCategoryId, categoryId);
        lambdaQueryWrapper.eq(Setmeal::getShopId, shopId);

        return Result.success(setmealService.list(lambdaQueryWrapper));
    }

    /**
     * 条件查询
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/listBy2Id")
    @ApiOperation("根据分类id和商店查询套餐")
    //@Cacheable(value = "setmeal")
    public Result<List<Setmeal>> listByCategoryIdAndShopId(Long categoryId, Long shopId) {
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Setmeal::getCategoryId, categoryId);
        lambdaQueryWrapper.eq(Setmeal::getShopId, shopId);

        return Result.success(setmealService.list(lambdaQueryWrapper));
    }

    /**
     * 根据套餐id查询包含的菜品列表
     *
     * @param id
     * @return
     */
    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品列表")
    public Result<List<DishItemVO>> dishList(@PathVariable("id") Long id) {
        List<DishItemVO> list = setmealService.getDishItemById(id);
        return Result.success(list);
    }
}
