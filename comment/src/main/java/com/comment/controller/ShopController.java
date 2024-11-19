package com.comment.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comment.dto.Result;
import com.comment.entity.Shop;
import com.comment.service.IShopService;
import com.comment.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author plumsnow
 * @since 2024
 */
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    public IShopService shopService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheClient cacheClient;

    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    //@Cacheable(value = "shop", key = "#id")
    public Result queryShopById(@PathVariable("id") Long id) {

        //Shop shop = cacheClient.queryObjectWithLogical("shop:", id, Shop.class,shopService::getById, "lock:shop", 30L, TimeUnit.MINUTES);

        Shop shop = cacheClient.queryWithPassThrough("shop:", id, Shop.class, shopService::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //Shop shop = queryShopMutex(id);

        //Shop shop = queryShopWithLogical(id);

        if (shop == null) {
            return Result.fail("商铺不存在");
        }

        return Result.ok(shop);
    }

    public Shop queryShopWithLogical(Long id) {
        String shopJson = stringRedisTemplate.opsForValue().get("shop:" + id);

        if (StrUtil.isBlank(shopJson)) {
            return null;
        }


        //1. 命中，反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Object data = redisData.getData();
        Shop shop = JSONUtil.toBean((JSONObject) data, Shop.class);

        LocalDateTime expireTime = redisData.getExpireTime();

        //2. 判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())) {
            //2.1 未过期，返回
            return shop;
        }
        //2.2 过期，需要缓存重建

        //3. 缓存重建
        //3.1 获取互斥锁
        LockUtils lockUtils =  new LockUtils(stringRedisTemplate);
        boolean flag = lockUtils.tryLock(RedisConstants.LOCK_SHOP_KEY + id);

        //3.2 判断获取是否成功
        if(flag) {
            //x. DoubleCheck 如果过期直接返回
            String shopJson1 = stringRedisTemplate.opsForValue().get("shop:" + id);
            RedisData redisData1 = JSONUtil.toBean(shopJson, RedisData.class);
            Object data1 = redisData.getData();
            Shop shop1 = JSONUtil.toBean((JSONObject) data, Shop.class);

            LocalDateTime expireTime1 = redisData.getExpireTime();

            //x.1 判断是否过期
            if(expireTime1.isAfter(LocalDateTime.now())) {
                //x.2 未过期，返回
                return shop1;
            }

            //3.3 成功，开启独立线程，进行缓存重建
            LockUtils.CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //重建缓存
                    this.saveShopToReids(id, RedisConstants.CACHE_SHOP_TTL);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    lockUtils.unLock(RedisConstants.LOCK_SHOP_KEY + id);
                }
            });
        }
        //3.4 失败，返回过期的商铺信息
        return shop;
    }

    public Shop queryShopMutex(Long id) {
        String shopJson = stringRedisTemplate.opsForValue().get("shop:" + id);

        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        if(shopJson != null){
            return null;
        }
        Shop shop = null;

        LockUtils lockUtils = new LockUtils(stringRedisTemplate);
        try {
            //DoubleCheck
            if(Boolean.TRUE.equals(stringRedisTemplate.hasKey("shop:" + id))){
                return null;
            }

            //x.实现缓存重建
            //x.1 获取锁
            boolean flag = lockUtils.tryLock(RedisConstants.LOCK_SHOP_KEY + id);

            //x.2 判断是否获取成功
            if(!flag){
                //x.3 失败，休眠重试
                Thread.sleep(50);
                 return queryShopMutex(id);
            }

            //x.4 成功，根据id查询数据库
            shop = shopService.getById(id);

            if (shop == null) {

                //将空值写入redis
                stringRedisTemplate.opsForValue().set("shop:" + id, "", 3L, TimeUnit.MINUTES);

                return null;
            }

            stringRedisTemplate.opsForValue().set("shop:" + id, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //x.5 释放锁
            lockUtils.unLock(RedisConstants.LOCK_SHOP_KEY + id);
        }


        return shop;
    }

    /**
     * 新增商铺信息
     * @param shop 商铺数据
     * @return 商铺id
     */
    @PostMapping
    @CacheEvict(value = "shop", key = "'shop:' + #shop.id")
    @Transactional
    public Result saveShop(@RequestBody Shop shop) {
        // 写入数据库
        shopService.save(shop);
        // 返回店铺id
        return Result.ok(shop.getId());
    }

    /**
     * 更新商铺信息
     * @param shop 商铺数据
     * @return 无
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        // 写入数据库
        shopService.updateById(shop);
        return Result.ok();
    }

    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId 商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
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
            return Result.ok(page.getRecords());
        }

        //算出分页查询需要的from（从哪开始）和end（从哪结束）
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

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
            return Result.ok(Collections.emptyList());
        }


        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        List<Long> ids = new ArrayList<>(content.size());
        Map<String, Distance> distanceMap = new HashMap<>(content.size());

        //查出的results如果长度小于from，意味着已经查到底了，直接返回就行
        if(content.size() <= from){
            return Result.ok(Collections.emptyList());
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

        return Result.ok(shopList);
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     * @param name 商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }

    public void saveShopToReids(Long id, Long expireSeconds) {
        Shop shop = shopService.getById(id);

        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));

        stringRedisTemplate.opsForValue().set("shop:" + id, JSONUtil.toJsonStr(redisData));
    }

}
