package com.Job.Posting.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@RestController
@RequestMapping("/api/monitoring/cache")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CacheMonitorController {

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private CacheManager cacheManager;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Cache monitoring service is running");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Check Redis connection
            if (redisConnectionFactory != null) {
                var connection = redisConnectionFactory.getConnection();
                if (connection != null) {
                    String ping = connection.ping();
                    response.put("redis_status", ping);
                    response.put("redis_available", "PONG".equalsIgnoreCase(ping));

                    // Get database size
                    Long dbSize = connection.dbSize();
                    response.put("total_keys", dbSize);

                    // Get memory info
                    Properties memoryProps = connection.info("memory");
                    if (memoryProps != null) {
                        Map<String, Object> memoryInfo = new HashMap<>();
                        memoryProps.forEach((key, value) ->
                                memoryInfo.put(key.toString(), value)
                        );
                        response.put("memory_info", memoryInfo);
                    }

                    // Get stats info
                    Properties statsProps = connection.info("stats");
                    if (statsProps != null) {
                        Map<String, Object> statsInfo = new HashMap<>();
                        statsProps.forEach((key, value) ->
                                statsInfo.put(key.toString(), value)
                        );
                        response.put("stats_info", statsInfo);
                    }

                    connection.close();
                }
            } else {
                response.put("redis_available", false);
                response.put("cache_type", "IN_MEMORY");
            }

            // List all cache names
            response.put("cache_names", cacheManager.getCacheNames());
            response.put("cache_type", cacheManager.getClass().getSimpleName());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking Redis status", e);
            response.put("redis_available", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getCacheInfo() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("cache_manager", cacheManager.getClass().getSimpleName());
            response.put("available_caches", cacheManager.getCacheNames());

            if (redisConnectionFactory != null) {
                var connection = redisConnectionFactory.getConnection();
                if (connection != null) {
                    // Get server info
                    Properties serverProps = connection.info("server");
                    if (serverProps != null) {
                        Map<String, Object> serverInfo = new HashMap<>();
                        serverProps.forEach((key, value) ->
                                serverInfo.put(key.toString(), value)
                        );
                        response.put("server_info", serverInfo);
                    }

                    // Get keyspace info
                    Properties keyspaceProps = connection.info("keyspace");
                    if (keyspaceProps != null) {
                        Map<String, Object> keyspaceInfo = new HashMap<>();
                        keyspaceProps.forEach((key, value) ->
                                keyspaceInfo.put(key.toString(), value)
                        );
                        response.put("keyspace_info", keyspaceInfo);
                    }

                    connection.close();
                }
            }

            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching Redis info", e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}