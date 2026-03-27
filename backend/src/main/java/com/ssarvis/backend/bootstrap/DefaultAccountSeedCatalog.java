package com.ssarvis.backend.bootstrap;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DefaultAccountSeedCatalog {

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

    public List<SeedCloneTemplate> defaultClones() {
        return DEFAULT_CLONES;
    }

    public List<String> defaultVoiceAliases() {
        return DEFAULT_VOICE_ALIASES;
    }

    public record SeedCloneTemplate(
            String alias,
            String shortDescription,
            String systemPrompt,
            Map<String, Object> answers
    ) {
    }
}
