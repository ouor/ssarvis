# Backend API Spec

Base URL
- Local: `http://localhost:8080`

Content Type
- Default Request: `application/json`
- Default Response: `application/json`

## POST `/api/system-prompt`

설문 응답을 받아 OpenAI를 통해 시스템 프롬프트를 생성한다.
생성에 성공하면 결과와 요청 요약을 DB에 이력으로 저장한다.

### Request Body

```json
{
  "answers": [
    {
      "question": "혼자 있는 시간과 사람 만나는 시간 중 어느 쪽이 더 편한가요?",
      "answer": "혼자가 더 편함"
    },
    {
      "question": "평소 결정은 어떤 편인가요?",
      "answer": "충분히 고민하고 정함"
    }
  ]
}
```

### Request Rules

- `answers`는 최소 1개 이상이어야 한다.
- 각 항목의 `question`은 비어 있으면 안 된다.
- 각 항목의 `answer`는 비어 있으면 안 된다.

### Success Response

Status
- `200 OK`

Body

```json
{
  "promptGenerationLogId": 12,
  "systemPrompt": "사용자와 대화할 때는 차분하고 사려 깊은 톤을 유지하세요..."
}
```

### Error Responses

#### `400 Bad Request`

입력 JSON 구조가 잘못되었거나 검증에 실패한 경우.

```json
{
  "timestamp": "2026-03-25T08:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed.",
  "details": [
    "answers: answers must contain at least one item."
  ],
  "path": "/api/system-prompt"
}
```

#### `502 Bad Gateway`

OpenAI API 호출이 실패했거나, 응답 본문에서 모델 출력 텍스트를 추출할 수 없는 경우.

```json
{
  "timestamp": "2026-03-25T08:31:00Z",
  "status": 502,
  "error": "Bad Gateway",
  "message": "OpenAI request failed with status 500. Body: ...",
  "details": [],
  "path": "/api/system-prompt"
}
```

#### `500 Internal Server Error`

서버 설정 누락 또는 내부 처리 오류.

대표 사례
- `OPENAI_API_KEY` 미설정
- OpenAI 요청 직렬화 실패
- 요청 처리 중 인터럽트 발생

```json
{
  "timestamp": "2026-03-25T08:32:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "OPENAI_API_KEY is not configured.",
  "details": [],
  "path": "/api/system-prompt"
}
```

## POST `/api/voices`

사용자가 업로드한 음성 샘플 파일을 DashScope에 등록하고, 이후 채팅 TTS에서 사용할 등록 음성 ID를 반환한다.

Content Type
- Request: `multipart/form-data`
- Response: `application/json`

### Request Body

Form field
- `sample`: 업로드할 음성 파일

예시
- `sample=voice.mp3`

### Request Rules

- `sample` 파일은 비어 있으면 안 된다.
- 파일의 MIME 타입이 있어야 한다.
- DashScope API 키가 서버에 설정되어 있어야 한다.
- 서버는 업로드한 파일명에서 DashScope `preferred_name`을 내부적으로 생성한다.

### Success Response

Status
- `200 OK`

Body

```json
{
  "registeredVoiceId": 5,
  "voiceId": "qwen-tts-vc-samplevoice-voice-20260325184538121-0d52",
  "preferredName": "samplevoice",
  "originalFilename": "voice.mp3",
  "audioMimeType": "audio/mpeg"
}
```

설명
- `preferredName`은 서버가 업로드 파일명을 바탕으로 내부적으로 생성하는 DashScope 등록용 이름이다.
- 이후 채팅 요청에서 `registeredVoiceId`를 보내면 이 등록 음성을 사용해 TTS를 생성한다.

### Error Responses

#### `400 Bad Request`

파일이 없거나 잘못된 경우.

```json
{
  "timestamp": "2026-03-25T08:39:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Voice sample file is required.",
  "details": [],
  "path": "/api/voices"
}
```

#### `502 Bad Gateway`

DashScope 음성 등록 호출이 실패한 경우.

```json
{
  "timestamp": "2026-03-25T08:39:30Z",
  "status": 502,
  "error": "Bad Gateway",
  "message": "DashScope voice registration failed with status 400. Body: ...",
  "details": [],
  "path": "/api/voices"
}
```

#### `500 Internal Server Error`

서버 설정 누락 또는 파일 처리 오류.

```json
{
  "timestamp": "2026-03-25T08:39:50Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "DASHSCOPE_API_KEY is not configured.",
  "details": [],
  "path": "/api/voices"
}
```

## POST `/api/chat/messages`

생성된 시스템 프롬프트를 기반으로 채팅 응답을 생성한다.
새 대화를 시작하거나, 기존 `conversationId`를 넘겨 같은 대화를 이어갈 수 있다.
선택적으로 등록된 음성을 함께 지정하면, 어시스턴트 응답의 TTS 오디오도 함께 내려준다.
성공하면 사용자 메시지와 어시스턴트 메시지가 모두 DB에 저장된다.

### Request Body

새 대화 시작:

```json
{
  "promptGenerationLogId": 12,
  "registeredVoiceId": 5,
  "message": "내 말투에 맞춰 오늘 할 일을 정리해줘."
}
```

기존 대화 이어가기:

```json
{
  "conversationId": 31,
  "registeredVoiceId": 5,
  "message": "조금 더 짧고 체크리스트 형태로 바꿔줘."
}
```

### Request Rules

- `message`는 비어 있으면 안 된다.
- 새 대화를 시작할 때는 `promptGenerationLogId`가 필요하다.
- 기존 대화를 이어갈 때는 `conversationId`를 사용한다.
- `registeredVoiceId`는 선택 값이다.
- `registeredVoiceId`가 있으면 해당 등록 음성으로 DashScope TTS를 생성한다.
- TTS 요청 시 서버는 `language_type`을 `Auto`로 사용한다.
- TTS 요청 본문은 DashScope 제한에 맞추기 위해 UTF-8 기준 600바이트 이하로 잘라서 전송한다.
- `conversationId`가 있으면 해당 대화에 연결된 시스템 프롬프트를 기준으로 응답을 생성한다.

### Success Response

Status
- `200 OK`

Body

```json
{
  "conversationId": 31,
  "assistantMessage": "좋아요. 오늘 해야 할 일을 우선순위 기준으로 짧게 정리해드릴게요...",
  "ttsVoiceId": "qwen-tts-vc-samplevoice-voice-20260325184538121-0d52",
  "ttsAudioMimeType": "audio/wav",
  "ttsAudioBase64": "UklGR..."
}
```

설명
- `ttsVoiceId`, `ttsAudioMimeType`, `ttsAudioBase64`는 `registeredVoiceId`를 함께 보냈고 TTS 생성에 성공한 경우에만 채워질 수 있다.
- 등록 음성을 지정하지 않았거나 TTS 생성이 생략된 경우 위 필드는 `null`로 취급하면 된다.

### Error Responses

#### `400 Bad Request`

입력 JSON 구조가 잘못되었거나 검증에 실패한 경우.

```json
{
  "timestamp": "2026-03-25T08:40:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed.",
  "details": [
    "message: message must not be blank."
  ],
  "path": "/api/chat/messages"
}
```

또는 필수 식별자가 빠진 경우.

```json
{
  "timestamp": "2026-03-25T08:40:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "promptGenerationLogId is required when conversationId is not provided.",
  "details": [],
  "path": "/api/chat/messages"
}
```

#### `404 Not Found`

참조한 프롬프트 생성 로그, 대화, 등록 음성이 없는 경우.

```json
{
  "timestamp": "2026-03-25T08:41:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Conversation not found.",
  "details": [],
  "path": "/api/chat/messages"
}
```

#### `502 Bad Gateway`

OpenAI API 호출이 실패했거나, 응답 본문에서 모델 출력 텍스트를 추출할 수 없는 경우.
또는 DashScope TTS 호출이 실패한 경우.

```json
{
  "timestamp": "2026-03-25T08:42:00Z",
  "status": 502,
  "error": "Bad Gateway",
  "message": "DashScope TTS failed with status 400. Body: ...",
  "details": [],
  "path": "/api/chat/messages"
}
```

#### `500 Internal Server Error`

서버 설정 누락 또는 내부 처리 오류.

```json
{
  "timestamp": "2026-03-25T08:43:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "OPENAI_API_KEY is not configured.",
  "details": [],
  "path": "/api/chat/messages"
}
```

## Environment

필수 또는 권장 환경 변수는 [backend/.env.example](/C:/Users/hurwy/Codes/ssarvis/backend/.env.example)에 정리되어 있다.

- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `OPENAI_BASE_URL`
- `APP_CORS_ALLOWED_ORIGINS`
- `DASHSCOPE_API_KEY`
- `DASHSCOPE_BASE_URL`
- `DASHSCOPE_TTS_MODEL`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

참고
- `integrationTest` Gradle 태스크는 `backend/.env` 파일이 있으면 그 값을 읽어 테스트 프로세스 환경변수로 주입한다.
- 현재 `integrationTest`는 실제 OpenAI, 실제 DashScope, 실제 MySQL을 호출한다.

## Persistence Side Effects

### 시스템 프롬프트 생성 성공 시

`prompt_generation_logs` 테이블에 아래 정보가 저장된다.

- 생성 시각
- 사용한 모델명
- 설문 응답 JSON
- 생성된 시스템 프롬프트 본문

### 채팅 응답 생성 성공 시

아래 테이블에 대화 이력이 저장된다.

- `chat_conversations`
- `chat_messages`

저장되는 정보
- 어떤 시스템 프롬프트 생성 로그에서 시작된 대화인지
- 사용자 메시지 본문
- 어시스턴트 메시지 본문
- 각 레코드 생성 시각

### 음성 등록 성공 시

아래 테이블에 등록 음성 정보가 저장된다.

- `registered_voices`

저장되는 정보
- DashScope 등록 음성 ID
- 대상 TTS 모델명
- 내부 생성된 `preferredName`
- 업로드한 원본 파일명
- 업로드 파일 MIME 타입
- 생성 시각
