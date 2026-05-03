package com.eldercare.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关服务启动类。
 */
@SpringBootApplication(scanBasePackages = "com.eldercare")
public class ElderCareGatewayApplication {

    /**
     * Java 程序入口。
     *
     * @param args 命令行参数。
     */
    public static void main(String[] args) {
        // 启动 Spring Cloud Gateway 应用。
        SpringApplication.run(ElderCareGatewayApplication.class, args);
    }
}
