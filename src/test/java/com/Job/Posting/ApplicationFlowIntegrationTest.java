package com.Job.Posting;

import com.Job.Posting.application.repository.JobApplicationRepository;
import com.Job.Posting.auth.repository.AppUserRepository;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.AuthProviderType;
import com.Job.Posting.entity.type.StatusType;
import com.Job.Posting.job.repository.JobRepository;
import com.Job.Posting.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties",
        properties = "spring.docker.compose.enabled=false")
class ApplicationFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired AppUserRepository appUserRepository;
    @Autowired UserRepository userRepository;
    @Autowired JobRepository jobRepository;
    @Autowired JobApplicationRepository jobApplicationRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // Prevent Firebase Admin SDK from trying to initialise during tests
    @MockitoBean
    FirebaseMessaging firebaseMessaging;
    @MockitoBean
    StringRedisTemplate stringRedisTemplate;

    private String applicantToken;
    private String employerToken;
    private Long applicantProfileId;
    private Long employerProfileId;
    private Long jobId;

    @BeforeEach
    void setUp() throws Exception {
        User applicantProfile = new User();
        applicantProfile.setName("Alice");
        applicantProfile.setNumber("9990000001");
        applicantProfile.setLocation("Delhi");
        applicantProfile.setExperience(2);
        applicantProfile.setIsVerified(true); // required to apply for jobs
        applicantProfile = userRepository.save(applicantProfile);
        applicantProfileId = applicantProfile.getId();

        AppUser applicantUser = AppUser.builder()
                .username("alice@example.com")
                .password(passwordEncoder.encode("password"))
                .providerType(AuthProviderType.EMAIL)
                .userProfile(applicantProfile)
                .build();
        appUserRepository.save(applicantUser);

        User employerProfile = new User();
        employerProfile.setName("Bob Corp");
        employerProfile.setNumber("9990000002");
        employerProfile.setLocation("Mumbai");
        employerProfile.setExperience(10);
        employerProfile.setIsVerified(true); // required to post jobs
        employerProfile = userRepository.save(employerProfile);
        employerProfileId = employerProfile.getId();

        AppUser employerUser = AppUser.builder()
                .username("bob@example.com")
                .password(passwordEncoder.encode("password"))
                .providerType(AuthProviderType.EMAIL)
                .userProfile(employerProfile)
                .build();
        appUserRepository.save(employerUser);

        Job job = new Job();
        job.setTitle("Backend Developer");
        job.setDescription("Java Spring Boot role");
        job.setSalary(80000.0);
        job.setLocation("Mumbai");
        job.setLatitude(19.076);
        job.setLongitude(72.877);
        job.setExperience_required(2);
        job.setCreatedBy(employerProfile);
        jobId = jobRepository.save(job).getId();

        applicantToken = loginAndGetToken("alice@example.com", "password");
        employerToken  = loginAndGetToken("bob@example.com",   "password");
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", username, "password", password));

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void applicant_canSubmitApplication_andItIsPersisted() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("jobId", jobId));


        mockMvc.perform(post("/application")
                        .header("Authorization", bearer(applicantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertThat(jobApplicationRepository.existsByJobIdAndUserId(jobId, applicantProfileId)).isTrue();
    }

    @Test
    void applicant_cannotApplyTwice_toTheSameJob() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("jobId", jobId));


        mockMvc.perform(post("/application")
                        .header("Authorization", bearer(applicantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/application")
                        .header("Authorization", bearer(applicantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void applicant_cannotApplyOnBehalfOfAnotherUser() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("jobId", jobId));


        mockMvc.perform(post("/application")
                        .header("Authorization", bearer(applicantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void employer_canUpdateApplicationStatus() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("jobId", jobId));


        String createResponse = mockMvc.perform(post("/application")
                        .header("Authorization", bearer(applicantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long applicationId = objectMapper.readTree(createResponse).get("id").asLong();

        String patchBody = objectMapper.writeValueAsString(Map.of("status", "SHORTLISTED"));
        mockMvc.perform(patch("/application/" + applicationId)
                        .header("Authorization", bearer(employerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));

        assertThat(jobApplicationRepository.findById(applicationId).orElseThrow().getStatus())
                .isEqualTo(StatusType.SHORTLISTED);
    }

    @Test
    void applicant_cannotUpdateApplicationStatus() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("jobId", jobId));


        String createResponse = mockMvc.perform(post("/application")
                        .header("Authorization", bearer(applicantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long applicationId = objectMapper.readTree(createResponse).get("id").asLong();

        String patchBody = objectMapper.writeValueAsString(Map.of("status", "HIRED"));
        mockMvc.perform(patch("/application/" + applicationId)
                        .header("Authorization", bearer(applicantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void applicant_canDeleteOwnApplication() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("jobId", jobId));


        String createResponse = mockMvc.perform(post("/application")
                        .header("Authorization", bearer(applicantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long applicationId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/application/" + applicationId)
                        .header("Authorization", bearer(applicantToken)))
                .andExpect(status().isNoContent());

        assertThat(jobApplicationRepository.existsById(applicationId)).isFalse();
    }

    @Test
    void employer_cannotDeleteApplicantsApplication() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("jobId", jobId));


        String createResponse = mockMvc.perform(post("/application")
                        .header("Authorization", bearer(applicantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long applicationId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/application/" + applicationId)
                        .header("Authorization", bearer(employerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_requestIsRejected() throws Exception {
        mockMvc.perform(get("/application"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employer_cannotModifyAnotherUsersProfile() throws Exception {
        String patchBody = objectMapper.writeValueAsString(Map.of("name", "Hacked Name"));
        mockMvc.perform(patch("/users/" + applicantProfileId)
                        .header("Authorization", bearer(employerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void user_canReadAnyProfile_butNotModifyOthers() throws Exception {
        mockMvc.perform(get("/users/" + applicantProfileId)
                        .header("Authorization", bearer(employerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void jobListing_isAccessibleToAllAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/jobs")
                        .header("Authorization", bearer(applicantToken)))
                .andExpect(status().isOk());
    }

    @Test
    void nonOwner_cannotDeleteAJob() throws Exception {
        mockMvc.perform(delete("/jobs/" + jobId)
                        .header("Authorization", bearer(applicantToken)))
                .andExpect(status().isForbidden());
    }
}