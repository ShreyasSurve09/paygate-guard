package com.paygate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PayGateGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayGateGuardApplication.class, args);
    }
}

