package com.plumsnow.controller.admin;

import com.plumsnow.context.BaseContext;
import com.plumsnow.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    @PutMapping("/{status}")
    @ApiOperation("修改店铺状态")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺状态:{}", status == 1 ? "营业中" : "打样中");

        redisTemplate.opsForValue().set(BaseContext.getCurrentShopId() + "SHOP_STATUS", status);

        return Result.success();
    }

    
    @GetMapping("/status")
    @ApiOperation("获取店铺状态")
    public Result<Integer> getStatus(){
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(BaseContext.getCurrentShopId() + "SHOP_STATUS");
        //如果shopStatus为空，在redis新增
        if(null == shopStatus){
            redisTemplate.opsForValue().set(BaseContext.getCurrentShopId() + "SHOP_STATUS", 1);
            shopStatus = 1;
        }

        log.info("获取店铺状态:{}", shopStatus == 1 ? "营业中" : "打样中");

        return Result.success(shopStatus);
    }
}
