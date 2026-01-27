package ir.tax.trustedtaxpayers.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import org.redisson.api.RedissonClient;
import org.redisson.Redisson;
import org.redisson.command.CommandAsyncExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@ConditionalOnBean(RedissonClient.class)
@ConditionalOnMissingBean(ProxyManager.class)
public class RateLimitConfigBeans {

    @Bean
    public ProxyManager<String> bucket4jProxyManager(RedissonClient redissonClient) {
        // In bucket4j 8.10.0, RedissonBasedProxyManager.builderFor requires CommandAsyncExecutor
        // RedissonClient is an interface, we need to cast to Redisson implementation to get CommandAsyncExecutor
        Redisson redisson = (Redisson) redissonClient;
        CommandAsyncExecutor commandExecutor = redisson.getCommandExecutor();
        return RedissonBasedProxyManager.builderFor(commandExecutor).build();
    }
}


