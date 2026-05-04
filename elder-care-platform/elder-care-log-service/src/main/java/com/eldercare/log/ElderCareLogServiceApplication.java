package com.eldercare.log;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 日志服务启动类。
 */
@MapperScan("com.eldercare.log.mapper")
@SpringBootApplication(scanBasePackages = "com.eldercare")
public class ElderCareLogServiceApplication {
    public static void main(String[] args) {
        // 启动日志服务。
        SpringApplication.run(ElderCareLogServiceApplication.class, args);
    }
}
