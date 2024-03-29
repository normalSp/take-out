package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Employee;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

/*    @Insert("insert into employee(username,name,phone,sex,id_number,password,status,create_time,update_time,create_user,update_user) " +
            "values(#{username},#{name},#{phone},#{sex},#{idNumber},#{password},#{status},#{createTime},#{updateTime},#{createUser},#{updateUser})")
    int insert(Employee employee);*/
}
