package br.com.fiap.vendasms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class VendasMsApplication {

    public static void main(String[] args) {
        SpringApplication.run(VendasMsApplication.class, args);
    }

}
