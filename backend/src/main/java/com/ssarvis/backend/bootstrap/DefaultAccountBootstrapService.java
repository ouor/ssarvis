package com.ssarvis.backend.bootstrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.auth.UserAccountRepository;
import com.ssarvis.backend.config.AppProperties;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.voice.RegisteredVoice;
import com.ssarvis.backend.voice.RegisteredVoiceRepository;
import com.ssarvis.backend.voice.VoiceService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DefaultAccountBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAccountBootstrapService.class);
    private static final String BOOTSTRAP_MODEL = "bootstrap-default";

    private final AppProperties appProperties;
    private final UserAccountRepository userAccountRepository;
    private final PromptGenerationLogRepository promptGenerationLogRepository;
    private final RegisteredVoiceRepository registeredVoiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final VoiceService voiceService;
    private final ObjectMapper objectMapper;
    private final DefaultAccountSeedCatalog seedCatalog;
    private final BootstrapVoiceSampleLoader bootstrapVoiceSampleLoader;

    public DefaultAccountBootstrapService(
            AppProperties appProperties,
            UserAccountRepository userAccountRepository,
            PromptGenerationLogRepository promptGenerationLogRepository,
            RegisteredVoiceRepository registeredVoiceRepository,
            PasswordEncoder passwordEncoder,
            VoiceService voiceService,
            ObjectMapper objectMapper,
            DefaultAccountSeedCatalog seedCatalog,
            BootstrapVoiceSampleLoader bootstrapVoiceSampleLoader
    ) {
        this.appProperties = appProperties;
        this.userAccountRepository = userAccountRepository;
        this.promptGenerationLogRepository = promptGenerationLogRepository;
        this.registeredVoiceRepository = registeredVoiceRepository;
        this.passwordEncoder = passwordEncoder;
        this.voiceService = voiceService;
        this.objectMapper = objectMapper;
        this.seedCatalog = seedCatalog;
        this.bootstrapVoiceSampleLoader = bootstrapVoiceSampleLoader;
    }

    public void ensureDefaultAccountResources() {
        AppProperties.DefaultAccount config = appProperties.getBootstrap().getDefaultAccount();
        if (!config.isEnabled()) {
            return;
        }

        String username = normalize(config.getUsername());
        String password = config.getPassword();
        String displayName = normalize(config.getDisplayName());
        validateRequiredConfig(username, password, displayName);

        UserAccount user = userAccountRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseGet(() -> createDefaultUserIfPossible(username, password, displayName));

        if (user == null) {
            return;
        }

        ensureDefaultClones(user);
        ensureDefaultVoices(user, config);
    }

    private UserAccount createDefaultUserIfPossible(String username, String password, String displayName) {
        if (userAccountRepository.existsByUsername(username)) {
            log.warn("Skipping default account bootstrap because username '{}' already exists as an inactive account.", username);
            return null;
        }

        UserAccount savedUser = userAccountRepository.save(new UserAccount(
                username,
                passwordEncoder.encode(password),
                displayName
        ));
        log.info("Created default bootstrap account '{}'.", username);
        return savedUser;
    }

    private void ensureDefaultClones(UserAccount user) {
        List<String> existingAliases = promptGenerationLogRepository.findAllByUserIdOrderByIdDesc(user.getId()).stream()
                .map(PromptGenerationLog::getAlias)
                .toList();

        for (DefaultAccountSeedCatalog.SeedCloneTemplate template : seedCatalog.defaultClones()) {
            if (existingAliases.contains(template.alias())) {
                continue;
            }

            promptGenerationLogRepository.save(new PromptGenerationLog(
                    user,
                    BOOTSTRAP_MODEL,
                    serializeAnswers(template.answers()),
                    template.systemPrompt(),
                    template.alias(),
                    template.shortDescription()
            ));
            log.info("Seeded default clone '{}' for user '{}'.", template.alias(), user.getUsername());
        }
    }

    private void ensureDefaultVoices(UserAccount user, AppProperties.DefaultAccount config) {
        List<String> samplePaths = sanitize(config.getVoiceSamplePaths());
        if (samplePaths.isEmpty()) {
            throw new IllegalStateException("APP_BOOTSTRAP_DEFAULT_ACCOUNT_VOICE_SAMPLE_PATHS must contain at least one path when bootstrap is enabled.");
        }

        List<String> aliases = resolveVoiceAliases(config.getVoiceAliases());
        List<String> existingDisplayNames = registeredVoiceRepository.findAllByUserIdOrderByIdDesc(user.getId()).stream()
                .map(RegisteredVoice::getDisplayName)
                .toList();

        for (int index = 0; index < seedCatalog.defaultVoiceAliases().size(); index++) {
            String alias = aliases.get(index);
            if (existingDisplayNames.contains(alias)) {
                continue;
            }

            var samplePath = bootstrapVoiceSampleLoader.resolveSamplePath(samplePaths, index);
            voiceService.registerVoice(user.getId(), bootstrapVoiceSampleLoader.loadSampleFile(samplePath), alias);
            log.info("Seeded default voice '{}' for user '{}'.", alias, user.getUsername());
        }
    }

    private List<String> resolveVoiceAliases(List<String> configuredAliases) {
        List<String> aliases = sanitize(configuredAliases);
        if (aliases.isEmpty()) {
            return seedCatalog.defaultVoiceAliases();
        }
        if (aliases.size() == 1) {
            return List.of(aliases.get(0), seedCatalog.defaultVoiceAliases().get(1));
        }
        return List.of(aliases.get(0), aliases.get(1));
    }

    private String serializeAnswers(Map<String, Object> answers) {
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize bootstrap clone answers.", exception);
        }
    }

    private void validateRequiredConfig(String username, String password, String displayName) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalStateException("APP_BOOTSTRAP_DEFAULT_ACCOUNT_USERNAME must be set when bootstrap is enabled.");
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException("APP_BOOTSTRAP_DEFAULT_ACCOUNT_PASSWORD must be set when bootstrap is enabled.");
        }
        if (!StringUtils.hasText(displayName)) {
            throw new IllegalStateException("APP_BOOTSTRAP_DEFAULT_ACCOUNT_DISPLAY_NAME must be set when bootstrap is enabled.");
        }
    }

    private List<String> sanitize(List<String> values) {
        List<String> sanitized = new ArrayList<>();
        if (values == null) {
            return sanitized;
        }
        for (String value : values) {
            String normalized = normalize(value);
            if (StringUtils.hasText(normalized)) {
                sanitized.add(normalized);
            }
        }
        return sanitized;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

}
