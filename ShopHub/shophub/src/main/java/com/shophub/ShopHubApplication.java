package com.shophub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableKafka
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.shophub.mapper")
@SpringBootApplication
public class ShopHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopHubApplication.class, args);
    }

}
