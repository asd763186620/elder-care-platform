package com.eldercare.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单服务启动类。
 */
@MapperScan("com.eldercare.order.mapper")
@EnableFeignClients(basePackages = "com.eldercare.api.client")
@SpringBootApplication(scanBasePackages = "com.eldercare")
public class ElderCareOrderServiceApplication {

    /**
     * Java 程序入口。
     *
     * @param args 命令行参数。
     */
    public static void main(String[] args) {
        // 启动订单服务。
        SpringApplication.run(ElderCareOrderServiceApplication.class, args);
    }
}
