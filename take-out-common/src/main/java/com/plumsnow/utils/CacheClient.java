package com.plumsnow.utils;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.comment.utils.LockUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@Slf4j
public class CacheClient {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    //保存到 redis 并设置 TTL
    public void setWithTTL(String key, Object value, Long time, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, timeUnit);
    }

    //保存到 redis 并设置 逻辑过期时间
    public void setWithExpire(String key, Object value, Long time, TimeUnit timeUnit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    //查询并保存到 redis ，并使用设置空值的办法解决缓存击透问题
    public <R, ID>R queryWithPassThrough(
            String keyPre, ID id, Class<R> type, Function<ID, R> function,
            Long time, TimeUnit timeUnit
    ) {
        String json = stringRedisTemplate.opsForValue().get(keyPre + id);

        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }

        if(json != null){
            return null;
        }

        R r = function.apply(id);

        if (r == null) {
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(keyPre + id, "", 3L, TimeUnit.MINUTES);

            return null;
        }

        this.setWithTTL(keyPre + id, r, time, timeUnit);
        return r;
    }

    //查询并保存到 redis ，并使用设置逻辑过期时间的办法解决缓存击穿问题
    public <R, ID> R queryObjectWithLogical(
            String keyPre, ID id, Class<R> type, Function<ID, R> function, String lockPre,
            Long time, TimeUnit timeUnit
    ) {
        String key = keyPre + id;

        String json = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isBlank(json)) {
            return null;
        }

        //1. 命中，反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        Object data = redisData.getData();
        R r = JSONUtil.toBean((JSONObject) data, type);

        LocalDateTime expireTime = redisData.getExpireTime();

        //2. 判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())) {
            //2.1 未过期，返回
            return r;
        }
        //2.2 过期，需要缓存重建

        //3. 缓存重建
        //3.1 获取互斥锁
        LockUtils lockUtils =  new LockUtils(stringRedisTemplate);
        boolean flag = lockUtils.tryLock(lockPre + id);

        //3.2 判断获取是否成功
        if(flag) {
            //x. DoubleCheck 如果过期直接返回
            String json1 = stringRedisTemplate.opsForValue().get(key);
            RedisData redisData1 = JSONUtil.toBean(json, RedisData.class);
            Object data1 = redisData1.getData();
            R r1 = JSONUtil.toBean((JSONObject) data, type);

            LocalDateTime expireTime1 = redisData1.getExpireTime();

            //x.1 判断是否过期
            if(expireTime1.isAfter(LocalDateTime.now())) {
                //x.2 未过期，返回
                return r1;
            }

            //3.3 成功，开启独立线程，进行缓存重建
            LockUtils.CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //重建缓存
                    R apply = function.apply(id);

                    this.setWithExpire(key, apply, time, timeUnit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    lockUtils.unLock(lockPre + id);
                }
            });
        }
        //3.4 失败，返回过期的商铺信息
        return r;
    }


    //查询并保存到 redis ，并使用互斥锁的办法解决缓存击穿问题
    public <R, ID>R queryObjectMutex(
            String keyPre, ID id, Class<R> type, Function<ID, R> function, String lockPre,
            Long time, TimeUnit timeUnit
    ) {
        String key = keyPre + id;

        String json = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }

        if(json != null){
            return null;
        }

        R r = null;

        LockUtils lockUtils = new LockUtils(stringRedisTemplate);
        try {
            //DoubleCheck
            if(Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))){
                return null;
            }

            //x.实现缓存重建
            //x.1 获取锁
            boolean flag = lockUtils.tryLock(lockPre + id);

            //x.2 判断是否获取成功
            if(!flag){
                //x.3 失败，休眠重试
                Thread.sleep(50);
                return queryObjectMutex(keyPre, id, type, function, lockPre,
                        time, timeUnit);
            }

            //x.4 成功，根据id查询数据库
            r = function.apply(id);

            if (r == null) {
                //将空值写入redis
                this.setWithTTL(key, "", time, timeUnit);

                return null;
            }

            this.setWithTTL(key, r, time, timeUnit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //x.5 释放锁
            lockUtils.unLock(lockPre + id);
        }


        return r;
    }

}
