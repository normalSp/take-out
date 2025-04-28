package com.plumsnow.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.plumsnow.constant.RedisKeyConstant;
import com.plumsnow.context.BaseContext;
import com.plumsnow.entity.Shop;
import com.plumsnow.result.Result;
import com.plumsnow.service.ShopService;
import com.plumsnow.utils.RedisConstants;
import com.plumsnow.utils.SystemConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "店铺相关接口")
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ShopService shopService;

    @GetMapping("/status")
    @ApiOperation("获取店铺状态")
    public Result<Integer> getStatus(){
        long shopId = RedisKeyConstant.getShopId(stringRedisTemplate, BaseContext.getCurrentId());

        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(shopId + "SHOP_STATUS");
        log.info("获取店铺状态:{}", shopStatus == 1 ? "营业中" : "打样中");

        return Result.success(shopStatus);
    }

    @GetMapping("/status/{shopId}")
    @ApiOperation("根据店铺id获取店铺状态")
    public Result<Integer> getStatusForOne(@PathVariable String shopId){
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(shopId + "SHOP_STATUS");
        if(shopStatus == null){
            log.info("id为{}的店铺状态:打样中", shopId);
            return Result.success(null);
        }

        log.info("id为{}的店铺状态:{}", shopId,shopStatus == 1 ? "营业中" : "打样中");

        return Result.success(shopStatus);
    }


    @GetMapping("/of/type")
    @ApiOperation("分页查询商铺信息")
    public Result queryShopByXY(
            @RequestParam(value = "typeId", defaultValue = "1") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size") Integer size,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y
    ) {
        //如果前端没有传xy，用默认查询
        if(x == null || y == null) {
            // 根据类型分页查询
            Page<Shop> page = shopService.query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.success(page.getRecords());
        }

        //算出分页查询需要的from（从哪开始）和end（从哪结束）
        //int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        //int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        int from = (current - 1) * size;
        int end = current * size;

        //从redis中查出5公里内的商铺
        //GEOSEARCH key BYLONLAT x y BYRADIUS 5 km WITHDISTANCE
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                        RedisConstants.SHOP_GEO_KEY + typeId,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );

        //判断是否为空
        if(results == null){
            return Result.success(Collections.emptyList());
        }


        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        List<Long> ids = new ArrayList<>(content.size());
        Map<String, Distance> distanceMap = new HashMap<>(content.size());

        //查出的results如果长度小于from，意味着已经查到底了，直接返回就行
        if(content.size() <= from){
            return Result.success(Collections.emptyList());
        }

        //如果正常就把result里的东西拿出塞进ids和shopList
        content.stream().skip(from).forEach(result -> {
            String shopId = result.getContent().getName();
            ids.add(Long.valueOf(shopId));

            Distance distance = result.getDistance();
            distanceMap.put(shopId, distance);
        });

        //按距离排好的顺序查询并设置好距离返回
        List<Shop> shopList = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Shop shop = shopService.getById(id);
            shop.setDistance(distanceMap.get(id.toString()).getValue());

            shopList.add(shop);
        }

        // 计算数据库中总商铺数量
        QueryWrapper<Shop> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type_id", typeId);
        Long count = (long) shopService.count(queryWrapper);

        shopList.get(0).setTotal(count);

        return Result.success(shopList);
    }

    @GetMapping("/one/{shopId}")
    @ApiOperation("根据id查询商铺信息")
    public Result<Shop> queryShopByType(@PathVariable String shopId){
        LambdaQueryWrapper<Shop> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Shop::getId, shopId);

        Shop one = shopService.getOne(lambdaQueryWrapper);

        if(one == null){
            return Result.error("没有找到该商店");
        }

        return Result.success(one);
    }
}
