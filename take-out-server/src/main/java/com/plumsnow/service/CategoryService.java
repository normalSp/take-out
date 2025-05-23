package com.plumsnow.service;

import com.plumsnow.dto.CategoryDTO;
import com.plumsnow.dto.CategoryPageQueryDTO;
import com.plumsnow.entity.Category;
import com.plumsnow.result.PageResult;
import java.util.List;

public interface CategoryService{

    /**
     * 新增分类
     * @param category
     */
    void save(Category category);

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据id删除分类
     * @param id
     */
    void deleteById(Long id);

    /**
     * 修改分类
     * @param categoryDTO
     */
    void update(CategoryDTO categoryDTO);

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    List<Category> list(Integer type);

    /**
     * 根据id查询分类
     * @param id
     * @return
     */
    Category getById(Long id);

    /**
     * 根据id查询分类
     * @param type
     * @param shopId
     * @return
     */
    List<Category> list(Integer type, Long shopId);
}
