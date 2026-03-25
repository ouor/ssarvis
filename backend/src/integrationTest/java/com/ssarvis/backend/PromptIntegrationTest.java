package com.ssarvis.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import java.time.Instant;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class PromptIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PromptGenerationLogRepository promptGenerationLogRepository;

    private Instant testStartedAt = Instant.now();

    @AfterEach
    void cleanUpCreatedRows() {
        promptGenerationLogRepository.findAll().stream()
                .filter(log -> log.getCreatedAt() != null && !log.getCreatedAt().isBefore(testStartedAt))
                .map(PromptGenerationLog::getId)
                .forEach(promptGenerationLogRepository::deleteById);
        testStartedAt = Instant.now();
    }

    @Test
    void integrationTestCallsOpenAiAndWritesToMysql() throws Exception {
        long beforeCount = promptGenerationLogRepository.count();

        mockMvc.perform(post("/api/system-prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {
                                      "question": "대화할 때 나는 보통",
                                      "answer": "상대가 걸어오면 잘 받는 편"
                                    },
                                    {
                                      "question": "평소 결정은 어떤 편인가요?",
                                      "answer": "충분히 고민하고 정함"
                                    },
                                    {
                                      "question": "좋아하는 분위기에 가까운 것은?",
                                      "answer": "조용하고 차분한 분위기"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemPrompt").isString());

        long afterCount = promptGenerationLogRepository.count();
        assertThat(afterCount).isEqualTo(beforeCount + 1);

        PromptGenerationLog latestLog = promptGenerationLogRepository.findAll().stream()
                .max(Comparator.comparing(PromptGenerationLog::getId))
                .orElseThrow();

        assertThat(latestLog.getModel()).isNotBlank();
        assertThat(latestLog.getAnswersJson()).contains("대화할 때 나는 보통");
        assertThat(latestLog.getSystemPrompt()).isNotBlank();
    }
}
