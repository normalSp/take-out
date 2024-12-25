package com.plumsnow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plumsnow.entity.Dish;
import com.plumsnow.dto.DishDTO;
import com.plumsnow.vo.DishVO;

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
