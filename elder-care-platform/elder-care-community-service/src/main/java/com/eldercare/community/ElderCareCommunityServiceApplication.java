package com.eldercare.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 社区服务启动类。
 */
@MapperScan("com.eldercare.community.mapper")
@EnableFeignClients(basePackages = "com.eldercare.api.client")
@SpringBootApplication(scanBasePackages = "com.eldercare")
public class ElderCareCommunityServiceApplication {

    /**
     * Java 程序入口。
     *
     * @param args 命令行参数。
     */
    public static void main(String[] args) {
        // 启动社区服务。
        SpringApplication.run(ElderCareCommunityServiceApplication.class, args);
    }
}
