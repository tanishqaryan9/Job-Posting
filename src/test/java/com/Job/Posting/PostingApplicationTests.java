package com.Job.Posting;

import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        // Prevent Spring Boot from trying to start Docker Compose during tests
        "spring.docker.compose.enabled=false"
})
class PostingApplicationTests {

    @MockitoBean
    FirebaseMessaging firebaseMessaging;
    @MockitoBean
    StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
        // Verifies that the Spring context starts correctly with all beans wired
    }
}