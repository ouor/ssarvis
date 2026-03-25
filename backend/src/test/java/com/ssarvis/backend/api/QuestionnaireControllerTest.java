package com.ssarvis.backend.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssarvis.backend.prompt.PromptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(QuestionnaireController.class)
@Import(GlobalExceptionHandler.class)
class QuestionnaireControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromptService promptService;

    @Test
    void generatePromptReturnsSystemPrompt() throws Exception {
        given(promptService.generateSystemPrompt(any()))
                .willReturn("사용자와 대화할 때는 차분하고 구조적인 설명을 우선하세요.");

        mockMvc.perform(post("/api/system-prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {
                                      "question": "평소 결정은 어떤 편인가요?",
                                      "answer": "충분히 고민하고 정함"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemPrompt").value("사용자와 대화할 때는 차분하고 구조적인 설명을 우선하세요."));
    }

    @Test
    void generatePromptReturnsBadRequestWhenAnswersAreEmpty() throws Exception {
        mockMvc.perform(post("/api/system-prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed."))
                .andExpect(jsonPath("$.details[0]").value("answers: answers must contain at least one item."))
                .andExpect(jsonPath("$.path").value("/api/system-prompt"));
    }

    @Test
    void generatePromptReturnsBadRequestWhenAnswerTextIsBlank() throws Exception {
        mockMvc.perform(post("/api/system-prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {
                                      "question": "평소 말투는 어떤 편인가요?",
                                      "answer": "  "
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("answers[0].answer: answer must not be blank."));
    }

    @Test
    void generatePromptPropagatesServiceErrors() throws Exception {
        given(promptService.generateSystemPrompt(any()))
                .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "OpenAI request failed with status 500."));

        mockMvc.perform(post("/api/system-prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {
                                      "question": "대화할 때 나는 보통",
                                      "answer": "상대가 걸어오면 잘 받는 편"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("OpenAI request failed with status 500."))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.path").value("/api/system-prompt"));
    }
}
