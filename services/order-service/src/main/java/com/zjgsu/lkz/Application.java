package com.zjgsu.lkz;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@org.springframework.cloud.openfeign.EnableFeignClients
@org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients(defaultConfiguration=com.zjgsu.lkz.client.ProductLoadBalancerConfiguration.class)
@SpringBootApplication
public class Application {
    public static void main(String[] args) { SpringApplication.run(Application.class, args); }
}
