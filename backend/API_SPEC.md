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
- `alias`: 사용자가 붙일 음성 별칭, 선택 값

예시
- `sample=voice.mp3`
- `alias=차분한 민지`

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
  "displayName": "차분한 민지",
  "preferredName": "samplevoice",
  "originalFilename": "voice.mp3",
  "audioMimeType": "audio/mpeg"
}
```

설명
- `displayName`은 프론트에 보여줄 사용자 입력 별칭이다. 별칭을 보내지 않으면 파일명 기반 값이 사용된다.
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

## GET `/api/clones`

저장된 시스템 프롬프트 로그를 클론 목록으로 조회한다.

### Success Response

Status
- `200 OK`

Body

```json
[
  {
    "cloneId": 12,
    "createdAt": "2026-03-25T11:20:00Z",
    "preview": "당신은 사용자를 차분하고 신뢰감 있게 돕는 한국어 AI 어시스턴트다..."
  }
]
```

설명
- 각 클론은 `prompt_generation_logs`의 한 레코드를 의미한다.
- `preview`는 시스템 프롬프트 본문의 앞부분 요약이다.

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

## POST `/api/chat/messages/stream`

채팅 응답을 NDJSON 스트림으로 내려준다. 텍스트를 먼저 전달하고, 이후 PCM 오디오 청크를 순차적으로 보낸다.
서버는 DashScope가 반환한 WAV 오디오를 다운로드하면서 PCM 본문을 잘라 프론트에 전달한다.
동시에 완성된 전체 오디오는 ffmpeg 인코딩 후 S3 및 DB 이력에 저장된다.

Content Type
- Response: `application/x-ndjson`

### Stream Event Types

```json
{"type":"message","conversationId":31,"assistantMessage":"좋아요. 우선순위부터 정리해볼게요."}
{"type":"audio_chunk","audioFormat":"pcm_s16le","sampleRate":24000,"channels":1,"chunkBase64":"..."}
{"type":"audio_chunk","audioFormat":"pcm_s16le","sampleRate":24000,"channels":1,"chunkBase64":"..."}
{"type":"done","conversationId":31,"ttsVoiceId":"voice-id","hasAudio":true}
```

오류가 발생하면 중간에 아래 이벤트가 올 수 있다.

```json
{"type":"error","message":"Failed to stream TTS audio."}
```

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

## GET `/api/voices`

등록된 음성 목록을 조회한다.

### Success Response

Status
- `200 OK`

Body

```json
[
  {
    "registeredVoiceId": 5,
    "voiceId": "qwen-tts-vc-samplevoice-voice-20260325184538121-0d52",
    "displayName": "차분한 민지",
    "preferredName": "samplevoice",
    "originalFilename": "voice.mp3",
    "audioMimeType": "audio/mpeg",
    "createdAt": "2026-03-25T11:21:00Z"
  }
]
```

## POST `/api/debates`

두 클론과 주제를 받아 논쟁 세션을 시작하고, 첫 번째 발언만 생성한다.
이후 브라우저는 오디오 재생이 끝날 때마다 다음 턴 API를 호출해 논쟁을 계속 이어갈 수 있다.
각 발언은 DB에 저장되고, 선택한 음성으로 TTS 오디오도 함께 내려온다.

### Request Body

```json
{
  "cloneAId": 12,
  "cloneBId": 13,
  "cloneAVoiceId": 5,
  "cloneBVoiceId": 6,
  "topic": "원격근무가 대면근무보다 더 효율적인가?"
}
```

## POST `/api/debates/stream`

논쟁 세션을 만들고 첫 턴을 NDJSON 스트림으로 내려준다. 첫 이벤트는 항상 `CLONE_A`의 발언이며, 이후 PCM 오디오 청크와 완료 이벤트가 이어진다.

## POST `/api/debates/{debateSessionId}/next/stream`

기존 논쟁 세션의 다음 턴 1개를 NDJSON 스트림으로 내려준다.
브라우저는 마지막 오디오 재생이 끝난 뒤 이 엔드포인트를 다시 호출해 논쟁을 이어간다.

### Debate Stream Event Types

```json
{"type":"turn","debateSessionId":8,"topic":"원격근무가 대면근무보다 더 효율적인가?","turn":{"turnIndex":1,"speaker":"CLONE_A","cloneId":12,"content":"저는 원격근무가 더 효율적이라고 봅니다..."}}
{"type":"audio_chunk","audioFormat":"pcm_s16le","sampleRate":24000,"channels":1,"chunkBase64":"..."}
{"type":"done","debateSessionId":8,"turnIndex":1,"ttsVoiceId":"voice-id","hasAudio":true}
```

### Request Rules

- `cloneAId`, `cloneBId`는 서로 다른 값이어야 한다.
- `cloneAVoiceId`, `cloneBVoiceId`는 모두 필요하다.
- `topic`은 비어 있으면 안 된다.
- 첫 응답은 항상 `CLONE_A`의 발언이다.
- 각 발언은 선택한 등록 음성으로 TTS를 생성한다.

### Success Response

Status
- `200 OK`

Body

```json
{
  "debateSessionId": 8,
  "topic": "원격근무가 대면근무보다 더 효율적인가?",
  "turn": {
    "turnIndex": 1,
    "speaker": "CLONE_A",
    "cloneId": 12,
    "content": "저는 원격근무가 더 효율적이라고 봅니다...",
    "ttsVoiceId": "qwen-tts-vc-samplevoice-voice-20260325184538121-0d52",
    "ttsAudioMimeType": "audio/wav",
    "ttsAudioBase64": "UklGR..."
  }
}
```

## POST `/api/debates/{debateSessionId}/next`

기존 논쟁 세션의 다음 턴 1개를 생성한다.
세션은 `CLONE_A -> CLONE_B -> CLONE_A -> ...` 순서로 계속 이어진다.

### Success Response

Status
- `200 OK`

Body

```json
{
  "debateSessionId": 8,
  "topic": "원격근무가 대면근무보다 더 효율적인가?",
  "turn": {
    "turnIndex": 2,
    "speaker": "CLONE_B",
    "cloneId": 13,
    "content": "저는 대면근무가 더 효율적이라고 봅니다...",
    "ttsVoiceId": "qwen-tts-vc-samplevoice-voice-20260325184538121-0d52",
    "ttsAudioMimeType": "audio/wav",
    "ttsAudioBase64": "UklGR..."
  }
}
```

## POST `/api/debates/{debateSessionId}/stop`

논쟁 세션을 중단한다.
브라우저가 자동으로 다음 턴을 요청하는 루프를 멈출 때 사용한다.

### Success Response

Status
- `204 No Content`

### Error Responses

#### `400 Bad Request`

```json
{
  "timestamp": "2026-03-25T11:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Two different clones must be selected.",
  "details": [],
  "path": "/api/debates"
}
```

#### `404 Not Found`

```json
{
  "timestamp": "2026-03-25T11:31:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Debate session not found.",
  "details": [],
  "path": "/api/debates/8/next"
}
```

#### `502 Bad Gateway`

```json
{
  "timestamp": "2026-03-25T11:32:00Z",
  "status": 502,
  "error": "Bad Gateway",
  "message": "DashScope TTS failed with status 400. Body: ...",
  "details": [],
  "path": "/api/debates"
}
```

## Environment

필수 또는 권장 환경 변수는 [backend/.env.example](/C:/Users/hurwy/Codes/ssarvis/backend/.env.example)에 정리되어 있다.

- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `OPENAI_BASE_URL`
- `OPENAI_CHAT_HISTORY_TURNS`
- `APP_CORS_ALLOWED_ORIGINS`
- `DASHSCOPE_API_KEY`
- `DASHSCOPE_BASE_URL`
- `DASHSCOPE_TTS_MODEL`
- `FFMPEG_PATH`
- `S3_ENABLED`
- `S3_BUCKET`
- `S3_REGION`
- `S3_ENDPOINT`
- `S3_ACCESS_KEY`
- `S3_SECRET_KEY`
- `S3_KEY_PREFIX`
- `S3_PATH_STYLE_ACCESS`
- `S3_PUBLIC_BASE_URL`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

참고
- `integrationTest` Gradle 태스크는 `backend/.env` 파일이 있으면 그 값을 읽어 테스트 프로세스 환경변수로 주입한다.
- 현재 `integrationTest`는 실제 OpenAI, 실제 DashScope, 실제 MySQL을 호출한다.
- OpenAI 호출은 모두 `POST /v1/chat/completions` 형식을 사용한다.
- 채팅 이어가기 시에는 시스템 프롬프트를 항상 포함하고, 과거 대화는 최근 `OPENAI_CHAT_HISTORY_TURNS`턴만 OpenAI로 전송한다.
- `S3_ENABLED=true` 이고 S3 설정이 유효하면, DashScope에서 받은 TTS 오디오는 ffmpeg로 MP3 인코딩 후 S3에 업로드된다.
- 프론트 응답은 기존과 동일하게 Base64 오디오를 포함하고, S3 업로드는 서버 내부 저장 및 이력 관리 용도로 수행된다.
- 스트리밍 엔드포인트는 DashScope가 반환한 WAV 오디오를 서버가 순차 다운로드하면서 PCM 청크로 잘라 NDJSON으로 전달한다.

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
- 선택적으로 연결된 TTS 오디오 자산 참조
- 각 레코드 생성 시각

추가 동작
- `S3_ENABLED=true` 일 때는 DashScope TTS 원본을 ffmpeg로 MP3 인코딩한 뒤 S3에 업로드한다.
- 업로드된 오디오 메타데이터는 `generated_audio_assets` 테이블에 저장되고, 어시스턴트 메시지는 해당 자산을 참조한다.

### 음성 등록 성공 시

아래 테이블에 등록 음성 정보가 저장된다.

- `registered_voices`

저장되는 정보
- DashScope 등록 음성 ID
- 사용자 표시용 `displayName`
- 대상 TTS 모델명
- 내부 생성된 `preferredName`
- 업로드한 원본 파일명
- 업로드 파일 MIME 타입
- 생성 시각

### 클론 논쟁 생성 성공 시

아래 테이블에 논쟁 정보가 저장된다.

- `debate_sessions`
- `debate_turns`

저장되는 정보
- 클론 A / 클론 B 참조
- 각 클론에 연결된 등록 음성 참조
- 논쟁 주제
- 각 발언의 순서, 발화자, 본문
- 선택적으로 연결된 TTS 오디오 자산 참조
- 생성 시각

### 생성 음성 저장 성공 시

아래 테이블에 인코딩 및 업로드된 오디오 자산 정보가 저장된다.

- `generated_audio_assets`

저장되는 정보
- 스토리지 제공자명
- 버킷 이름
- 오브젝트 키
- 오브젝트 URL
- DashScope 원본 오디오 MIME 타입
- ffmpeg 인코딩 후 저장된 MIME 타입
- 원본/저장 바이트 크기
- DashScope 등록 음성 ID
- 생성 시각
