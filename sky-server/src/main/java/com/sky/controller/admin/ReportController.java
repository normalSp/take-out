package com.sky.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.result.Result;
import com.sky.service.OrdersService;
import com.sky.service.UserService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("admin/report")
@Api(tags = "数据统计相关接口")
@Slf4j
public class ReportController {
    @Autowired
    private OrdersService ordersService;
    @Autowired
    private UserService userService;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd")  LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){

        List<LocalDate> dataList = new ArrayList<>();

        LocalDate l = begin;
        while(!l.isEqual(end)){
            l = l.plusDays(1);
            dataList.add(l);
        }

        String join = StringUtils.join(dataList, ",");

        LambdaQueryWrapper<Orders> lambdaQueryWrapper  = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.between(Orders::getOrderTime, begin, end);
        List<Orders> ordersList = ordersService.list(lambdaQueryWrapper);

        List<BigDecimal> turnoverList = new ArrayList<>();
        for(LocalDate date : dataList){
            LocalDateTime localDateTimeMax = LocalDateTime.of(date, LocalTime.MAX);
            LocalDateTime localDateTimeMin = LocalDateTime.of(date, LocalTime.MIN);

            BigDecimal turnover = new BigDecimal(0);

            for(Orders orders : ordersList){
                if (Objects.equals(orders.getStatus(), Orders.COMPLETED)) {
                    if (orders.getOrderTime().isAfter(localDateTimeMin) && orders.getOrderTime().isBefore(localDateTimeMax)) {
                        turnover = turnover.add(orders.getAmount());
                    }
                }
            }

            turnoverList.add(turnover);
        }

        String turnoverJoin = StringUtils.join(turnoverList, ",");

        return Result.success(new TurnoverReportVO(join, turnoverJoin));
    }


    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd")  LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){

        List<LocalDate> dataList = new ArrayList<>();

        LocalDate l = begin;
        while(!l.isEqual(end)){
            l = l.plusDays(1);
            dataList.add(l);
        }

        String dateJoin = StringUtils.join(dataList, ",");

        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.between(User::getCreateTime, begin, end);
        List<User> userList = userService.list(lambdaQueryWrapper);

        List<Integer> userTotalCountList = new ArrayList<>();
        List<Integer> userNewCountList = new ArrayList<>();

        for(LocalDate date : dataList){
            Integer userTotalCount = 0;
            Integer userNewCount = 0;

            for(User user : userList){
                if(user.getCreateTime().isAfter(LocalDateTime.of(date, LocalTime.MIN)) && user.getCreateTime().isBefore(LocalDateTime.of(date, LocalTime.MAX))){
                    userNewCount++;
                }
                if(user.getCreateTime().isBefore(LocalDateTime.of(date, LocalTime.MAX))){
                    userTotalCount++;
                }
            }

            userTotalCountList.add(userTotalCount);
            userNewCountList.add(userNewCount);
        }

        String userTotalCountString = StringUtils.join(userTotalCountList, ",");
        String userNewCountString = StringUtils.join(userNewCountList, ",");

        return Result.success(new UserReportVO(dateJoin, userTotalCountString, userNewCountString));
    }


    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd")  LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){

        List<LocalDate> dataList = new ArrayList<>();

        LocalDate l = begin;
        while(!l.isEqual(end)){
            l = l.plusDays(1);
            dataList.add(l);
        }

        String dateJoin = StringUtils.join(dataList, ",");

        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.between(Orders::getOrderTime, begin, end);
        List<Orders> ordersList = ordersService.list(lambdaQueryWrapper);

        List<Integer> orderTotalCountList = new ArrayList<>();
        List<Integer> orderEffectiveCountList = new ArrayList<>();

        for(LocalDate date : dataList){
            Integer orderTotalCount = 0;
            Integer orderEffectiveCount = 0;

            for(Orders orders : ordersList){
                if(orders.getOrderTime().isAfter(LocalDateTime.of(date, LocalTime.MIN)) && orders.getOrderTime().isBefore(LocalDateTime.of(date, LocalTime.MAX))) {
                    if (Objects.equals(orders.getStatus(), Orders.COMPLETED)) {
                        orderEffectiveCount++;
                    }
                    orderTotalCount++;
                }
            }
            
            orderTotalCountList.add(orderTotalCount);
            orderEffectiveCountList.add(orderEffectiveCount);
        }

        String orderCountList = StringUtils.join(orderTotalCountList, ",");
        String validOrderCountList = StringUtils.join(orderEffectiveCountList, ",");

        Integer orderTotalCount = 0;
        for(Integer i : orderTotalCountList){
            orderTotalCount += i;
        }

        Integer orderEffectiveCount = 0;
        for(Integer i : orderEffectiveCountList){
            orderEffectiveCount += i;
        }

        // 订单完成率
        Double orderCompletionRate = orderEffectiveCount * 1.0 / orderTotalCount;

        OrderReportVO orderReportVO = new OrderReportVO(
                dateJoin,
                orderCountList,
                validOrderCountList,
                orderTotalCount,
                orderEffectiveCount,
                orderCompletionRate);

        return Result.success(orderReportVO);
    }
}
