package ir.tax.trustedtaxpayers.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@ConditionalOnProperty(name = "rate.limit.enabled", havingValue = "true")
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password
    ) {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        if (password != null && !password.isBlank()) {
            config.useSingleServer().setAddress(address).setPassword(password);
        } else {
            config.useSingleServer().setAddress(address);
        }
        return Redisson.create(config);
    }
}


