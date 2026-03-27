package com.ssarvis.backend.voice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.friend.FriendRequestRepository;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VoiceServiceTest {

    @Mock
    private RegisteredVoiceRepository registeredVoiceRepository;

    @Mock
    private AudioStorageService audioStorageService;

    @Mock
    private AuthService authService;

    @Mock
    private VoiceAccessPolicy voiceAccessPolicy;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    private VoiceService voiceService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getDashscope().setApiKey("test-dashscope-key");
        appProperties.getDashscope().setTtsModel("current-model");
        voiceService = new VoiceService(
                mock(HttpClient.class),
                new ObjectMapper(),
                appProperties,
                registeredVoiceRepository,
                audioStorageService,
                authService,
                voiceAccessPolicy,
                friendRequestRepository
        );
    }

    @Test
    void listVoicesReturnsOnlyCurrentModelForCurrentUser() {
        UserAccount user = assignId(new UserAccount("haru", "hashed", "하루"), 1L);
        given(authService.getActiveUserAccount(1L)).willReturn(user);
        given(registeredVoiceRepository.findAllByUserIdOrderByIdDesc(1L)).willReturn(List.of(
                assignId(new RegisteredVoice(user, "voice-new", "current-model", "haruvoice", "하루 보이스", "haru.wav", "audio/wav"), 10L),
                assignId(new RegisteredVoice(user, "voice-old", "older-model", "oldvoice", "예전 보이스", "old.wav", "audio/wav"), 11L)
        ));

        List<VoiceSummaryResponse> voices = voiceService.listVoices(1L);

        assertThat(voices).hasSize(1);
        assertThat(voices.get(0).registeredVoiceId()).isEqualTo(10L);
        assertThat(voices.get(0).voiceId()).isEqualTo("voice-new");
        assertThat(voices.get(0).displayName()).isEqualTo("하루 보이스");
    }

    @Test
    void synthesizeRejectsVoiceRegisteredForOlderModel() {
        UserAccount user = assignId(new UserAccount("haru", "hashed", "하루"), 1L);
        RegisteredVoice voice = assignId(
                new RegisteredVoice(user, "voice-old", "older-model", "oldvoice", "예전 보이스", "old.wav", "audio/wav"),
                12L
        );
        given(voiceAccessPolicy.getUsableVoice(1L, 12L)).willReturn(voice);

        assertThatThrownBy(() -> voiceService.synthesize("테스트 문장", 12L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo("This voice was registered for an older DashScope model. Please register it again.");
                });

        verifyNoInteractions(audioStorageService);
    }

    @Test
    void splitTextForTtsKeepsChunksWithinByteLimit() throws Exception {
        Method splitMethod = VoiceService.class.getDeclaredMethod("splitTextForTts", String.class);
        splitMethod.setAccessible(true);

        String longText = "첫 문장은 조금 길게 작성합니다. "
                + "두 번째 문장도 꽤 길게 이어서 작성합니다. "
                + "세 번째 문장까지 이어져서 전체 길이가 제한을 넘도록 만듭니다. "
                + "마지막 문장도 추가합니다.";

        @SuppressWarnings("unchecked")
        List<String> chunks = (List<String>) splitMethod.invoke(voiceService, longText.repeat(8));

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(600)
        );
        assertThat(String.join(" ", chunks)).contains("첫 문장은 조금 길게 작성합니다.");
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

    private RegisteredVoice assignId(RegisteredVoice registeredVoice, Long id) {
        try {
            java.lang.reflect.Field idField = RegisteredVoice.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(registeredVoice, id);
            return registeredVoice;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign test id.", exception);
        }
    }
}
