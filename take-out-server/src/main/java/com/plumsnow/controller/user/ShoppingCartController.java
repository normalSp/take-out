package com.plumsnow.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plumsnow.constant.RedisKeyConstant;
import com.plumsnow.context.BaseContext;
import com.plumsnow.entity.Dish;
import com.plumsnow.entity.Setmeal;
import com.plumsnow.entity.ShoppingCart;
import com.plumsnow.result.Result;
import com.plumsnow.service.DishService;
import com.plumsnow.service.SetmealService;
import com.plumsnow.service.ShoppingCartService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private DishService dishService;
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 添加购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    @Transactional
    public Result<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        log.info("添加到购物车的信息：{}",shoppingCart);

        //1. 设置用户id，指定是哪个用户的购物车数据
        shoppingCart.setUserId(BaseContext.getCurrentId());

        //1.1 设置商家id
        long shopId;
        if(shoppingCart.getShopId() != null){
            shopId = shoppingCart.getShopId();
        }else {
            shopId = RedisKeyConstant.getShopId(stringRedisTemplate, BaseContext.getCurrentId());
            shoppingCart.setShopId(shopId);
        }

        //2. 查询当前菜品或套餐是否在购物车中
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId,shoppingCart.getUserId());
        lambdaQueryWrapper.eq(ShoppingCart::getShopId,shopId);

        //2.1 判断加入购物车的是菜品还是套餐
        if(null != shoppingCart.getDishId()) {
            lambdaQueryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());

        }else if(null != shoppingCart.getSetmealId()){
            lambdaQueryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }

        ShoppingCart shoppingCartOne = shoppingCartService.getOne(lambdaQueryWrapper);

        if(null != shoppingCartOne) {
            //3. 如果在，则数据+1
            Integer number = shoppingCartOne.getNumber();
            shoppingCartOne.setNumber(number + 1);
            shoppingCartService.updateById(shoppingCartOne);
        }else {
            //4. 如果不在，则新增一条数据，数量置为1
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());

            //4.1 判断加入购物车的是菜品还是套餐,设置金额
            if(null != shoppingCart.getDishId()) {
                LambdaQueryWrapper<Dish> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
                lambdaQueryWrapper1.eq(Dish::getId, shoppingCart.getDishId());
                Dish dish = dishService.getOne(lambdaQueryWrapper1);

                BigDecimal price = dish.getPrice();
                Integer number = shoppingCart.getNumber();
                BigDecimal amount = price.multiply(new BigDecimal(number));

                shoppingCart.setAmount(amount);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());

            }else if(null != shoppingCart.getSetmealId()){
                LambdaQueryWrapper<Setmeal> lambdaQueryWrapper1 = new LambdaQueryWrapper<>();
                lambdaQueryWrapper1.eq(Setmeal::getId, shoppingCart.getSetmealId());
                Setmeal setmeal = setmealService.getOne(lambdaQueryWrapper1);

                BigDecimal price = setmeal.getPrice();
                Integer number = shoppingCart.getNumber();
                BigDecimal amount = price.multiply(new BigDecimal(number));

                shoppingCart.setAmount(amount);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());

            }

            shoppingCartService.save(shoppingCart);
            shoppingCartOne =  shoppingCart;
        }

        return Result.success(shoppingCartOne);
    }

    /**
     * 购物车商品减1
     * @param shoppingCart
     * @return
     */
    @PostMapping("/sub")
    public Result<String> sub(@RequestBody ShoppingCart shoppingCart){
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        //设置商家id
        long shopId;
        if(shoppingCart.getShopId() != null){
            shopId = shoppingCart.getShopId();
        }else {
            shopId = RedisKeyConstant.getShopId(stringRedisTemplate, BaseContext.getCurrentId());
            shoppingCart.setShopId(shopId);
        }
        lambdaQueryWrapper.eq(ShoppingCart::getShopId, shopId);

        if(null != shoppingCart.getDishId()){
            lambdaQueryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        }

        else{
            lambdaQueryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }


        ShoppingCart shoppingCartOne = shoppingCartService.getOne(lambdaQueryWrapper);

        if(null != shoppingCartOne) {
            Integer number = shoppingCartOne.getNumber();
            if(number > 1) {
                shoppingCartOne.setNumber(number - 1);
                shoppingCartService.updateById(shoppingCartOne);
                return Result.success("减少成功");
            }else {
                shoppingCartService.removeById(shoppingCartOne.getId());
                return Result.success("删除成功");
            }
        }

        return Result.error("更新失败");
    }

    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    public Result<List<ShoppingCart>> list(){
        log.info("查看购物车...");

        LambdaQueryWrapper<ShoppingCart>  lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        long shopId = RedisKeyConstant.getShopId(stringRedisTemplate, BaseContext.getCurrentId());
        lambdaQueryWrapper.eq(ShoppingCart::getShopId, shopId);
        lambdaQueryWrapper.orderByDesc(ShoppingCart::getCreateTime);

        List<ShoppingCart> shoppingCarts = shoppingCartService.list(lambdaQueryWrapper);

        return Result.success(shoppingCarts);
    }

    /**
     * 根据shopId查看购物车
     * @return
     */
    @GetMapping("/list/{shopId}")
    public Result<List<ShoppingCart>> listByShopId(@PathVariable Long shopId){
        log.info("查看商店id为{}购物车...", shopId);

        LambdaQueryWrapper<ShoppingCart>  lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        lambdaQueryWrapper.eq(ShoppingCart::getShopId, shopId);
        lambdaQueryWrapper.orderByDesc(ShoppingCart::getCreateTime);

        List<ShoppingCart> shoppingCarts = shoppingCartService.list(lambdaQueryWrapper);

        return Result.success(shoppingCarts);
    }


    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    @ApiOperation("清空购物车")
    public Result<String> clean(){
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        long shopId = RedisKeyConstant.getShopId(stringRedisTemplate, BaseContext.getCurrentId());
        lambdaQueryWrapper.eq(ShoppingCart::getShopId, shopId);

        shoppingCartService.remove(lambdaQueryWrapper);

        return Result.success("清空购物车成功");
    }

    /**
     * 根据商店id清空购物车
     * @return
     */
    @DeleteMapping("/clean/{shopId}")
    @ApiOperation("根据商店id清空购物车")
    public Result<String> cleanByShopId(@PathVariable Long shopId){
        LambdaQueryWrapper<ShoppingCart> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        lambdaQueryWrapper.eq(ShoppingCart::getShopId, shopId);

        shoppingCartService.remove(lambdaQueryWrapper);

        return Result.success("清空购物车成功");
    }

}
