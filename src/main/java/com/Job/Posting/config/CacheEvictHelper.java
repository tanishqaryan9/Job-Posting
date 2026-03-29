package com.Job.Posting.config;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class CacheEvictHelper {

    @CacheEvict(value = "notifications", key = "#userId")
    public void evictUnreadCount(Long userId)
    {

    }

    @CacheEvict(value = "feed", allEntries = true)
    public void evictFeed()
    {

    }
}
