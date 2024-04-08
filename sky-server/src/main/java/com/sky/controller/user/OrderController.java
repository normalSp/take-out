package com.sky.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import com.sky.service.OrdersService;
import com.sky.service.ShoppingCartService;
import com.sky.service.UserService;
import com.sky.service.impl.OrderDetailService;
import com.sky.vo.OrderSubmitVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Slf4j
@Api(tags = "C端-订单部分接口")
public class OrderController {

    @Autowired
    private OrdersService ordersService;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private AddressBookService addressBookService;
    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private UserService userService;

    @PostMapping("/submit")
    @ApiOperation("订单提交")
    @Transactional
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("订单提交信息:{}",ordersSubmitDTO);

        //处理业务异常（地址为空）
        LambdaQueryWrapper<AddressBook> lambdaQueryWrapper4AddressBook = new LambdaQueryWrapper<>();
        lambdaQueryWrapper4AddressBook.eq(AddressBook::getId,ordersSubmitDTO.getAddressBookId());
        AddressBook addressBook = addressBookService.getOne(lambdaQueryWrapper4AddressBook);
        if(null == addressBook){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //处理业务异常（购物车数量为空）
        LambdaQueryWrapper<ShoppingCart> lamdaQueryWrapper4ShoppingCart = new LambdaQueryWrapper<>();
        lamdaQueryWrapper4ShoppingCart.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(lamdaQueryWrapper4ShoppingCart);
        if(null == shoppingCarts || shoppingCarts.isEmpty()){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        
        //copyDTO到orders
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);

        //向order插入userName、userId
        orders.setUserId(BaseContext.getCurrentId());
        LambdaQueryWrapper<User> lambdaQueryWrapper4User = new LambdaQueryWrapper<>();
        lambdaQueryWrapper4User.eq(User::getId,orders.getUserId());
        User user = userService.getOne(lambdaQueryWrapper4User);
        orders.setUserName(user.getName());

        //向order插入其他属性
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(user.getPhone());
        orders.setConsignee(addressBook.getConsignee());

        //向订单表插入一条数据
        ordersService.save(orders);

        //向订单详情表插入n条数据
        List<OrderDetail>  orderDetails = new ArrayList<>();
        for(ShoppingCart shoppingCart : shoppingCarts){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailService.saveBatch(orderDetails);

        //清空购物车
        shoppingCartService.remove(lamdaQueryWrapper4ShoppingCart);

        //创建VO对象并返回前端
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .orderAmount(orders.getAmount())
                .orderNumber(String.valueOf(orders.getId()))
                .orderTime(orders.getOrderTime())
                .build();

        return Result.success(orderSubmitVO);
    }



}
