package com.ssarvis.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void cleanUsers() {
        userAccountRepository.deleteAll();
    }

    @Test
    void signUpCreatesUserAndReturnsAccessToken() throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "haru",
                                  "password": "secret123",
                                  "displayName": "하루"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.username").value("haru"))
                .andExpect(jsonPath("$.displayName").value("하루"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        UserAccount userAccount = userAccountRepository.findById(response.get("userId").asLong()).orElseThrow();
        assertThat(userAccount.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(userAccount.isDeleted()).isFalse();
    }

    @Test
    void signUpRejectsDuplicateUsername() throws Exception {
        userAccountRepository.save(new UserAccount("haru", "hashed-password", "하루"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "haru",
                                  "password": "secret123",
                                  "displayName": "또다른 하루"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username is already taken."));
    }

    @Test
    void loginReturnsAccessTokenForActiveUser() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "dami",
                                  "password": "pass1234",
                                  "displayName": "다미"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "dami",
                                  "password": "pass1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("dami"))
                .andExpect(jsonPath("$.displayName").value("다미"))
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void meReturnsCurrentUserWhenTokenIsValid() throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "miso",
                                  "password": "pass1234",
                                  "displayName": "미소"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("miso"))
                .andExpect(jsonPath("$.displayName").value("미소"));
    }

    @Test
    void meReturnsUnauthorizedWhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authorization header is required."));
    }

    @Test
    void loginRejectsSoftDeletedUser() throws Exception {
        String signUpResponseBody = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "softy",
                                  "password": "pass1234",
                                  "displayName": "소프티"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long userId = objectMapper.readTree(signUpResponseBody).get("userId").asLong();
        UserAccount userAccount = userAccountRepository.findById(userId).orElseThrow();
        userAccount.softDelete();
        userAccountRepository.save(userAccount);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "softy",
                                  "password": "pass1234"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void logoutReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }
}
