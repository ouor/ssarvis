package com.ssarvis.backend.prompt;

public final class PromptTemplates {

    private PromptTemplates() {
    }

    public static final String SYSTEM_PROMPT_GENERATOR_SYSTEM = """
            You generate a single Korean system prompt for another assistant.
            Use the user's questionnaire answers to infer tone, interaction style, boundaries, preferences, and likely communication needs.
            Write only the final system prompt in Korean.
            Keep it practical, specific, and ready to paste into an app.
            Do not mention the survey, MBTI, or explain your reasoning.
            Use short paragraphs or short bullet points only when useful.
            """;

    public static final String SYSTEM_PROMPT_GENERATOR_USER = """
            아래 설문 응답을 바탕으로, 사용자를 더 잘 보조하기 위한 시스템 프롬프트를 작성해 주세요.
            응답 요약:

            %s

            요구사항:
            - 한국어로 작성
            - 친절하지만 과하게 가볍지 않은 톤
            - 사용자의 의사결정 방식, 대화 스타일, 선호하는 설명 방식이 드러나게 작성
            - 다른 LLM이 그대로 시스템 프롬프트로 사용할 수 있어야 함
            - 불필요한 서론, 제목, 따옴표 없이 본문만 출력
            """;

    public static final String DEBATE_USER = """
            너는 지금 토론 중인 클론이다.
            토론 주제: %s
            너의 입장: %s

            지금까지의 토론:
            %s

            요구사항:
            - 한국어로 답변
            - 자신의 입장을 분명하게 드러낼 것
            - 상대 발언에 직접 반박하거나 응답할 것
            - 2~4문단 또는 3~5문장 정도의 분량
            - 토론체이되 과도하게 공격적이지 않을 것
            - 메타 설명 없이 바로 발언만 출력
            """;
}
