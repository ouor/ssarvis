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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DefaultAccountBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAccountBootstrapService.class);
    private static final String BOOTSTRAP_MODEL = "bootstrap-default";
    private static final Path WORKING_DIRECTORY = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    private static final List<SeedCloneTemplate> DEFAULT_CLONES = List.of(
            new SeedCloneTemplate(
                    "기본 클론 1",
                    "차분하게 정리해 주는 분석형 클론",
                    """
                    당신은 차분하고 분석적인 대화 상대입니다.
                    핵심을 먼저 요약하고, 근거와 구조를 붙여 설명합니다.
                    모르는 것은 추측하지 말고, 필요한 경우 불확실성을 드러냅니다.
                    한국어로 자연스럽고 간결하게 답변합니다.
                    """,
                    Map.of("seed", "default-account", "persona", "analyst")
            ),
            new SeedCloneTemplate(
                    "기본 클론 2",
                    "아이디어를 넓혀 주는 창의형 클론",
                    """
                    당신은 유연하고 창의적인 대화 상대입니다.
                    사용자의 의도를 파악한 뒤 여러 가능성을 제안하고, 대화의 흐름을 부드럽게 이어갑니다.
                    지나치게 장황해지지 않도록 핵심을 유지하면서도 새로운 관점을 제공합니다.
                    한국어로 친근하고 생동감 있게 답변합니다.
                    """,
                    Map.of("seed", "default-account", "persona", "creative")
            )
    );
    private static final List<String> DEFAULT_VOICE_ALIASES = List.of("기본 음성 1", "기본 음성 2");

    private final AppProperties appProperties;
    private final UserAccountRepository userAccountRepository;
    private final PromptGenerationLogRepository promptGenerationLogRepository;
    private final RegisteredVoiceRepository registeredVoiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final VoiceService voiceService;
    private final ObjectMapper objectMapper;

    public DefaultAccountBootstrapService(
            AppProperties appProperties,
            UserAccountRepository userAccountRepository,
            PromptGenerationLogRepository promptGenerationLogRepository,
            RegisteredVoiceRepository registeredVoiceRepository,
            PasswordEncoder passwordEncoder,
            VoiceService voiceService,
            ObjectMapper objectMapper
    ) {
        this.appProperties = appProperties;
        this.userAccountRepository = userAccountRepository;
        this.promptGenerationLogRepository = promptGenerationLogRepository;
        this.registeredVoiceRepository = registeredVoiceRepository;
        this.passwordEncoder = passwordEncoder;
        this.voiceService = voiceService;
        this.objectMapper = objectMapper;
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

        for (SeedCloneTemplate template : DEFAULT_CLONES) {
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

        for (int index = 0; index < DEFAULT_VOICE_ALIASES.size(); index++) {
            String alias = aliases.get(index);
            if (existingDisplayNames.contains(alias)) {
                continue;
            }

            Path samplePath = resolveSamplePath(samplePaths, index);
            voiceService.registerVoice(user.getId(), loadSampleFile(samplePath, alias), alias);
            log.info("Seeded default voice '{}' for user '{}'.", alias, user.getUsername());
        }
    }

    private List<String> resolveVoiceAliases(List<String> configuredAliases) {
        List<String> aliases = sanitize(configuredAliases);
        if (aliases.isEmpty()) {
            return DEFAULT_VOICE_ALIASES;
        }
        if (aliases.size() == 1) {
            return List.of(aliases.get(0), DEFAULT_VOICE_ALIASES.get(1));
        }
        return List.of(aliases.get(0), aliases.get(1));
    }

    private Path resolveSamplePath(List<String> samplePaths, int index) {
        String configuredPath = samplePaths.get(Math.min(index, samplePaths.size() - 1));
        Path rawPath = Path.of(configuredPath).normalize();
        if (rawPath.isAbsolute()) {
            return rawPath;
        }

        Path workingDirCandidate = WORKING_DIRECTORY.resolve(rawPath).normalize();
        if (Files.exists(workingDirCandidate)) {
            return workingDirCandidate;
        }

        Path backendDirCandidate = WORKING_DIRECTORY.resolve("backend").resolve(rawPath).normalize();
        if (Files.exists(backendDirCandidate)) {
            return backendDirCandidate;
        }

        return backendDirCandidate;
    }

    private MultipartFile loadSampleFile(Path samplePath, String alias) {
        try {
            if (!Files.exists(samplePath)) {
                throw new IllegalStateException("Bootstrap voice sample file not found: " + samplePath);
            }

            String originalFilename = samplePath.getFileName().toString();
            String contentType = MediaTypeFactory.getMediaType(originalFilename)
                    .map(MediaType::toString)
                    .orElseGet(() -> probeContentType(samplePath));

            return new SeedMultipartFile(
                    "sampleFile",
                    originalFilename,
                    contentType,
                    Files.readAllBytes(samplePath)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load bootstrap voice sample file: " + samplePath, exception);
        }
    }

    private String probeContentType(Path samplePath) {
        try {
            String detected = Files.probeContentType(samplePath);
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        } catch (IOException ignored) {
            // Fall back to a common audio type below.
        }
        if (samplePath.getFileName().toString().toLowerCase().endsWith(".wav")) {
            return "audio/wav";
        }
        return "application/octet-stream";
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

    private record SeedCloneTemplate(
            String alias,
            String shortDescription,
            String systemPrompt,
            Map<String, Object> answers
    ) {
    }

    private record SeedMultipartFile(
            String name,
            String originalFilename,
            String contentType,
            byte[] bytes
    ) implements MultipartFile {

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes.clone();
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), bytes);
        }

        @Override
        public void transferTo(Path dest) throws IOException, IllegalStateException {
            Files.write(dest, bytes);
        }
    }
}
