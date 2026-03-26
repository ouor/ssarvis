package com.ssarvis.backend.ownership;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssarvis.backend.auth.UserAccount;
import com.ssarvis.backend.auth.UserAccountRepository;
import com.ssarvis.backend.chat.ChatConversation;
import com.ssarvis.backend.chat.ChatConversationRepository;
import com.ssarvis.backend.debate.DebateSession;
import com.ssarvis.backend.debate.DebateSessionRepository;
import com.ssarvis.backend.prompt.PromptGenerationLog;
import com.ssarvis.backend.prompt.PromptGenerationLogRepository;
import com.ssarvis.backend.voice.GeneratedAudioAsset;
import com.ssarvis.backend.voice.GeneratedAudioAssetRepository;
import com.ssarvis.backend.voice.RegisteredVoice;
import com.ssarvis.backend.voice.RegisteredVoiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class OwnershipMappingDataJpaTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PromptGenerationLogRepository promptGenerationLogRepository;

    @Autowired
    private RegisteredVoiceRepository registeredVoiceRepository;

    @Autowired
    private ChatConversationRepository chatConversationRepository;

    @Autowired
    private DebateSessionRepository debateSessionRepository;

    @Autowired
    private GeneratedAudioAssetRepository generatedAudioAssetRepository;

    @Test
    void ownedEntitiesCanBePersistedAndQueriedByUser() {
        UserAccount owner = userAccountRepository.save(new UserAccount("haru", "hashed", "하루"));
        UserAccount otherUser = userAccountRepository.save(new UserAccount("miso", "hashed", "미소"));

        PromptGenerationLog ownerClone = promptGenerationLogRepository.save(new PromptGenerationLog(
                owner,
                "gpt-5",
                "[]",
                "owner-system-prompt",
                "하루 클론",
                "하루 설명"
        ));
        PromptGenerationLog otherClone = promptGenerationLogRepository.save(new PromptGenerationLog(
                otherUser,
                "gpt-5",
                "[]",
                "other-system-prompt",
                "미소 클론",
                "미소 설명"
        ));

        RegisteredVoice ownerVoice = registeredVoiceRepository.save(new RegisteredVoice(
                owner,
                "voice-owner",
                "qwen-model",
                "ownerpref",
                "하루 목소리",
                "owner.wav",
                "audio/wav"
        ));
        RegisteredVoice otherVoice = registeredVoiceRepository.save(new RegisteredVoice(
                otherUser,
                "voice-other",
                "qwen-model",
                "otherpref",
                "미소 목소리",
                "other.wav",
                "audio/wav"
        ));

        ChatConversation ownerConversation = chatConversationRepository.save(new ChatConversation(owner, ownerClone));
        ChatConversation otherConversation = chatConversationRepository.save(new ChatConversation(otherUser, otherClone));

        DebateSession ownerDebate = debateSessionRepository.save(new DebateSession(
                owner,
                ownerClone,
                ownerClone,
                ownerVoice,
                ownerVoice,
                "재택근무가 더 효율적인가?"
        ));
        DebateSession otherDebate = debateSessionRepository.save(new DebateSession(
                otherUser,
                otherClone,
                otherClone,
                otherVoice,
                otherVoice,
                "대면근무가 더 효율적인가?"
        ));

        GeneratedAudioAsset ownerAsset = generatedAudioAssetRepository.save(new GeneratedAudioAsset(
                owner,
                "S3",
                "bucket",
                "owner.mp3",
                "https://example.com/owner.mp3",
                "audio/wav",
                "audio/mpeg",
                100,
                50,
                "voice-owner"
        ));
        generatedAudioAssetRepository.save(new GeneratedAudioAsset(
                otherUser,
                "S3",
                "bucket",
                "other.mp3",
                "https://example.com/other.mp3",
                "audio/wav",
                "audio/mpeg",
                120,
                60,
                "voice-other"
        ));

        assertThat(promptGenerationLogRepository.findAllByUserIdOrderByIdDesc(owner.getId()))
                .extracting(PromptGenerationLog::getId)
                .containsExactly(ownerClone.getId());
        assertThat(promptGenerationLogRepository.findByIdAndUserId(ownerClone.getId(), owner.getId())).contains(ownerClone);
        assertThat(promptGenerationLogRepository.findByIdAndUserId(otherClone.getId(), owner.getId())).isEmpty();

        assertThat(registeredVoiceRepository.findAllByUserIdOrderByIdDesc(owner.getId()))
                .extracting(RegisteredVoice::getId)
                .containsExactly(ownerVoice.getId());
        assertThat(registeredVoiceRepository.findByIdAndUserId(ownerVoice.getId(), owner.getId())).contains(ownerVoice);
        assertThat(registeredVoiceRepository.findByIdAndUserId(otherVoice.getId(), owner.getId())).isEmpty();

        assertThat(chatConversationRepository.findByIdAndUserId(ownerConversation.getId(), owner.getId())).contains(ownerConversation);
        assertThat(chatConversationRepository.findByIdAndUserId(otherConversation.getId(), owner.getId())).isEmpty();

        assertThat(debateSessionRepository.findByIdAndUserId(ownerDebate.getId(), owner.getId())).contains(ownerDebate);
        assertThat(debateSessionRepository.findByIdAndUserId(otherDebate.getId(), owner.getId())).isEmpty();

        assertThat(generatedAudioAssetRepository.findAllByUserIdOrderByIdDesc(owner.getId()))
                .extracting(GeneratedAudioAsset::getId)
                .containsExactly(ownerAsset.getId());
    }
}
