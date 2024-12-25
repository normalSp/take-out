package com.plumsnow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plumsnow.dto.DishDTO;
import com.plumsnow.entity.Dish;
import com.plumsnow.entity.DishFlavor;
import com.plumsnow.mapper.DishFlavorMapper;
import com.plumsnow.mapper.DishMapper;
import com.plumsnow.mapper.SetmealDishMapper;
import com.plumsnow.mapper.SetmealMapper;
import com.plumsnow.service.DishFlavorService;
import com.plumsnow.service.DishService;
import com.plumsnow.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    
    /**
     * 新增菜品，同时保存对应口味信息
     * 由于需要同时操作两张表
     * @Transactional: 开启事务管理
     * @param dishDto
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDto) {
        Dish dish = new Dish();
        dish.setId(dishDto.getId());
        dish.setName(dishDto.getName());
        dish.setPrice(dishDto.getPrice());
        dish.setDescription(dishDto.getDescription());
        dish.setCategoryId(dishDto.getCategoryId());
        dish.setImage(dishDto.getImage());
        dish.setStatus(dishDto.getStatus());
        dish.setShopId(dishDto.getShopId());

        //保存菜品的基本信息到菜品表dish
        log.info("往dish表中插入信息...");
        super.save(dish);

        //由于flavors集合中的dishId没有赋值，所以需要手动赋值
        List<DishFlavor> flavors = dishDto.getFlavors();
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dish.getId());
        }

        //保存菜品的口味信息到菜品口味表dish_flavor
        log.info("往dish_flavor表中插入信息...");
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        if(null == dish.getName()){
            dish.setName("");
        }
        if(null == dish.getCategoryId() && null != dish.getStatus()){
            List<Dish> dishList = dishMapper.listWithoutCategoryId(dish);
        } else if (null != dish.getCategoryId() && null == dish.getStatus()) {
            List<Dish> dishList = dishMapper.listWithoutStatus(dish);
        } else if (null == dish.getCategoryId() && null == dish.getStatus()) {
            List<Dish> dishList = dishMapper.listWithoutStatusAndCategoryId(dish);
        }

        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
