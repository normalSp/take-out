package com.plumsnow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plumsnow.entity.Setmeal;
import com.plumsnow.vo.DishItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SetmealMapper extends BaseMapper<Setmeal> {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    /**
     * 动态条件查询套餐
     * @param setmeal
     * @return
     */
    @Select("select * from setmeal where name like concat('%',#{name},'%') and category_id = #{categoryId} and status = #{status} order by update_time desc;")
    List<Setmeal> list(Setmeal setmeal);

    @Select("select * from setmeal where name like concat('%',#{name},'%') and status = #{status} order by update_time desc;")
    List<Setmeal> listWithoutCategoryId(Setmeal setmeal);

    @Select("select * from setmeal where name like concat('%',#{name},'%') and category_id = #{categoryId} order by update_time desc;")
    List<Setmeal> listWithoutStatus(Setmeal setmeal);

    @Select("select * from setmeal where name like concat('%',#{name},'%')order by update_time desc;")
    List<Setmeal> listWithoutCategoryIdAndStatus(Setmeal setmeal);

    /**
     * 根据套餐id查询菜品选项
     * @param setmealId
     * @return
     */
    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from setmeal_dish sd left join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{setmealId}")
    List<DishItemVO> getDishItemBySetmealId(Long setmealId);


    /**
     * 根据条件统计套餐数量
     * @param map
     * @return
     */
    @Select("select count(id) from setmeal where status = #{map.status} AND shop_id = #{shopId}")
    Integer countByMap(Map map, Long shopId);

}
