package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类 用于AliOssUtil
 */
@Configuration
@Slf4j
public class OssConfig {
    @Bean
    @ConditionalOnMissingBean // 如果没有这个bean，则创建
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties){
        log.info("开始创建阿里云文件上传工具类对象：{}", aliOssProperties);

        return new AliOssUtil(
                aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId() + aliOssProperties.getAccessKeyId1(),
                aliOssProperties.getAccessKeySecret() + aliOssProperties.getAccessKeySecret1(),
                aliOssProperties.getBucketName());
    }

}
