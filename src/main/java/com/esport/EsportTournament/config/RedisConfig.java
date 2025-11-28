package com.esport.EsportTournament.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.Cache;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
public class RedisConfig implements CachingConfigurer {

    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl.enabled:true}")
    private boolean sslEnabled;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() throws URISyntaxException {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        // Parse Redis URL if provided (Railway format: redis://:password@host:port)
        if (redisUrl != null && !redisUrl.isEmpty() && redisUrl.startsWith("redis://")) {
            URI uri = new URI(redisUrl);
            config.setHostName(uri.getHost());
            config.setPort(uri.getPort());
            if (uri.getUserInfo() != null && uri.getUserInfo().contains(":")) {
                String password = uri.getUserInfo().split(":")[1];
                if (password != null && !password.isEmpty()) {
                    config.setPassword(password);
                }
            }
        } else {
            // Use individual properties
            config.setHostName(redisHost);
            config.setPort(redisPort);
            if (redisPassword != null && !redisPassword.isEmpty()) {
                config.setPassword(redisPassword);
            }
        }

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder().build();

        if (sslEnabled) {
            clientConfig = LettuceClientConfiguration.builder()
                    .useSsl()
                    .build();
        }

        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.error("❌ Redis Cache GET Error for key '{}': {}", key, exception.getMessage());
                // Treat as cache miss - fallback to DB
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.error("❌ Redis Cache PUT Error for key '{}': {}", key, exception.getMessage());
                // Continue execution
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.error("❌ Redis Cache EVICT Error for key '{}': {}", key, exception.getMessage());
                // Continue execution
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("❌ Redis Cache CLEAR Error: {}", exception.getMessage());
                // Continue execution
            }
        };
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Keys = Strings
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Values = JSON
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
