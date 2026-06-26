package com.example.IncidentPulse.ApplicationCofig;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Caching is keyed to two hot, read-heavy paths:
 *
 *   users:byUsername  - resolved on EVERY authenticated request by JwtFilter.
 *   onCall:current    - computed for every new incident to find the assignee.
 *
 * Real (dev/prod) profiles use Redis with conservative per-cache TTLs so stale
 * data self-heals quickly; the 'test' profile uses a plain in-memory manager so
 * the suite needs no Redis server.
 */
@Configuration
@EnableCaching
public class CachingConfig {

    public static final String USERS_BY_USERNAME = "users:byUsername";
    public static final String ON_CALL_CURRENT = "onCall:current";

    @Bean
    @Profile("!test")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(USERS_BY_USERNAME, base.entryTtl(Duration.ofMinutes(5)));
        perCache.put(ON_CALL_CURRENT, base.entryTtl(Duration.ofSeconds(60)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    @Bean
    @Profile("test")
    public CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager(USERS_BY_USERNAME, ON_CALL_CURRENT);
    }
}
