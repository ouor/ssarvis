package com.ssarvis.backend.prompt;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssarvis.backend.api.GlobalExceptionHandler;
import com.ssarvis.backend.auth.AuthenticatedUser;
import com.ssarvis.backend.auth.JwtAuthenticationInterceptor;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CloneController.class)
@Import(GlobalExceptionHandler.class)
class CloneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromptGenerationLogRepository promptGenerationLogRepository;

    @Test
    void listClonesReturnsOnlyCurrentUsersClones() throws Exception {
        PromptGenerationLog first = new PromptGenerationLog("gpt-5", "[]", "system-1", "하루", "하루 설명");
        PromptGenerationLog second = new PromptGenerationLog("gpt-5", "[]", "system-2", "미소", "미소 설명");
        assignId(first, 10L);
        assignCreatedAt(first, Instant.parse("2026-03-26T01:00:00Z"));
        assignId(second, 11L);
        assignCreatedAt(second, Instant.parse("2026-03-26T02:00:00Z"));

        given(promptGenerationLogRepository.findAllByUserIdOrderByIdDesc(1L)).willReturn(List.of(second, first));

        mockMvc.perform(get("/api/clones")
                        .requestAttr(
                                JwtAuthenticationInterceptor.AUTHENTICATED_USER_ATTRIBUTE,
                                new AuthenticatedUser(1L, "haru", "하루")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cloneId").value(11))
                .andExpect(jsonPath("$[0].alias").value("미소"))
                .andExpect(jsonPath("$[1].cloneId").value(10))
                .andExpect(jsonPath("$[1].alias").value("하루"));
    }

    private void assignId(PromptGenerationLog log, Long id) throws Exception {
        java.lang.reflect.Field field = PromptGenerationLog.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(log, id);
    }

    private void assignCreatedAt(PromptGenerationLog log, Instant createdAt) throws Exception {
        java.lang.reflect.Field field = PromptGenerationLog.class.getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(log, createdAt);
    }
}
