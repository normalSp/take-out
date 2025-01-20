package com.plumsnow.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.plumsnow.constant.MessageConstant;
import com.plumsnow.constant.StatusConstant;
import com.plumsnow.context.BaseContext;
import com.plumsnow.dto.CategoryDTO;
import com.plumsnow.dto.CategoryPageQueryDTO;
import com.plumsnow.entity.Category;
import com.plumsnow.exception.DeletionNotAllowedException;
import com.plumsnow.mapper.CategoryMapper;
import com.plumsnow.mapper.DishMapper;
import com.plumsnow.mapper.SetmealMapper;
import com.plumsnow.result.PageResult;
import com.plumsnow.service.CategoryService;
import com.plumsnow.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类业务层
 */
@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private AliOssUtil aliOssUtil;
    /**
     * 新增分类
     * @param categoryDTO
     */
    public void save(Category categoryDTO) {
        Category category = new Category();
        //属性拷贝
        BeanUtils.copyProperties(categoryDTO, category);

        //分类状态默认为禁用状态0
        category.setStatus(StatusConstant.DISABLE);

        //设置创建时间、修改时间、创建人、修改人
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setCreateUser(BaseContext.getCurrentId());
        category.setUpdateUser(BaseContext.getCurrentId());
        category.setShopId(categoryDTO.getShopId());

        categoryMapper.insert(category);
    }

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize());
        //下一条sql进行分页，自动加入limit关键字分页
        if(null == categoryPageQueryDTO.getName()){
            categoryPageQueryDTO.setName("%");
        }
        if(null == categoryPageQueryDTO.getType()){
            Page<Category> page = categoryMapper.pageQuery1(categoryPageQueryDTO);
            log.info("分页查询分类数据：{}",page);
            return new PageResult(page.getTotal(), page.getResult());
        }
        Page<Category> page = categoryMapper.pageQuery(categoryPageQueryDTO);
        log.info("分页查询分类数据：{}",page);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 根据id删除分类
     * @param id
     */
    public void deleteById(Long id) {
        //查询当前分类是否关联了菜品，如果关联了就抛出业务异常
        Integer count = dishMapper.countByCategoryId(id);
        if(count > 0){
            //当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        //查询当前分类是否关联了套餐，如果关联了就抛出业务异常
        count = setmealMapper.countByCategoryId(id);
        if(count > 0){
            //当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        //删除分类数据
        categoryMapper.deleteById(id);
    }

    /**
     * 修改分类
     * @param categoryDTO
     */
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO,category);

        //设置修改时间、修改人
        category.setUpdateTime(LocalDateTime.now());
        category.setUpdateUser(BaseContext.getCurrentId());

        Category oldCategory = categoryMapper.selectById(category.getId());
        category.setType(categoryDTO.getType());
        category.setStatus(oldCategory.getStatus());

        log.info("修改分类数据：{}",category);

        categoryMapper.update(category);
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Category oldCategory = categoryMapper.selectById(id);

        Category category = Category.builder()
                .id(id)
                .status(status)
                .updateTime(LocalDateTime.now())
                .updateUser(BaseContext.getCurrentId())
                .type(oldCategory.getType())
                .name(oldCategory.getName())
                .sort(oldCategory.getSort())
                .build();
        categoryMapper.update(category);
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    public List<Category> list(Integer type) {
        if(null != type){
            return categoryMapper.list(type, BaseContext.getCurrentShopId());
        }
        return categoryMapper.listWithoutType(BaseContext.getCurrentShopId());
    }

    /**
     * 根据id查询分类
     * @param id
     * @return
     */
    @Override
    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }

    /**
     * 用户端根据类型查询分类
     * @param type
     * @param shopId
     * @return
     */
    @Override
    public List<Category> list(Integer type, Long shopId) {
        if(null != type){
            return categoryMapper.list(type, shopId);
        }
        return categoryMapper.listWithoutType(shopId);
    }
}
