package com.plumsnow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plumsnow.entity.Setmeal;
import com.plumsnow.vo.DishItemVO;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);

}
