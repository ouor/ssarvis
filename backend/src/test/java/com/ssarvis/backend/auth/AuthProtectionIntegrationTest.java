package com.ssarvis.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthProtectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedApiRejectsMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/clones"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authorization header is required."));
    }

    @Test
    void protectedApiRejectsBlankBearerToken() throws Exception {
        mockMvc.perform(get("/api/clones")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authorization header is required."));
    }

    @Test
    void protectedApiRejectsMalformedToken() throws Exception {
        mockMvc.perform(get("/api/clones")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired access token."));
    }

    @Test
    void friendApiRejectsMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/friends"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authorization header is required."));
    }
}
