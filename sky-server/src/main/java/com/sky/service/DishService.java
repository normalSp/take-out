package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.entity.Dish;
import com.sky.dto.DishDTO;

public interface DishService extends IService<Dish> {

    public void saveWithFlavor(DishDTO dishDto);
}
