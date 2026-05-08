package com.example.dati_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.dati_backend.mapper")
public class DatiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatiBackendApplication.class, args);
    }

}
