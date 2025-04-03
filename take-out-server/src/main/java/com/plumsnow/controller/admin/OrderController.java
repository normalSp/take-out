package com.plumsnow.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plumsnow.constant.MessageConstant;
import com.plumsnow.context.BaseContext;
import com.plumsnow.dto.OrdersCancelDTO;
import com.plumsnow.dto.OrdersPageQueryDTO;
import com.plumsnow.entity.OrderDetail;
import com.plumsnow.entity.Orders;
import com.plumsnow.result.PageResult;
import com.plumsnow.result.Result;
import com.plumsnow.service.OrdersDetailService;
import com.plumsnow.service.OrdersService;
import com.plumsnow.vo.OrderStatisticsVO;
import com.plumsnow.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
@Api(tags = "后台订单管理")
public class OrderController {

    @Autowired
    private OrdersService ordersService;
    @Autowired
    private OrdersDetailService ordersDetailService;

    @GetMapping("/conditionSearch")
    @Transactional
    @ApiOperation(value = "条件分页查询订单")
    public Result<PageResult> page(OrdersPageQueryDTO ordersPageQueryDTO){
        LambdaQueryWrapper<Orders> lambdaQueryWrapper  = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(null != ordersPageQueryDTO.getPhone(),  Orders::getPhone, ordersPageQueryDTO.getPhone());
        lambdaQueryWrapper.like(null != ordersPageQueryDTO.getNumber(),  Orders::getNumber, ordersPageQueryDTO.getNumber());
        lambdaQueryWrapper.eq(null != ordersPageQueryDTO.getStatus(),  Orders::getStatus, ordersPageQueryDTO.getStatus());
        lambdaQueryWrapper.between(null != ordersPageQueryDTO.getBeginTime() && null != ordersPageQueryDTO.getEndTime(),
                Orders::getOrderTime, ordersPageQueryDTO.getBeginTime(), ordersPageQueryDTO.getEndTime());
        lambdaQueryWrapper.eq(Orders::getShopId, BaseContext.getCurrentShopId());

        Page page = new Page(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        ordersService.page(page, lambdaQueryWrapper);

        if(0 == page.getTotal()){
            return Result.error(MessageConstant.ORDER_NOT_FOUND);
        }

        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> ordersList = page.getRecords();

        for(Orders orders : ordersList){
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);

            // 查询订单菜品详情信息（订单中的菜品和数量）
            LambdaQueryWrapper<OrderDetail> lambdaQueryWrapper4OrderDetail = new LambdaQueryWrapper<>();
            lambdaQueryWrapper4OrderDetail.eq(OrderDetail::getOrderId, orders.getId());
            List<OrderDetail> orderDetailList = ordersDetailService.list(lambdaQueryWrapper4OrderDetail);

            // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
            List<String> orderDishList = orderDetailList.stream().map(x -> {
                String orderDish = x.getName() + "*" + x.getNumber() + ";";
                return orderDish;
            }).collect(Collectors.toList());

            // 将该订单对应的所有菜品信息拼接在一起
            String join = String.join("", orderDishList);

            orderVO.setOrderDishes(join);
            orderVO.setOrderDetailList(orderDetailList);

            orderVOList.add(orderVO);
        }

        PageResult pageResult = new PageResult(page.getTotal(), orderVOList);

        return Result.success(pageResult);
    }


    @GetMapping("/statistics")
    @ApiOperation(value = "订单统计")
    public Result<OrderStatisticsVO> orderStatistics(){
        //根据状态，分别查询出待接单、待派送、派送中的订单数量
        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Orders::getStatus, Orders.TO_BE_CONFIRMED);
        lambdaQueryWrapper.eq(Orders::getShopId, BaseContext.getCurrentShopId());
        List<Orders> list1 = ordersService.list(lambdaQueryWrapper);
        Integer toBeConfirmed = list1.size();

        LambdaQueryWrapper<Orders> lambdaQueryWrapper2 = new LambdaQueryWrapper<>();
        lambdaQueryWrapper2.eq(Orders::getStatus, Orders.CONFIRMED);
        lambdaQueryWrapper2.eq(Orders::getShopId, BaseContext.getCurrentShopId());
        List<Orders> list2 = ordersService.list(lambdaQueryWrapper2);
        Integer confirmed = list2.size();

        LambdaQueryWrapper<Orders> lambdaQueryWrapper3 = new LambdaQueryWrapper<>();
        lambdaQueryWrapper3.eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS);
        lambdaQueryWrapper3.eq(Orders::getShopId, BaseContext.getCurrentShopId());
        List<Orders> list3 = ordersService.list(lambdaQueryWrapper3);
        Integer deliveryInProgress = list3.size();

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return Result.success(orderStatisticsVO);
    }

    @GetMapping("/details/{id}")
    @ApiOperation("根据id查询订单")
    public Result<OrderVO> getOrderById(@PathVariable Long id){
        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Orders::getId, id);
        lambdaQueryWrapper.eq(Orders::getShopId, BaseContext.getCurrentShopId());
        Orders orders = ordersService.getOne(lambdaQueryWrapper);

        LambdaQueryWrapper<OrderDetail> lambdaQueryWrapper4OrderDetail = new LambdaQueryWrapper<>();
        lambdaQueryWrapper4OrderDetail.eq(OrderDetail::getOrderId, orders.getId());
        lambdaQueryWrapper4OrderDetail.eq(OrderDetail::getShopId, BaseContext.getCurrentShopId());
        List<OrderDetail> orderDetails = ordersDetailService.list(lambdaQueryWrapper4OrderDetail);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);

        return Result.success(orderVO);
    }


    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result<String> cancelOrder(@RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception {
        LambdaQueryWrapper<Orders> lamdaQueryWrapper = new LambdaQueryWrapper<>();
        lamdaQueryWrapper.eq(Orders::getId, ordersCancelDTO.getId());
        Orders orders = ordersService.getOne(lamdaQueryWrapper);

        //管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders1 = new Orders();
        BeanUtils.copyProperties(orders, orders1);
        orders1.setId(ordersCancelDTO.getId());
        orders1.setStatus(Orders.CANCELLED);
        orders1.setCancelReason(ordersCancelDTO.getCancelReason());
        orders1.setCancelTime(LocalDateTime.now());

        LambdaUpdateWrapper<Orders> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(Orders::getId, orders1.getId());
        ordersService.update(orders1, lambdaUpdateWrapper);

        return Result.success("订单取消成功");
    }


}

