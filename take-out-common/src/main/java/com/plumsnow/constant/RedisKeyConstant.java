package com.plumsnow.constant;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * redis key 常量类
 */
public class RedisKeyConstant {

    public static final String SHOP_ID = "SHOP_ID:";

    public static Long getShopId(StringRedisTemplate stringRedisTemplate, Long userId) {
        String key = RedisKeyConstant.SHOP_ID + userId;
        String shopIdS = stringRedisTemplate.opsForValue().get(key);

        return Long.parseLong(shopIdS);
    }
}
