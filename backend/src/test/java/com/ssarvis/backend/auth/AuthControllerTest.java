package com.ssarvis.backend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssarvis.backend.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void signUpReturnsAuthResponse() throws Exception {
        given(authService.signUp(any()))
                .willReturn(new AuthSession(1L, "haru", "하루", "access-token"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "haru",
                                  "password": "secret123",
                                  "displayName": "하루"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("haru"))
                .andExpect(jsonPath("$.displayName").value("하루"))
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void loginReturnsAuthResponse() throws Exception {
        given(authService.login(any()))
                .willReturn(new AuthSession(2L, "miso", "미소", "login-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "miso",
                                  "password": "pass1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.username").value("miso"))
                .andExpect(jsonPath("$.displayName").value("미소"))
                .andExpect(jsonPath("$.accessToken").value("login-token"));
    }

    @Test
    void signUpReturnsBadRequestWhenDisplayNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "haru",
                                  "password": "secret123",
                                  "displayName": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed."))
                .andExpect(jsonPath("$.details[0]").value("displayName: displayName must not be blank."));
    }

    @Test
    void meReturnsAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(3L, "dami", "다미")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(3))
                .andExpect(jsonPath("$.username").value("dami"))
                .andExpect(jsonPath("$.displayName").value("다미"));
    }

    @Test
    void logoutReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }
}
