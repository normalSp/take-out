package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 动态条件查询菜品
     *
     * @param dish
     * @return
     */
    @Select("select * from dish where name like concat('%',#{name},'%') " +
            "and category_id = #{categoryId} " +
            "and status = #{status} " +
            "order by create_time desc")
    List<Dish> list(Dish dish);

}
