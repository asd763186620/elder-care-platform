package com.eldercare.volunteer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 志愿者服务启动类。
 */
@MapperScan("com.eldercare.volunteer.mapper")
@EnableFeignClients(basePackages = "com.eldercare.api.client")
@SpringBootApplication(scanBasePackages = "com.eldercare")
public class ElderCareVolunteerServiceApplication {

    /**
     * Java 程序入口。
     *
     * @param args 命令行参数。
     */
    public static void main(String[] args) {
        // 启动志愿者服务。
        SpringApplication.run(ElderCareVolunteerServiceApplication.class, args);
    }
}
