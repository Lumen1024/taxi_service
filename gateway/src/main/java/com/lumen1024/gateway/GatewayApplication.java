package com.lumen1024.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;

@SpringBootApplication(
    scanBasePackages = "com.lumen1024",
    exclude = SimpleDiscoveryClientAutoConfiguration.class
)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
