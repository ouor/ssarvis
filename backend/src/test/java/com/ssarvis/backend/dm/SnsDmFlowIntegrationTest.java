package com.ssarvis.backend.dm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.auth.UserAccountRepository;
import com.ssarvis.backend.follow.FollowRepository;
import com.ssarvis.backend.openai.OpenAiClient;
import com.ssarvis.backend.post.PostRepository;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SnsDmFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DmHiddenBundleRepository dmHiddenBundleRepository;

    @Autowired
    private DmMessageRepository dmMessageRepository;

    @Autowired
    private DmThreadRepository dmThreadRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private PromptGenerationLogRepository promptGenerationLogRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @MockBean
    private OpenAiClient openAiClient;

    @BeforeEach
    void cleanDatabase() {
        dmHiddenBundleRepository.deleteAll();
        dmMessageRepository.deleteAll();
        dmThreadRepository.deleteAll();
        postRepository.deleteAll();
        followRepository.deleteAll();
        promptGenerationLogRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void publicProfileAndDmAutoReplyFlowWorksEndToEnd() throws Exception {
        given(openAiClient.requestChatCompletion(any())).willReturn("지금은 자리를 비워서 AI가 대신 답장해요.");

        JsonNode senderSignUp = signUp("haru", "secret123", "하루");
        JsonNode receiverSignUp = signUp("miso", "secret123", "미소");

        String senderToken = senderSignUp.get("accessToken").asText();
        String receiverToken = receiverSignUp.get("accessToken").asText();
        Long receiverUserId = receiverSignUp.get("userId").asLong();

        UserAccount receiver = userAccountRepository.findByIdAndDeletedAtIsNull(receiverUserId).orElseThrow();
        promptGenerationLogRepository.save(new PromptGenerationLog(
                receiver,
                "gpt-5",
                "[]",
                "너는 미소를 대리하는 AI다.",
                "미소 클론",
                "미소의 말투를 흉내 내는 대표 클론"
        ));

        mockMvc.perform(patch("/api/profiles/me/auto-reply")
                        .header(HttpHeaders.AUTHORIZATION, bearer(receiverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mode": "ALWAYS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("ALWAYS"));

        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(receiverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "미소의 공개 게시물"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerUsername").value("miso"))
                .andExpect(jsonPath("$.content").value("미소의 공개 게시물"));

        mockMvc.perform(get("/api/follows/users/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderToken))
                        .param("query", "미소"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("miso"))
                .andExpect(jsonPath("$[0].following").value(false));

        mockMvc.perform(get("/api/profiles/" + receiverUserId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("미소"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));

        mockMvc.perform(get("/api/profiles/" + receiverUserId + "/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("미소의 공개 게시물"));

        String threadBody = mockMvc.perform(post("/api/dms/threads")
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetUserId": %d
                                }
                                """.formatted(receiverUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherParticipant.username").value("miso"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long threadId = objectMapper.readTree(threadBody).get("threadId").asLong();

        String messageBody = mockMvc.perform(post("/api/dms/threads/" + threadId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "안녕, 지금 있어?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiGenerated").value(false))
                .andExpect(jsonPath("$.content").value("안녕, 지금 있어?"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long rootMessageId = objectMapper.readTree(messageBody).get("messageId").asLong();

        mockMvc.perform(get("/api/dms/threads/" + threadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].content").value("안녕, 지금 있어?"))
                .andExpect(jsonPath("$.messages[0].bundleRootMessageId").value(rootMessageId))
                .andExpect(jsonPath("$.messages[1].aiGenerated").value(true))
                .andExpect(jsonPath("$.messages[1].bundleRootMessageId").value(rootMessageId))
                .andExpect(jsonPath("$.messages[1].content").value("지금은 자리를 비워서 AI가 대신 답장해요."));

        mockMvc.perform(post("/api/dms/threads/" + threadId + "/bundles/" + rootMessageId + "/hide")
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bundleRootMessageId").value(rootMessageId))
                .andExpect(jsonPath("$.hidden").value(true));

        mockMvc.perform(get("/api/dms/threads/" + threadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(senderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hiddenBundleMessageIds[0]").value(rootMessageId));
    }

    private JsonNode signUp(String username, String password, String displayName) throws Exception {
        String body = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "displayName": "%s"
                                }
                                """.formatted(username, password, displayName)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
