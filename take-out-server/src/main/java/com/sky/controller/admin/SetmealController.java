package com.sky.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import com.sky.service.SetmealDishService;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/admin/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CategoryService categoryService;

    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    //@Cacheable(value = "setmeal")
    @Transactional
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("套餐分页查询:{}",setmealPageQueryDTO);
        Page page = new Page(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());

        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(StringUtils.isNotBlank(setmealPageQueryDTO.getName()),
                Setmeal::getName, setmealPageQueryDTO.getName());
        lambdaQueryWrapper.eq(null != setmealPageQueryDTO.getCategoryId(),
                Setmeal::getCategoryId, setmealPageQueryDTO.getCategoryId());
        lambdaQueryWrapper.eq(null != setmealPageQueryDTO.getStatus(),
                Setmeal::getStatus, setmealPageQueryDTO.getStatus());
        lambdaQueryWrapper.eq(Setmeal::getShopId, BaseContext.getCurrentShopId());
        lambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);

        setmealService.page(page, lambdaQueryWrapper);

        List<SetmealVO> setmealVOList = new ArrayList<>();
        List<Setmeal> records = page.getRecords();

        for(Setmeal setmeal : records){
            SetmealVO setmealVO = new SetmealVO();
            BeanUtils.copyProperties(setmeal,setmealVO);

            Category category = categoryService.getById(setmeal.getCategoryId());

            setmealVO.setCategoryName(category.getName());

            setmealVOList.add(setmealVO);
        }

        PageResult pageResult = new PageResult();
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(setmealVOList);

        return Result.success(pageResult);
    }
    
    @PostMapping
    @Transactional
    @CacheEvict(value = "setmeal",allEntries = true)
    @ApiOperation("新增套餐")
    public Result<String> save(@RequestBody SetmealDTO setmealDTO){
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmeal.setShopId(BaseContext.getCurrentShopId());

        setmealService.save(setmeal);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();

        for(SetmealDish setmealDish :  setmealDishes){
            setmealDish.setSetmealId(setmeal.getId());
        }

        setmealDishService.saveBatch(setmealDishes);

        return Result.success(MessageConstant.SAVE_SUCCESS);
    }
    

    @GetMapping("/{id}")
    @Transactional
    @ApiOperation("根据id查询套餐")
    public Result<SetmealVO> getById(@PathVariable Long id){
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Setmeal::getId,id);
        lambdaQueryWrapper.eq(Setmeal::getShopId, BaseContext.getCurrentShopId());

        Setmeal setmeal = setmealService.getOne(lambdaQueryWrapper);

        if(null == setmeal){
            return Result.error(MessageConstant.SETMEAL_NOT_EXIST);
        }

        SetmealVO setmealVO =  new SetmealVO();

        BeanUtils.copyProperties(setmeal,setmealVO);

        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
        lambdaQueryWrapper1.eq(SetmealDish::getSetmealId,setmeal.getId());

        List<SetmealDish> setmealDishes = setmealDishService.list(lambdaQueryWrapper1);

        setmealVO.setSetmealDishes(setmealDishes);

        return Result.success(setmealVO);
    }

    @PutMapping
    @Transactional
    @CacheEvict(value = "setmeal",allEntries = true)
    @ApiOperation("修改套餐")
    public Result<String> update(@RequestBody SetmealDTO setmealDTO){
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        setmealService.updateById(setmeal);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();

        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SetmealDish::getSetmealId,setmeal.getId());

        setmealDishService.remove(lambdaQueryWrapper);

        for(SetmealDish setmealDish : setmealDishes){
            setmealDish.setSetmealId(setmeal.getId());
        }

        setmealDishService.saveBatch(setmealDishes);

        return Result.success(MessageConstant.UPDATE_SUCCESS);
    }

    @DeleteMapping()
    @Transactional
    @CacheEvict(value = "setmeal",allEntries = true)
    @ApiOperation("删除套餐")
    public Result<String> deletes(Long[] ids){
        setmealService.removeByIds(Arrays.asList(ids));

        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);

        setmealDishService.remove(lambdaQueryWrapper);

        return Result.success(MessageConstant.DELETE_SUCCESS);

    }

    @PostMapping("/status/{status}")
    @ApiOperation("启用或禁用套餐")
    @CacheEvict(value = "setmeal",allEntries = true)
    public Result<String> forbidOrEnable(@PathVariable Integer status, Long id){
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Setmeal::getId,id);

        Setmeal setmeal = setmealService.getOne(lambdaQueryWrapper);

        if(null != setmeal){
            setmeal.setStatus(status);
            setmealService.updateById(setmeal);
            return Result.success(MessageConstant.UPDATE_SUCCESS);
        }

        return Result.error(MessageConstant.SETMEAL_NOT_EXIST);
    }

}
