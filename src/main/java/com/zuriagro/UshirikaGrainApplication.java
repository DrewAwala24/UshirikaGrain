package com.zuriagro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableJpaAuditing
public class UshirikaGrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(UshirikaGrainApplication.class, args);
    }
}
