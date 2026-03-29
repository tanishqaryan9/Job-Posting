package com.Job.Posting;

import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class PostingApplicationTests {

    @MockitoBean
    FirebaseMessaging firebaseMessaging;

    @Test
    void contextLoads() {
        //verifies that the spring context starts correctly with all beans wired
    }
}
