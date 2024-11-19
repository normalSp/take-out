package com.comment;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.comment.mapper")
@SpringBootApplication
@EnableCaching
@EnableAspectJAutoProxy(exposeProxy = true)
@Slf4j
public class CommentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentApplication.class, args);
        log.info("server started");
        log.info("毕设-点评系统？ 启动！");
    }

}
