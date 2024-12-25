package com.plumsnow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.plumsnow.dto.EmployeeLoginDTO;
import com.plumsnow.entity.Employee;

public interface EmployeeService extends IService<Employee> {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

}
