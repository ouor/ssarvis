package com.ssarvis.backend.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.friend.FriendRelationshipService;
import com.ssarvis.backend.openai.OpenAiClient;
import com.ssarvis.backend.openai.OpenAiContextAssembler;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptServiceTest {

    @Mock
    private PromptGenerationLogRepository promptGenerationLogRepository;

    @Mock
    private OpenAiContextAssembler openAiContextAssembler;

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private AuthService authService;

    @Mock
    private CloneAccessPolicy cloneAccessPolicy;

    @Mock
    private FriendRelationshipService friendRelationshipService;

    private PromptService promptService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getOpenai().setModel("gpt-5.4");
        promptService = new PromptService(
                new ObjectMapper(),
                appProperties,
                promptGenerationLogRepository,
                openAiContextAssembler,
                openAiClient,
                authService,
                cloneAccessPolicy,
                friendRelationshipService
        );
    }

    @Test
    void generateSystemPromptReusesExistingUserClone() throws Exception {
        UserAccount user = assignId(new UserAccount("haru", "hashed", "하루"), 1L);
        PromptGenerationLog existingClone = assignId(
                new PromptGenerationLog(user, "gpt-4", "[]", "old prompt", "이전 클론", "이전 설명"),
                19L
        );
        existingClone.updateVisibility(true);

        PromptGenerateRequest request = new PromptGenerateRequest(List.of(
                new PromptGenerateRequest.AnswerItem("좋아하는 분위기", "차분하고 배려 있는 대화")
        ));

        given(authService.getActiveUserAccount(1L)).willReturn(user);
        given(openAiContextAssembler.buildSystemPromptGenerationMessages(any())).willReturn(List.of());
        given(openAiContextAssembler.buildAliasGenerationMessages(any())).willReturn(List.of());
        given(openAiContextAssembler.buildShortDescriptionGenerationMessages(any())).willReturn(List.of());
        given(openAiClient.requestChatCompletion(any())).willReturn(
                "새 시스템 프롬프트",
                "새 클론",
                "새 설명"
        );
        given(promptGenerationLogRepository.findTopByUserIdOrderByIdDesc(1L)).willReturn(Optional.of(existingClone));
        given(promptGenerationLogRepository.save(any(PromptGenerationLog.class))).willAnswer(invocation -> invocation.getArgument(0));

        PromptGenerateResult result = promptService.generateSystemPrompt(1L, request);

        ArgumentCaptor<PromptGenerationLog> savedCaptor = ArgumentCaptor.forClass(PromptGenerationLog.class);
        verify(promptGenerationLogRepository).save(savedCaptor.capture());
        PromptGenerationLog savedClone = savedCaptor.getValue();

        assertThat(result.promptGenerationLogId()).isEqualTo(19L);
        assertThat(savedClone.getId()).isEqualTo(19L);
        assertThat(savedClone.getAlias()).isEqualTo("새 클론");
        assertThat(savedClone.getShortDescription()).isEqualTo("새 설명");
        assertThat(savedClone.getSystemPrompt()).isEqualTo("새 시스템 프롬프트");
        assertThat(savedClone.isPublic()).isFalse();
    }

    @Test
    void listClonesMineReturnsOnlyLatestClone() {
        UserAccount user = assignId(new UserAccount("haru", "hashed", "하루"), 1L);
        PromptGenerationLog latestClone = assignId(
                new PromptGenerationLog(user, "gpt-5.4", "[]", "prompt", "최신 클론", "최신 설명"),
                23L
        );

        given(authService.getActiveUserAccount(1L)).willReturn(user);
        given(promptGenerationLogRepository.findTopByUserIdOrderByIdDesc(1L)).willReturn(Optional.of(latestClone));

        List<CloneSummaryResponse> clones = promptService.listClones(1L, com.ssarvis.backend.access.AssetListScope.MINE);

        assertThat(clones).hasSize(1);
        assertThat(clones.get(0).cloneId()).isEqualTo(23L);
        assertThat(clones.get(0).alias()).isEqualTo("최신 클론");
    }

    private UserAccount assignId(UserAccount userAccount, Long id) {
        try {
            java.lang.reflect.Field idField = UserAccount.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(userAccount, id);
            return userAccount;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign test id.", exception);
        }
    }

    private PromptGenerationLog assignId(PromptGenerationLog clone, Long id) {
        try {
            java.lang.reflect.Field idField = PromptGenerationLog.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(clone, id);
            return clone;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign test id.", exception);
        }
    }
}
