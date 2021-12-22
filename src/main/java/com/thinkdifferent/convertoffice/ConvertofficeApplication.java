package com.thinkdifferent.convertoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ConvertofficeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConvertofficeApplication.class, args);
    }

}
