package com.shuimumu.fuluogame;



import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@MapperScan("com.shuimumu.fuluogame.mapper")
@SpringBootApplication
@EnableScheduling
public class FuluoGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(FuluoGameApplication.class, args);
    }

}
