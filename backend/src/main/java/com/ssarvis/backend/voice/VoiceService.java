package com.ssarvis.backend.voice;

import com.ssarvis.backend.auth.AuthService;
import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.access.AssetListScope;
import com.ssarvis.backend.access.VisibilityUpdateRequest;
import com.ssarvis.backend.config.AppProperties;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Base64;
import java.util.List;
import com.ssarvis.backend.friend.FriendRelationshipService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VoiceService {
    private final AppProperties appProperties;
    private final RegisteredVoiceRepository registeredVoiceRepository;
    private final AudioStorageService audioStorageService;
    private final AuthService authService;
    private final VoiceAccessPolicy voiceAccessPolicy;
    private final FriendRelationshipService friendRelationshipService;
    private final DashscopeVoiceClient dashscopeVoiceClient;

    public VoiceService(
            AppProperties appProperties,
            RegisteredVoiceRepository registeredVoiceRepository,
            AudioStorageService audioStorageService,
            AuthService authService,
            VoiceAccessPolicy voiceAccessPolicy,
            FriendRelationshipService friendRelationshipService,
            DashscopeVoiceClient dashscopeVoiceClient
    ) {
        this.appProperties = appProperties;
        this.registeredVoiceRepository = registeredVoiceRepository;
        this.audioStorageService = audioStorageService;
        this.authService = authService;
        this.voiceAccessPolicy = voiceAccessPolicy;
        this.friendRelationshipService = friendRelationshipService;
        this.dashscopeVoiceClient = dashscopeVoiceClient;
    }

    public RegisteredVoice registerVoice(Long userId, MultipartFile sampleFile, String alias) {
        AppProperties.Dashscope dashscope = appProperties.getDashscope();
        UserAccount user = authService.getActiveUserAccount(userId);
        if (!StringUtils.hasText(dashscope.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DASHSCOPE_API_KEY is not configured.");
        }
        if (sampleFile == null || sampleFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voice sample file is required.");
        }
        if (!StringUtils.hasText(sampleFile.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voice sample content type is required.");
        }

        try {
            String displayName = buildDisplayName(alias, sampleFile.getOriginalFilename());
            String preferredName = buildPreferredName(displayName, sampleFile.getOriginalFilename());
            String providerVoiceId = dashscopeVoiceClient.registerVoice(dashscope, sampleFile, preferredName);

            RegisteredVoice voice = registeredVoiceRepository.findTopByUserIdOrderByIdDesc(userId)
                    .map(existingVoice -> {
                        existingVoice.updateRegistration(
                                providerVoiceId,
                                dashscope.getTtsModel(),
                                preferredName,
                                displayName,
                                sampleFile.getOriginalFilename() != null ? sampleFile.getOriginalFilename() : "voice-sample",
                                sampleFile.getContentType()
                        );
                        return existingVoice;
                    })
                    .orElseGet(() -> new RegisteredVoice(
                            user,
                            providerVoiceId,
                            dashscope.getTtsModel(),
                            preferredName,
                            displayName,
                            sampleFile.getOriginalFilename() != null ? sampleFile.getOriginalFilename() : "voice-sample",
                            sampleFile.getContentType()
                    ));

            return registeredVoiceRepository.save(voice);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read the uploaded voice sample.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DashScope voice registration was interrupted.", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<VoiceSummaryResponse> listVoices(Long userId) {
        authService.getActiveUserAccount(userId);
        String currentTtsModel = appProperties.getDashscope().getTtsModel();
        return registeredVoiceRepository.findAllByUserIdOrderByIdDesc(userId).stream()
                .filter(voice -> currentTtsModel.equals(voice.getTargetModel()))
                .limit(1)
                .map(voice -> new VoiceSummaryResponse(
                        voice.getId(),
                        voice.getProviderVoiceId(),
                        voice.getDisplayName(),
                        voice.getPreferredName(),
                        voice.getOriginalFilename(),
                        voice.getAudioMimeType(),
                        voice.getCreatedAt(),
                        voice.isPublic(),
                        voice.getUser() != null ? voice.getUser().getDisplayName() : null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VoiceSummaryResponse> listVoices(Long userId, AssetListScope scope) {
        authService.getActiveUserAccount(userId);
        String currentTtsModel = appProperties.getDashscope().getTtsModel();
        List<RegisteredVoice> voices = switch (scope) {
            case MINE -> registeredVoiceRepository.findTopByUserIdOrderByIdDesc(userId)
                    .map(List::of)
                    .orElseGet(List::of);
            case FRIEND -> listFriendVoices(userId);
            case PUBLIC -> registeredVoiceRepository.findAllByIsPublicTrueOrderByIdDesc();
        };
        return voices.stream()
                .filter(voice -> currentTtsModel.equals(voice.getTargetModel()))
                .map(voice -> new VoiceSummaryResponse(
                        voice.getId(),
                        voice.getProviderVoiceId(),
                        voice.getDisplayName(),
                        voice.getPreferredName(),
                        voice.getOriginalFilename(),
                        voice.getAudioMimeType(),
                        voice.getCreatedAt(),
                        voice.isPublic(),
                        voice.getUser() != null ? voice.getUser().getDisplayName() : null
                ))
                .toList();
    }

    private List<RegisteredVoice> listFriendVoices(Long userId) {
        List<Long> friendIds = friendRelationshipService.findAcceptedFriendIds(userId);
        if (friendIds.isEmpty()) {
            return List.of();
        }
        return registeredVoiceRepository.findAllByUserIdInAndIsPublicFalseOrderByIdDesc(friendIds);
    }

    public VoiceVisibilityResponse updateVoiceVisibility(Long userId, Long registeredVoiceId, VisibilityUpdateRequest request) {
        if (request == null || request.isPublic() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isPublic is required.");
        }

        RegisteredVoice voice = voiceAccessPolicy.getManageableVoice(userId, registeredVoiceId);
        voice.updateVisibility(request.isPublic());
        RegisteredVoice saved = registeredVoiceRepository.save(voice);
        return new VoiceVisibilityResponse(saved.getId(), saved.isPublic());
    }

    public VoiceSynthesisResult synthesize(String text, Long registeredVoiceId, Long userId) {
        if (!StringUtils.hasText(text) || registeredVoiceId == null) {
            return null;
        }

        AppProperties.Dashscope dashscope = appProperties.getDashscope();
        if (!StringUtils.hasText(dashscope.getApiKey())) {
            return null;
        }

        RegisteredVoice voice = voiceAccessPolicy.getUsableVoice(userId, registeredVoiceId);
        validateVoiceCompatibility(voice);

        try {
            DashscopeVoiceClient.DashscopeSynthesisResult synthesisResult = dashscopeVoiceClient.synthesize(
                    text,
                    voice.getProviderVoiceId(),
                    dashscope,
                    null
            );
            if (synthesisResult == null) {
                return null;
            }
            GeneratedAudioAsset audioAsset = audioStorageService.storeDashscopeAudio(
                    voice.getUser(),
                    synthesisResult.audioBytes(),
                    synthesisResult.audioMimeType(),
                    voice.getProviderVoiceId()
            );
            return new VoiceSynthesisResult(
                    voice.getProviderVoiceId(),
                    synthesisResult.audioMimeType(),
                    Base64.getEncoder().encodeToString(synthesisResult.audioBytes()),
                    audioAsset
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to request DashScope TTS.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DashScope TTS request was interrupted.", exception);
        }
    }

    public VoiceSynthesisResult streamSynthesize(String text, Long registeredVoiceId, Long userId, VoiceChunkListener chunkListener) {
        if (!StringUtils.hasText(text) || registeredVoiceId == null) {
            return null;
        }

        AppProperties.Dashscope dashscope = appProperties.getDashscope();
        if (!StringUtils.hasText(dashscope.getApiKey())) {
            return null;
        }

        RegisteredVoice voice = voiceAccessPolicy.getUsableVoice(userId, registeredVoiceId);
        validateVoiceCompatibility(voice);

        try {
            DashscopeVoiceClient.DashscopeSynthesisResult synthesisResult = dashscopeVoiceClient.synthesize(
                    text,
                    voice.getProviderVoiceId(),
                    dashscope,
                    chunkListener
            );
            if (synthesisResult == null) {
                return null;
            }
            GeneratedAudioAsset audioAsset = audioStorageService.storeDashscopeAudio(
                    voice.getUser(),
                    synthesisResult.audioBytes(),
                    synthesisResult.audioMimeType(),
                    voice.getProviderVoiceId()
            );
            return new VoiceSynthesisResult(
                    voice.getProviderVoiceId(),
                    synthesisResult.audioMimeType(),
                    Base64.getEncoder().encodeToString(synthesisResult.audioBytes()),
                    audioAsset
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process streamed TTS audio.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "DashScope TTS stream was interrupted.", exception);
        }
    }

    private String buildDisplayName(String alias, String originalFilename) {
        if (StringUtils.hasText(alias)) {
            return alias.trim();
        }

        String baseName = StringUtils.hasText(originalFilename) ? originalFilename : "voice";
        int extensionIndex = baseName.lastIndexOf('.');
        if (extensionIndex > 0) {
            baseName = baseName.substring(0, extensionIndex);
        }
        return baseName;
    }

    private String buildPreferredName(String alias, String originalFilename) {
        String baseName = StringUtils.hasText(alias) ? alias : originalFilename;
        baseName = StringUtils.hasText(baseName) ? baseName : "voice";
        int extensionIndex = baseName.lastIndexOf('.');
        if (extensionIndex > 0) {
            baseName = baseName.substring(0, extensionIndex);
        }

        String normalized = Normalizer.normalize(baseName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z]", "")
                .toLowerCase();

        if (!StringUtils.hasText(normalized)) {
            normalized = "voice";
        }

        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private void validateVoiceCompatibility(RegisteredVoice voice) {
        String currentTtsModel = appProperties.getDashscope().getTtsModel();
        if (!currentTtsModel.equals(voice.getTargetModel())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This voice was registered for an older DashScope model. Please register it again."
            );
        }
    }
}
