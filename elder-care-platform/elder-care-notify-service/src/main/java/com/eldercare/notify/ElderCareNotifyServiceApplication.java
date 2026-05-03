package com.eldercare.notify;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 通知服务启动类。
 */
@MapperScan("com.eldercare.notify.mapper")
@SpringBootApplication(scanBasePackages = "com.eldercare")
public class ElderCareNotifyServiceApplication {

    /**
     * Java 程序入口。
     *
     * @param args 命令行参数。
     */
    public static void main(String[] args) {
        // 启动通知服务。
        SpringApplication.run(ElderCareNotifyServiceApplication.class, args);
    }
}
