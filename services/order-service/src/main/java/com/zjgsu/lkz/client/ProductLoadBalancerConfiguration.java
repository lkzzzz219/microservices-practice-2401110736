package com.zjgsu.lkz.client;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.*;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// Loaded only in LoadBalancer child contexts. No component scanning annotation.
public class ProductLoadBalancerConfiguration {
    @Bean
    @ConditionalOnProperty(name="spring.cloud.nacos.discovery.enabled",matchIfMissing=true)
    ReactorLoadBalancer<ServiceInstance> productBalancer(Environment environment, LoadBalancerClientFactory factory) {
        String name=environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RoundRobinLoadBalancer(factory.getLazyProvider(name,ServiceInstanceListSupplier.class),name);
    }
}
