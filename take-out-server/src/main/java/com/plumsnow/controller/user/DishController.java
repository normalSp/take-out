package com.plumsnow.controller.user;

import com.plumsnow.constant.RedisKeyConstant;
import com.plumsnow.constant.StatusConstant;
import com.plumsnow.context.BaseContext;
import com.plumsnow.entity.Dish;
import com.plumsnow.result.Result;
import com.plumsnow.service.DishService;
import com.plumsnow.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        long shopId = RedisKeyConstant.getShopId(stringRedisTemplate, BaseContext.getCurrentId());
        dish.setShopId(shopId);

        List<DishVO> list = dishService.listWithFlavor(dish);

        return Result.success(list);
    }

    /**
     * 根据分类id和商店id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/listBy2Id")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> listBy2Id(Long categoryId, Long shopId) {
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        //long shopId = RedisKeyConstant.getShopId(stringRedisTemplate, BaseContext.getCurrentId());
        dish.setShopId(shopId);

        List<DishVO> list = dishService.listWithFlavor(dish);

        return Result.success(list);
    }

}
