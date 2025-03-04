package com.plumsnow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plumsnow.entity.Setmeal;
import com.plumsnow.mapper.DishMapper;
import com.plumsnow.mapper.SetmealDishMapper;
import com.plumsnow.mapper.SetmealMapper;
import com.plumsnow.service.SetmealService;
import com.plumsnow.vo.DishItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        if(null == setmeal.getCategoryId() && null!= setmeal.getStatus()){
            List<Setmeal> list = setmealMapper.listWithoutCategoryId(setmeal);
        } else if (null != setmeal.getCategoryId() && null == setmeal.getStatus()) {
            List<Setmeal> list = setmealMapper.listWithoutStatus(setmeal);
        } else if (null == setmeal.getCategoryId() && null == setmeal.getStatus()) {
            List<Setmeal> list = setmealMapper.listWithoutCategoryIdAndStatus(setmeal);
        }
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }


}
