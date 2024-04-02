package com.sky.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.MessageConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import com.sky.service.DishFlavorService;
import com.sky.service.DishService;
import com.sky.service.impl.DishServiceImpl;
import com.sky.utils.AliOssUtil;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Api("菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private AliOssUtil aliOssUtil;
    @Autowired
    private DishService dishService;
    @Autowired
    private CategoryService categoryService;

    @PostMapping()
    @ApiOperation("菜品保存")
    @Transactional
    public Result<String> save(@RequestBody  DishDTO dishDTO){
        log.info("菜品保存信息：{}", dishDTO);

        dishService.saveWithFlavor(dishDTO);

        return Result.success(MessageConstant.SAVE_SUCCESS);
    }
    

    @GetMapping("/page")
    @ApiOperation("分页查询菜品")
    @Transactional
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
}
