package com.gitee.apigrab.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.gitee.apigrab"})
@MapperScan("com.gitee.apigrab.persistence.mapper")
public class ApigrabApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApigrabApplication.class, args);
    }
}
