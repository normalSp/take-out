package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.entity.Dish;
import com.sky.dto.DishDTO;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService extends IService<Dish> {

    public void saveWithFlavor(DishDTO dishDto);

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);
}
