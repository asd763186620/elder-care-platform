package com.eldercare.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 用户服务启动类。
 */
@MapperScan("com.eldercare.user.mapper")
@EnableFeignClients(basePackages = "com.eldercare.api.client")
@SpringBootApplication(scanBasePackages = "com.eldercare")
public class ElderCareUserServiceApplication {

    /**
     * Java 程序入口。
     *
     * @param args 命令行参数。
     */
    public static void main(String[] args) {
        // 启动用户服务。
        SpringApplication.run(ElderCareUserServiceApplication.class, args);
    }
}
