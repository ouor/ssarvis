package com.ssarvis.backend.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.auth.UserAccountRepository;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.voice.RegisteredVoice;
import com.ssarvis.backend.voice.RegisteredVoiceRepository;
import com.ssarvis.backend.voice.VoiceService;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class DefaultAccountBootstrapServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PromptGenerationLogRepository promptGenerationLogRepository;

    @Mock
    private RegisteredVoiceRepository registeredVoiceRepository;

    @Mock
    private VoiceService voiceService;

    private PasswordEncoder passwordEncoder;
    private AppProperties appProperties;
    private DefaultAccountBootstrapService bootstrapService;
    private BootstrapVoiceSampleLoader bootstrapVoiceSampleLoader;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        appProperties = new AppProperties();
        bootstrapVoiceSampleLoader = new BootstrapVoiceSampleLoader();
        bootstrapService = new DefaultAccountBootstrapService(
                appProperties,
                userAccountRepository,
                promptGenerationLogRepository,
                registeredVoiceRepository,
                passwordEncoder,
                voiceService,
                new ObjectMapper(),
                new DefaultAccountSeedCatalog(),
                bootstrapVoiceSampleLoader
        );
    }

    @Test
    void doesNothingWhenBootstrapIsDisabled() {
        bootstrapService.ensureDefaultAccountResources();

        verifyNoInteractions(userAccountRepository, promptGenerationLogRepository, registeredVoiceRepository, voiceService);
    }

    @Test
    void createsDefaultAccountClonesAndVoicesWhenMissing(@TempDir Path tempDir) throws Exception {
        Path sample = tempDir.resolve("seed.wav");
        Files.writeString(sample, "seed-audio");
        AppProperties.DefaultAccount config = appProperties.getBootstrap().getDefaultAccount();
        config.setEnabled(true);
        config.setUsername(" seed-user ");
        config.setPassword("secret123");
        config.setDisplayName(" 시드 계정 ");
        config.setVoiceSamplePaths(List.of(sample.toString()));

        given(userAccountRepository.findByUsernameAndDeletedAtIsNull("seed-user")).willReturn(Optional.empty());
        given(userAccountRepository.existsByUsername("seed-user")).willReturn(false);
        given(userAccountRepository.save(any(UserAccount.class))).willAnswer(invocation -> assignUserId(invocation.getArgument(0), 1L));
        given(promptGenerationLogRepository.findAllByUserIdOrderByIdDesc(1L)).willReturn(List.of());
        given(registeredVoiceRepository.findAllByUserIdOrderByIdDesc(1L)).willReturn(List.of());

        bootstrapService.ensureDefaultAccountResources();

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("seed-user");
        assertThat(userCaptor.getValue().getDisplayName()).isEqualTo("시드 계정");
        assertThat(passwordEncoder.matches("secret123", userCaptor.getValue().getPasswordHash())).isTrue();

        ArgumentCaptor<PromptGenerationLog> cloneCaptor = ArgumentCaptor.forClass(PromptGenerationLog.class);
        verify(promptGenerationLogRepository, org.mockito.Mockito.times(2)).save(cloneCaptor.capture());
        assertThat(cloneCaptor.getAllValues()).extracting(PromptGenerationLog::getAlias)
                .containsExactly("기본 클론 1", "기본 클론 2");

        ArgumentCaptor<MultipartFile> fileCaptor = ArgumentCaptor.forClass(MultipartFile.class);
        verify(voiceService).registerVoice(eq(1L), fileCaptor.capture(), eq("기본 음성 1"));
        verify(voiceService).registerVoice(eq(1L), fileCaptor.capture(), eq("기본 음성 2"));
        assertThat(fileCaptor.getAllValues()).hasSize(2);
        assertThat(fileCaptor.getAllValues()).allSatisfy(file -> {
            assertThat(file.getOriginalFilename()).isEqualTo("seed.wav");
            assertThat(file.getContentType()).startsWith("audio/");
        });
    }

    @Test
    void topsUpOnlyMissingDefaultResourcesForExistingUser(@TempDir Path tempDir) throws Exception {
        Path sampleA = tempDir.resolve("voice-a.wav");
        Path sampleB = tempDir.resolve("voice-b.wav");
        Files.writeString(sampleA, "voice-audio-a");
        Files.writeString(sampleB, "voice-audio-b");
        AppProperties.DefaultAccount config = appProperties.getBootstrap().getDefaultAccount();
        config.setEnabled(true);
        config.setUsername("seed-user");
        config.setPassword("secret123");
        config.setDisplayName("시드 계정");
        config.setVoiceSamplePaths(List.of(sampleA.toString(), sampleB.toString()));

        UserAccount existingUser = assignUserId(new UserAccount("seed-user", passwordEncoder.encode("secret123"), "시드 계정"), 7L);
        given(userAccountRepository.findByUsernameAndDeletedAtIsNull("seed-user")).willReturn(Optional.of(existingUser));
        given(promptGenerationLogRepository.findAllByUserIdOrderByIdDesc(7L)).willReturn(List.of(
                new PromptGenerationLog(existingUser, "bootstrap-default", "{}", "prompt", "기본 클론 1", "첫 번째")
        ));
        given(registeredVoiceRepository.findAllByUserIdOrderByIdDesc(7L)).willReturn(List.of(
                new RegisteredVoice(existingUser, "voice-1", "model", "voice1", "기본 음성 1", "voice-a.wav", "audio/wav")
        ));

        bootstrapService.ensureDefaultAccountResources();

        verify(userAccountRepository, never()).save(any(UserAccount.class));
        verify(promptGenerationLogRepository).save(any(PromptGenerationLog.class));
        verify(voiceService).registerVoice(eq(7L), any(MultipartFile.class), eq("기본 음성 2"));
        verify(voiceService, never()).registerVoice(eq(7L), any(MultipartFile.class), eq("기본 음성 1"));
    }

    @Test
    void skipsBootstrapWhenUsernameAlreadyExistsAsInactiveAccount() {
        AppProperties.DefaultAccount config = appProperties.getBootstrap().getDefaultAccount();
        config.setEnabled(true);
        config.setUsername("seed-user");
        config.setPassword("secret123");
        config.setDisplayName("시드 계정");
        config.setVoiceSamplePaths(List.of("unused.wav"));

        given(userAccountRepository.findByUsernameAndDeletedAtIsNull("seed-user")).willReturn(Optional.empty());
        given(userAccountRepository.existsByUsername("seed-user")).willReturn(true);

        bootstrapService.ensureDefaultAccountResources();

        verify(userAccountRepository, never()).save(any(UserAccount.class));
        verifyNoInteractions(promptGenerationLogRepository, registeredVoiceRepository, voiceService);
    }

    @Test
    void resolveSamplePathSupportsBackendRelativePath() throws Exception {
        Method resolveMethod = BootstrapVoiceSampleLoader.class.getDeclaredMethod("resolveSamplePath", List.class, int.class);
        resolveMethod.setAccessible(true);

        Path resolved = (Path) resolveMethod.invoke(bootstrapVoiceSampleLoader, List.of("dev-assets/voices/voice1.wav"), 0);

        assertThat(resolved.toString().replace('\\', '/')).endsWith("backend/dev-assets/voices/voice1.wav");
    }

    private UserAccount assignUserId(UserAccount userAccount, Long id) {
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
