package dev.fnvir.kajz.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import dev.fnvir.kajz.apigateway.util.IpUtils;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {
    
    @Bean
    @Primary
    RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 600, 3); // 200 req per min
    }
    
    @Bean
    KeyResolver ipKeyResolver() {
        return exchange -> {
            var req = exchange.getRequest();
            return Mono.just(IpUtils.getClientIp(req));
        };
    }

}
