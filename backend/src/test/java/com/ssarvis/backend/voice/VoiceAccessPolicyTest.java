package com.ssarvis.backend.voice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.friend.FriendRequestRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VoiceAccessPolicyTest {

    @Mock
    private RegisteredVoiceRepository registeredVoiceRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    private VoiceAccessPolicy voiceAccessPolicy;

    @BeforeEach
    void setUp() {
        voiceAccessPolicy = new VoiceAccessPolicy(registeredVoiceRepository, friendRequestRepository);
    }

    @Test
    void readableVoiceAllowsOwner() {
        UserAccount owner = assignId(new UserAccount("haru", "hashed", "하루"), 1L);
        RegisteredVoice voice = new RegisteredVoice(owner, "voice-a", "tts-model", "voicea", "하루 음성", "a.wav", "audio/wav");
        given(registeredVoiceRepository.findById(20L)).willReturn(Optional.of(voice));

        RegisteredVoice readable = voiceAccessPolicy.getReadableVoice(1L, 20L);

        assertThat(readable).isSameAs(voice);
    }

    @Test
    void readableVoiceAllowsFriendEvenWhenPrivate() {
        UserAccount owner = assignId(new UserAccount("miso", "hashed", "미소"), 2L);
        RegisteredVoice voice = new RegisteredVoice(owner, "voice-b", "tts-model", "voiceb", "미소 음성", "b.wav", "audio/wav");
        given(registeredVoiceRepository.findById(21L)).willReturn(Optional.of(voice));
        given(friendRequestRepository.existsAcceptedBetweenUsers(1L, 2L)).willReturn(true);

        RegisteredVoice readable = voiceAccessPolicy.getReadableVoice(1L, 21L);

        assertThat(readable).isSameAs(voice);
    }

    @Test
    void readableVoiceRejectsNonFriendPrivateVoice() {
        UserAccount owner = assignId(new UserAccount("miso", "hashed", "미소"), 2L);
        RegisteredVoice voice = new RegisteredVoice(owner, "voice-c", "tts-model", "voicec", "미소 음성", "c.wav", "audio/wav");
        given(registeredVoiceRepository.findById(22L)).willReturn(Optional.of(voice));
        given(friendRequestRepository.existsAcceptedBetweenUsers(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> voiceAccessPolicy.getReadableVoice(1L, 22L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Registered voice not found.");
                });
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
}
