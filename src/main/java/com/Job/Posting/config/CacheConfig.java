package com.Job.Posting.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // Custom Jackson serializer with polymorphic type handling
        RedisSerializer<Object> serializer = new CustomJsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "jobs",          defaultConfig.entryTtl(Duration.ofMinutes(5)),
                "feed",          defaultConfig.entryTtl(Duration.ofMinutes(2)),
                "notifications", defaultConfig.entryTtl(Duration.ofMinutes(1)),
                "application",   defaultConfig.entryTtl(Duration.ofMinutes(3))
        );

        try {
            String pong = connectionFactory.getConnection().ping();
            if (!"PONG".equalsIgnoreCase(pong)) {
                throw new IllegalStateException("Unexpected Redis ping response: " + pong);
            }

            // Detailed Redis monitoring
            logRedisInfo(connectionFactory);

            log.info("Redis connection established successfully");
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultConfig)
                    .withInitialCacheConfigurations(cacheConfigs)
                    .build();
        } catch (Exception e) {
            log.warn("Redis unavailable, falling back to in-memory cache: {}", e.getMessage());
            return new ConcurrentMapCacheManager("jobs", "feed", "notifications", "application");
        }
    }

    /**
     * Log detailed Redis server information for monitoring
     */
    private void logRedisInfo(RedisConnectionFactory connectionFactory) {
        try {
            var connection = connectionFactory.getConnection();
            if (connection != null) {
                log.info("========== REDIS MONITORING INFO ==========");

                // Get server info
                Properties serverProps = connection.info("server");
                if (serverProps != null) {
                    log.info("--- SERVER INFO ---");
                    serverProps.forEach((key, value) ->
                            log.info("  {} = {}", key, value)
                    );
                }

                // Get memory info
                Properties memoryProps = connection.info("memory");
                if (memoryProps != null) {
                    log.info("--- MEMORY INFO ---");
                    memoryProps.forEach((key, value) ->
                            log.info("  {} = {}", key, value)
                    );
                }

                // Get stats info
                Properties statsProps = connection.info("stats");
                if (statsProps != null) {
                    log.info("--- STATS INFO ---");
                    statsProps.forEach((key, value) ->
                            log.info("  {} = {}", key, value)
                    );
                }

                // Get keyspace info
                Properties keyspaceProps = connection.info("keyspace");
                if (keyspaceProps != null) {
                    log.info("--- KEYSPACE INFO ---");
                    keyspaceProps.forEach((key, value) ->
                            log.info("  {} = {}", key, value)
                    );
                }

                // Get database statistics
                Long dbSize = connection.dbSize();
                log.info("Total keys in Redis: {}", dbSize);

                log.info("=========================================");
                connection.close();
            }
        } catch (Exception e) {
            log.debug("Could not retrieve Redis info: {}", e.getMessage());
        }
    }

    /**
     * Custom Redis serializer using Jackson with polymorphic type handling.
     * This avoids deprecated serializers while maintaining full control over serialization.
     */
    @Slf4j
    static class CustomJsonRedisSerializer implements RedisSerializer<Object> {

        private final ObjectMapper objectMapper;

        public CustomJsonRedisSerializer() {
            this.objectMapper = createConfiguredMapper();
        }

        private ObjectMapper createConfiguredMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // Enable polymorphic type handling for caching different object types
            BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                    .allowIfBaseType(Object.class)
                    .build();
            mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

            return mapper;
        }

        @Override
        public byte[] serialize(Object source) {
            if (source == null) {
                return new byte[0];
            }
            try {
                byte[] result = objectMapper.writeValueAsBytes(source);
                log.debug("Serialized object of type: {} to {} bytes",
                        source.getClass().getSimpleName(), result.length);
                return result;
            } catch (Exception e) {
                log.error("Failed to serialize object of type: {}", source.getClass().getSimpleName(), e);
                throw new RuntimeException("Redis serialization failed", e);
            }
        }

        @Override
        public Object deserialize(byte[] source) {
            if (source == null || source.length == 0) {
                return null;
            }
            try {
                Object result = objectMapper.readValue(source, Object.class);
                log.debug("Deserialized object of type: {} from {} bytes",
                        result.getClass().getSimpleName(), source.length);
                return result;
            } catch (Exception e) {
                log.error("Failed to deserialize object from {} bytes", source.length, e);
                throw new RuntimeException("Redis deserialization failed", e);
            }
        }
    }
}