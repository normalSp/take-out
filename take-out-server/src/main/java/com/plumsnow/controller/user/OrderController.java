package com.plumsnow.controller.user;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plumsnow.WebSocket.WebSocketServer;
import com.plumsnow.constant.MessageConstant;
import com.plumsnow.constant.RedisKeyConstant;
import com.plumsnow.context.BaseContext;
import com.plumsnow.dto.OrdersPaymentDTO;
import com.plumsnow.dto.OrdersSubmitDTO;
import com.plumsnow.entity.*;
import com.plumsnow.exception.AddressBookBusinessException;
import com.plumsnow.exception.OrderBusinessException;
import com.plumsnow.exception.ShoppingCartBusinessException;
import com.plumsnow.result.PageResult;
import com.plumsnow.result.Result;
import com.plumsnow.service.AddressBookService;
import com.plumsnow.service.OrdersService;
import com.plumsnow.service.ShoppingCartService;
import com.plumsnow.service.UserService;
import com.plumsnow.service.impl.OrderDetailService;
import com.plumsnow.utils.WeChatPayUtil;
import com.plumsnow.vo.OrderPaymentVO;
import com.plumsnow.vo.OrderSubmitVO;
import com.plumsnow.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName());
        long shopId = RedisKeyConstant.getShopId(stringRedisTemplate, BaseContext.getCurrentId());
        orders.setShopId(shopId);

        //向订单表插入一条数据
        ordersService.save(orders);

        //向订单详情表插入n条数据
        List<OrderDetail>  orderDetails = new ArrayList<>();
        for(ShoppingCart shoppingCart : shoppingCarts){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetail.setId(null);
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

    @PostMapping("/submitByShopId")
    @ApiOperation("订单提交-附加shopId")
    @Transactional
    public Result<OrderSubmitVO> submitByShopId(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
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
        lamdaQueryWrapper4ShoppingCart.eq(ShoppingCart::getShopId,ordersSubmitDTO.getShopId());
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
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName());
        orders.setShopId(ordersSubmitDTO.getShopId());

        //向订单表插入一条数据
        ordersService.save(orders);

        //向订单详情表插入n条数据
        List<OrderDetail>  orderDetails = new ArrayList<>();
        for(ShoppingCart shoppingCart : shoppingCarts){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetail.setId(null);
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



        //跳过支付模块直接完成支付
        LambdaUpdateWrapper<Orders> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(Orders::getId, orders.getId());
        lambdaUpdateWrapper.set(Orders::getStatus, 4); //待派送
        lambdaUpdateWrapper.set(Orders::getPayStatus, 1); //已支付
        ordersService.update(lambdaUpdateWrapper);

        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = ordersService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status   订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("历史订单查询")
    public Result<PageResult> page(int page, int pageSize, Integer status){
        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        lambdaQueryWrapper.eq(null != status,Orders::getStatus,status);
        lambdaQueryWrapper.orderByDesc(Orders::getEstimatedDeliveryTime);

        Page page_ = new Page(page,pageSize);

        ordersService.page(page_,lambdaQueryWrapper);

        if(0 == page_.getTotal()){
            return Result.error(MessageConstant.ORDER_NOT_FOUND);
        }

        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> ordersList = page_.getRecords();

        for(Orders orders : ordersList){
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);

            // 查询订单菜品详情信息（订单中的菜品和数量）
            LambdaQueryWrapper<OrderDetail> lambdaQueryWrapper4OrderDetail = new LambdaQueryWrapper<>();
            lambdaQueryWrapper4OrderDetail.eq(OrderDetail::getOrderId, orders.getId());
            List<OrderDetail> orderDetailList = orderDetailService.list(lambdaQueryWrapper4OrderDetail);

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


        PageResult pageResult = new PageResult(page_.getTotal(), orderVOList);

        return Result.success(pageResult);
    }

    @GetMapping("/orderDetail/{id}")
    @ApiOperation("根据id查询订单")
    public Result<OrderVO> getOrderById(@PathVariable Long id){
        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Orders::getId, id);
        Orders orders = ordersService.getOne(lambdaQueryWrapper);

        LambdaQueryWrapper<OrderDetail> lambdaQueryWrapper4OrderDetail = new LambdaQueryWrapper<>();
        lambdaQueryWrapper4OrderDetail.eq(OrderDetail::getOrderId, orders.getId());
        List<OrderDetail> orderDetails = orderDetailService.list(lambdaQueryWrapper4OrderDetail);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);

        return Result.success(orderVO);
    }

    /**
     * 客户催单
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("客户催单")
    public Result reminder(@PathVariable("id") Long id){
        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Orders::getId,id);
        Orders orders = ordersService.getOne(lambdaQueryWrapper);

        if(null == orders){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map map = new HashMap<>();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("cintent", "订单号：" + orders.getId());

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);

        return Result.success();
    }

    @Transactional
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result<String> cancel(@PathVariable Long id) throws Exception {
        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Orders::getId,id);
        Orders orders = ordersService.getOne(lambdaQueryWrapper);

        // 校验订单是否存在
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (orders.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
        Integer payStatus = orders.getPayStatus();
        if (payStatus == Orders.TO_BE_CONFIRMED) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    orders.getNumber(),
                    orders.getNumber(),
                    new BigDecimal("0.01"),
                    new BigDecimal("0.01"));
            log.info("申请退款：{}", refund);
            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());

        LambdaUpdateWrapper<Orders> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(Orders::getId, id);
        ordersService.update(orders, lambdaUpdateWrapper);

        return Result.success(MessageConstant.ORDER_CANCEL_SUCCESS);
    }
    
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result<String> repetition(@PathVariable String id){
        LambdaQueryWrapper<OrderDetail> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(OrderDetail::getOrderId,id);
        List<OrderDetail> orderDetailList = orderDetailService.list(lambdaQueryWrapper);

        List<ShoppingCart> shoppingCartList = new ArrayList<>();

        for(OrderDetail orderDetail : orderDetailList){
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,shoppingCart);

            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCartList.add(shoppingCart);
        }

        shoppingCartService.saveBatch(shoppingCartList);

        return Result.success(MessageConstant.SAVE_SUCCESS);
    }
}
