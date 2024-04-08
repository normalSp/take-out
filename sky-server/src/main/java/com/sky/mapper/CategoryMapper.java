package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper{

    /**
     * 插入数据
     *
     * @param category
     * @return
     */
    @Insert("insert into category(type, name, sort, status, create_time, update_time, create_user, update_user)" +
            " VALUES" +
            " (#{type}, #{name}, #{sort}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    int insert(Category category);

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @Select(" SELECT * FROM category WHERE ((name like concat('%',#{name},'%')) AND (type LIKE concat('%',#{type}))) ")
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 分页查询1
     * @param categoryPageQueryDTO
     * @return
     */
    @Select(" SELECT * FROM category WHERE ((name like concat('%',#{name},'%')) AND (type LIKE concat('%'))) ")
    Page<Category> pageQuery1(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据id删除分类
     * @param id
     */
    @Delete("delete from category where id = #{id}")
    void deleteById(Long id);

    /**
     * 根据id修改分类
     * @param category
     */
    @Update("UPDATE category\n" +
            "SET\n" +
            "  type = #{type},\n" +
            "  name = #{name},\n" +
            "  sort = #{sort},\n" +
            "  status = #{status},\n" +
            "  update_time = #{updateTime},\n" +
            "  update_user = #{updateUser}\n" +
            "WHERE id = #{id}")
    void update(Category category);

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    @Select("SELECT * FROM category WHERE status = 1 AND type = #{type}")
    List<Category> list(Integer type);

    @Select("SELECT * FROM category WHERE status = 1 AND type like concat('%')")
    List<Category> listWithoutType();

    /**
     * 根据id查询分类
     * @param id
     * @return
     */
    @Select("Select * from category where id = #{id}")
    Category selectById(Long id);

}
