package com.ssarvis.backend.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.config.AppProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PromptService {

    private static final String DEVELOPER_PROMPT = """
            You generate a single Korean system prompt for another assistant.
            Use the user's questionnaire answers to infer tone, interaction style, boundaries, preferences, and likely communication needs.
            Write only the final system prompt in Korean.
            Keep it practical, specific, and ready to paste into an app.
            Do not mention the survey, MBTI, or explain your reasoning.
            Use short paragraphs or short bullet points only when useful.
            """;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final PromptGenerationLogRepository promptGenerationLogRepository;

    public PromptService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            PromptGenerationLogRepository promptGenerationLogRepository
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.promptGenerationLogRepository = promptGenerationLogRepository;
    }

    public String generateSystemPrompt(PromptGenerateRequest request) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one answer is required.");
        }

        String apiKey = appProperties.getOpenai().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "OPENAI_API_KEY is not configured."
            );
        }

        try {
            String payload = objectMapper.writeValueAsString(buildPayload(request.answers()));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(appProperties.getOpenai().getBaseUrl() + "/responses"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI request failed with status " + response.statusCode() + "."
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode outputText = root.get("output_text");
            if (outputText == null || outputText.asText().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI response did not include output_text."
                );
            }

            String systemPrompt = outputText.asText().trim();
            saveGenerationLog(request.answers(), systemPrompt);
            return systemPrompt;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize OpenAI request.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI request was interrupted.", exception);
        }
    }

    private void saveGenerationLog(List<PromptGenerateRequest.AnswerItem> answers, String systemPrompt) throws IOException {
        String answersJson = objectMapper.writeValueAsString(answers);
        PromptGenerationLog log = new PromptGenerationLog(
                appProperties.getOpenai().getModel(),
                answersJson,
                systemPrompt
        );
        promptGenerationLogRepository.save(log);
    }

    private OpenAiRequest buildPayload(List<PromptGenerateRequest.AnswerItem> answers) {
        StringBuilder prompt = new StringBuilder("""
                아래 설문 응답을 바탕으로, 사용자를 더 잘 보조하기 위한 시스템 프롬프트를 작성해 주세요.
                응답 요약:

                """);

        for (PromptGenerateRequest.AnswerItem answer : answers) {
            if (answer == null || !StringUtils.hasText(answer.question()) || !StringUtils.hasText(answer.answer())) {
                continue;
            }
            prompt.append("- ").append(answer.question().trim()).append(": ").append(answer.answer().trim()).append('\n');
        }

        prompt.append("""

                요구사항:
                - 한국어로 작성
                - 친절하지만 과하게 가볍지 않은 톤
                - 사용자의 의사결정 방식, 대화 스타일, 선호하는 설명 방식이 드러나게 작성
                - 다른 LLM이 그대로 시스템 프롬프트로 사용할 수 있어야 함
                - 불필요한 서론, 제목, 따옴표 없이 본문만 출력
                """);

        return new OpenAiRequest(
                appProperties.getOpenai().getModel(),
                new Reasoning("low"),
                new Text("medium"),
                List.of(
                        new InputMessage("developer", DEVELOPER_PROMPT),
                        new InputMessage("user", prompt.toString())
                )
        );
    }

    private record OpenAiRequest(
            String model,
            Reasoning reasoning,
            Text text,
            List<InputMessage> input
    ) {
    }

    private record Reasoning(String effort) {
    }

    private record Text(String verbosity) {
    }

    private record InputMessage(String role, String content) {
    }
}
