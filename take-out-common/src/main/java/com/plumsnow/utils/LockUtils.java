package com.plumsnow.utils;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LockUtils {
        private StringRedisTemplate stringRedisTemplate;

        public static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

        public LockUtils(StringRedisTemplate stringRedisTemplate) {
            this.stringRedisTemplate = stringRedisTemplate;
        }

        public boolean tryLock(String key){
            Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "lock", 100, TimeUnit.SECONDS);
            return BooleanUtil.isTrue(flag);
        }

        public boolean unLock(String key){
            Boolean flag = stringRedisTemplate.delete(key);
            return BooleanUtil.isTrue(flag);
        }
}
