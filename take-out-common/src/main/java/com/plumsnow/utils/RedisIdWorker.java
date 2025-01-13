package com.plumsnow.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    private static final long BEGIN_TIMESTAMP = 1640995200L;
    private static final long COUNT_BIT = 32;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public long getId(String keyPre) {
        //1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long timeStamp = now.toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;

        //2.生成序列号
        //2.1获取当日日期，防止超出自增长上限
        String todayDate = now.format(DateTimeFormatter.ofPattern("yyy:MM:dd"));
        //2.2自增长
        Long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPre + ":" + todayDate);

        //3.拼接返回(左移序列号的位数：32位，然后或拼接，加也行，但是效率没那么高)
        return timeStamp << COUNT_BIT | increment;
    }
}
