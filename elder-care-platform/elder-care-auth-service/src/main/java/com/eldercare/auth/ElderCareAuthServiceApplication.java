package com.eldercare.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证服务启动类。
 */
@MapperScan("com.eldercare.auth.mapper")
@SpringBootApplication(scanBasePackages = "com.eldercare")
public class ElderCareAuthServiceApplication {

    /**
     * Java 程序入口。
     *
     * @param args 命令行参数。
     */
    public static void main(String[] args) {
        // 启动认证服务。
        SpringApplication.run(ElderCareAuthServiceApplication.class, args);
    }
}
