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

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

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
            // Use try-with-resources so the connection is always closed, even on exception
            try (var connection = connectionFactory.getConnection()) {
                String pong = connection.ping();
                if (!"PONG".equalsIgnoreCase(pong)) {
                    throw new IllegalStateException("Unexpected Redis ping response: " + pong);
                }
                Long dbSize = connection.dbSize();
                log.info("Redis connection established. Keys in store: {}", dbSize);
            }

            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultConfig)
                    .withInitialCacheConfigurations(cacheConfigs)
                    .build();

        } catch (Exception e) {
            log.warn("Redis unavailable — falling back to in-memory cache: {}", e.getMessage());
            return new ConcurrentMapCacheManager("jobs", "feed", "notifications", "application");
        }
    }

    /**
     * Custom Redis serializer using Jackson with polymorphic type handling.
     * Avoids deprecated GenericJackson2JsonRedisSerializer while keeping full control.
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

            BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                    .allowIfBaseType(Object.class)
                    .build();
            mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

            mapper.addMixIn(org.springframework.data.domain.PageImpl.class, PageImplMixin.class);

            return mapper;
        }

        /**
         * Tells Jackson how to reconstruct a {@code PageImpl} from Redis JSON.
         * {@code PageImpl} has no no-arg constructor, so without this mixin
         * deserialization throws {@code InvalidDefinitionException}.
         */
        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        abstract static class PageImplMixin<T> {
            @com.fasterxml.jackson.annotation.JsonCreator
            PageImplMixin(
                    @com.fasterxml.jackson.annotation.JsonProperty("content")
                    java.util.List<T> content,
                    @com.fasterxml.jackson.annotation.JsonProperty("pageable")
                    org.springframework.data.domain.Pageable pageable,
                    @com.fasterxml.jackson.annotation.JsonProperty("totalElements")
                    long totalElements) {
            }
        }

        @Override
        public byte[] serialize(Object source) {
            if (source == null) return new byte[0];
            try {
                return objectMapper.writeValueAsBytes(source);
            } catch (Exception e) {
                log.error("Failed to serialize object of type: {}", source.getClass().getSimpleName(), e);
                throw new RuntimeException("Redis serialization failed", e);
            }
        }

        @Override
        public Object deserialize(byte[] source) {
            if (source == null || source.length == 0) return null;
            try {
                return objectMapper.readValue(source, Object.class);
            } catch (Exception e) {
                log.error("Failed to deserialize Redis value ({} bytes)", source.length, e);
                throw new RuntimeException("Redis deserialization failed", e);
            }
        }
    }
}