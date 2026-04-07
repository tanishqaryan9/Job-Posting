package com.Job.Posting.config;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dev")
public class CacheFlushController {

    private final RedisConnectionFactory connectionFactory;

    public CacheFlushController(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @DeleteMapping("/cache/flush")
    @PreAuthorize("hasRole('ADMIN')")
    public String flushCache() {
        connectionFactory.getConnection().serverCommands().flushAll();
        return "Cache flushed!";
    }
}
