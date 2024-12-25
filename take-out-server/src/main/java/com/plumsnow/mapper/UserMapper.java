package com.plumsnow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.plumsnow.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据动态条件统计用户数量
     * @param map
     * @return
     */
    @Select("select count(id) from user where create_time > #{begin} and create_time < #{end}")
    Integer countByMap(Map map);
}
