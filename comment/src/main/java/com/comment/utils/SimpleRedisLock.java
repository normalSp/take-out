package com.comment.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
    private String name;

    private static final String KEY_PRE = "lock:";
    private static final String ID_PRE = UUID.randomUUID().toString(true) + "-";

    private static final DefaultRedisScript<Long> UNLOCK_LUA_SCRIPT;
    static {
        UNLOCK_LUA_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_LUA_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_LUA_SCRIPT.setResultType(Long.class);
    }


    private final StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        String threadId = ID_PRE + Thread.currentThread().getId();

        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PRE + name, threadId + "", timeoutSec, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(flag);
    }

//    @Override
//    public void unlock() {
//        String threadId = ID_PRE + Thread.currentThread().getId();
//
//        String id = stringRedisTemplate.opsForValue().get(KEY_PRE + name);
//
//        if (threadId.equals(id)) {
//            stringRedisTemplate.delete(KEY_PRE + name);
//        }
//    }

    public void unlock(){
        stringRedisTemplate.execute(
                UNLOCK_LUA_SCRIPT,
                Collections.singletonList(KEY_PRE + name),
                ID_PRE + Thread.currentThread().getId());

    }
}
