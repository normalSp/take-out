package com.sky.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.exception.PasswordErrorException;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success();
    }

    /**
     * 新增员工
     * @return
     */
    @ApiOperation("新增员工")
    @PostMapping
    @CacheEvict(value = "employee", allEntries = true)
    public Result<String> save(@RequestBody EmployeeDTO employeeDTO) {
        log.info("新增员工：{}", employeeDTO);

        //设置初始信息
        //初始密码设置为123456，md5加密
        Employee employee = Employee.builder()
                .username(employeeDTO.getUsername())
                .name(employeeDTO.getName())
                .phone(employeeDTO.getPhone())
                .sex(employeeDTO.getSex())
                .idNumber(employeeDTO.getIdNumber())
                .password(DigestUtils.md5Hex(PasswordConstant.DEFAULT_PASSWORD))
                .status(StatusConstant.ENABLE)
                .shopId(BaseContext.getCurrentShopId())
                .build();

        employeeService.save(employee);


        return Result.success("新增成功");
    }

    @GetMapping("/page")
    @ApiOperation("分页查询员工")
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO){
        log.info("分页查询员工：{}", employeePageQueryDTO);

        Page page = new Page(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());

        LambdaQueryWrapper<Employee> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        lambdaQueryWrapper.like(StringUtils.isNotBlank(employeePageQueryDTO.getName()),
                Employee::getName, employeePageQueryDTO.getName());

        //仅查询当前商家的所属员工
        Long currentShopId = BaseContext.getCurrentShopId();
        lambdaQueryWrapper.eq(Employee::getShopId, currentShopId);

        lambdaQueryWrapper.orderByDesc(Employee::getUpdateTime);

        employeeService.page(page, lambdaQueryWrapper);

        return Result.success(new PageResult(page.getTotal(), page.getRecords()));
    }

    /**
     * 禁用账号
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("禁用/启用账号")
    @CacheEvict(value = "employee", allEntries = true)
    public Result<String> forbidOrEnable(@PathVariable Integer status,Long id){
        log.info("账号状态：{}", status);
        log.info("账号id：{}", id);

        LambdaQueryWrapper<Employee> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Employee::getId, id);

        Employee employee = employeeService.getOne(lambdaQueryWrapper);

        if(null != employee){
            if(Objects.equals(status, StatusConstant.ENABLE)){
                employee.setStatus(StatusConstant.ENABLE);
                employeeService.updateById(employee);
                return Result.success(MessageConstant.ACTIVATED_SUCCEED);
            }
            employee.setStatus(StatusConstant.DISABLE);
            employeeService.updateById(employee);
            return Result.success(MessageConstant.LOCKED_SUCCEED);
        }

        return Result.error(MessageConstant.ACCOUNT_NOT_FOUND);
    }

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询员工信息")
    public Result<Employee> getEmployeeById(@PathVariable Long id) {
        log.info("根据id查询员工信息：{}", id);

        LambdaQueryWrapper<Employee> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Employee::getId, id);
        lambdaQueryWrapper.eq(Employee::getShopId, BaseContext.getCurrentShopId());
        Employee employee = employeeService.getOne(lambdaQueryWrapper);

        return Result.success(employee);
    }

    /**
     * 修改员工信息
     * @param employeeDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改员工信息")
    @CacheEvict(value = "employee", allEntries = true)
    public Result<String> update(@RequestBody EmployeeDTO employeeDTO) {
        log.info("修改员工信息：{}", employeeDTO);

        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);

        if(null != employee.getId()){
            employeeService.updateById(employee);
            return Result.success(MessageConstant.ACCOUNT_EDIT_SUCCEED);
        }
        return Result.error(MessageConstant.ACCOUNT_NOT_FOUND);
    }

    @PutMapping("/editPassword")
    @ApiOperation("修改密码")
    public Result<String> editPassword(@RequestBody PasswordEditDTO passwordEditDTO){
        passwordEditDTO.setEmpId(BaseContext.getCurrentId());

        LambdaQueryWrapper<Employee> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Employee::getId, passwordEditDTO.getEmpId());

        Employee employee = employeeService.getOne(lambdaQueryWrapper);

        if(null == employee){
            return Result.error(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if(!Objects.equals(employee.getPassword(), DigestUtils.md5Hex(passwordEditDTO.getOldPassword()))){
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        employee.setPassword(DigestUtils.md5Hex(passwordEditDTO.getNewPassword()));
        employeeService.updateById(employee);

        return Result.success(MessageConstant.UPDATE_SUCCESS);
    }
}
