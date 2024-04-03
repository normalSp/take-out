package com.sky.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import com.sky.service.DishFlavorService;
import com.sky.service.DishService;
import com.sky.service.SetmealDishService;
import com.sky.utils.AliOssUtil;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Api("菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private DishFlavorService dishFlavorService;

    @PostMapping()
    @ApiOperation("菜品保存")
    @Transactional
    @CacheEvict(value = "dish", key = "#dishDTO.categoryId")
    public Result<String> save(@RequestBody  DishDTO dishDTO){
        log.info("菜品保存信息：{}", dishDTO);

        dishService.saveWithFlavor(dishDTO);

        return Result.success(MessageConstant.SAVE_SUCCESS);
    }
    

    @GetMapping("/page")
    @ApiOperation("分页查询菜品")
    @Transactional
    @Cacheable(value = "dish", key = "#dishPageQueryDTO.categoryId")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页查询菜品：{}", dishPageQueryDTO);

        Page<Dish> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        LambdaQueryWrapper<Dish> lambdaQueryWrapper =new LambdaQueryWrapper<>();

        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);

        lambdaQueryWrapper.like(StringUtils.isNotBlank(dishPageQueryDTO.getName()),
                Dish::getName, dishPageQueryDTO.getName());

        lambdaQueryWrapper.eq(null != dishPageQueryDTO.getCategoryId(),
                Dish::getCategoryId, dishPageQueryDTO.getCategoryId());
        lambdaQueryWrapper.eq(null != dishPageQueryDTO.getStatus(),
                Dish::getStatus, dishPageQueryDTO.getStatus());

        dishService.page(page, lambdaQueryWrapper);

        List<DishVO> dishVOList = new ArrayList<>();

        Category category;
        List<Dish> dishList = page.getRecords();

        for (Dish dish : dishList) {
            DishVO dishVO = new DishVO();
            category = categoryService.getById(dish.getCategoryId());

            dishVO.setCategoryName(category.getName());
            dishVO.setId(dish.getId());
            dishVO.setName(dish.getName());
            dishVO.setPrice(dish.getPrice());
            dishVO.setStatus(dish.getStatus());
            dishVO.setUpdateTime(dish.getUpdateTime());
            dishVO.setStatus(dish.getStatus());
            dishVO.setImage(dish.getImage());
            dishVO.setDescription(dish.getDescription());

            dishVOList.add(dishVO);
        }

        PageResult pageResult = new PageResult(page.getTotal(), dishVOList);

        return Result.success(pageResult);
    }


    @DeleteMapping()
    @Transactional
    @ApiOperation("删除菜品")
    @CacheEvict(value = "dish", allEntries = true)
    public Result<String> deletes(Long[] ids){
        log.info("调用删除菜品方法，传入的菜品id --> ids:{}", ids);

        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getDishId, Arrays.asList(ids));

        SetmealDish setmealDish = setmealDishService.getOne(lambdaQueryWrapper);

        if(null != setmealDish){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        for (Long id : ids) {
            Dish dish = dishService.getById(id);
            if(StatusConstant.DISABLE.equals(dish.getStatus())){
                dishService.removeById(id);
                LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
                lambdaQueryWrapper1.eq(DishFlavor::getDishId, id);
                dishFlavorService.remove(lambdaQueryWrapper1);
            }
            else{
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        return Result.success(MessageConstant.DELETE_SUCCESS);
    }
    
    @PostMapping("/status/{status}")
    @ApiOperation("菜品启售或禁售")
    @CacheEvict(value = "dish", allEntries = true)
    public Result<String> forbidOrEnable(@PathVariable Integer status, Long id){
        log.info("调用菜品启售或禁售方法，传入的菜品id --> id:{}, 状态 --> status:{}", id, status);

        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Dish::getId, id);

        Dish dish = dishService.getOne(lambdaQueryWrapper);

        if(null == dish){
            return Result.error(MessageConstant.DISH_NOT_EXIST);
        }

        dish.setStatus(status);

        dishService.updateById(dish);

        return Result.success(MessageConstant.UPDATE_SUCCESS);

    }

    @GetMapping("/{id}")
    @Transactional
    @ApiOperation("根据id查询菜品和口味信息")
    public Result<DishVO> getById(@PathVariable Long id){
        LambdaQueryWrapper<Dish> lambdaQueryWrapper =  new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Dish::getId, id);

        Dish dish = dishService.getOne(lambdaQueryWrapper);

        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId, id);

        List<DishFlavor> dishFlavorList = dishFlavorService.list(dishFlavorLambdaQueryWrapper);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavorList);

        return Result.success(dishVO);
    }

    @PutMapping()
    @ApiOperation("修改菜品")
    @Transactional
    @CacheEvict(value = "dish", key = "#dishDTO.categoryId")
    public Result<String> update(@RequestBody DishDTO dishDTO){
        List<DishFlavor> dishFlavorList = dishDTO.getFlavors();

        for(DishFlavor dishFlavor : dishFlavorList){
            dishFlavor.setDishId(dishDTO.getId());
        }

        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO, dish);
        dishService.updateById(dish);

        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(DishFlavor::getDishId, dish.getId());

        dishFlavorService.remove(lambdaQueryWrapper);

        dishFlavorService.saveBatch(dishFlavorList);

        return Result.success(MessageConstant.UPDATE_SUCCESS);
    }


    @GetMapping ("/list")
    @ApiOperation("根据分类id查询菜品")
    @CacheEvict(value = "dish", key = "#dishDTO.categoryId")
    public Result<List<Dish>> getByCategoryId(Long categoryId){
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Dish::getCategoryId, categoryId);
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);

        List<Dish> dishList = dishService.list(lambdaQueryWrapper);

        return Result.success(dishList);
    }
}
