package com.Job.Posting;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PostingApplicationTests {

    @Test
    void contextLoads() {
        //verifies that the spring context starts correctly with all beans wired
    }
}
