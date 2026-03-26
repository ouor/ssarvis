# Backend API Spec

Base URL
- Local: `http://localhost:8080`

Content Type
- Default Request: `application/json`
- Default Response: `application/json`

## Authentication

인증 방식
- JWT access token

인증 헤더

```http
Authorization: Bearer <access-token>
```

보호 대상 API
- `/api/auth/me` (`GET`, `DELETE`)
- `/api/system-prompt`
- `/api/clones` (`GET`)
- `/api/voices` (`GET`, `POST`)
- `/api/chat/**`
- `/api/debates/**`

공통 규칙
- 보호 대상 API는 `Authorization` 헤더가 필요하다.
- access token이 없거나 잘못되었거나 만료되면 `401 Unauthorized`를 반환한다.
- soft delete 된 회원은 보호 대상 API를 사용할 수 없다.
- 회원 탈퇴는 hard delete가 아니라 `users.deleted_at`을 채우는 soft delete로 처리된다.
- soft delete 된 회원의 클론, 음성, 대화, 논쟁, 오디오 자산은 삭제되지 않고 그대로 남는다.
- 로그인한 회원은 자신의 클론, 음성, 대화, 논쟁, 오디오 자산에만 접근할 수 있다.
- 단, 클론과 음성은 소유자가 `isPublic=true`로 전환하면 다른 로그인 회원도 조회하고 사용할 수 있다.
- 공개 자산의 관리(공개/비공개 전환)는 소유자만 가능하다.
- 공개 자산을 사용해 생성한 채팅/논쟁 기록은 사용한 회원 소유로 저장된다.
- 다른 회원의 리소스를 참조하면 서버는 일반적으로 `404 Not Found`로 응답한다.

## POST `/api/auth/signup`

회원가입을 수행하고 access token을 즉시 발급한다.

### Request Body

```json
{
  "username": "haru",
  "password": "secret123",
  "displayName": "하루"
}
```

### Request Rules

- `username`은 비어 있으면 안 된다.
- `username`은 최대 50자다.
- `password`는 비어 있으면 안 된다.
- `password`는 4자 이상 100자 이하여야 한다.
- `displayName`은 비어 있으면 안 된다.
- `displayName`은 최대 100자다.
- `username`은 고유해야 한다.

### Success Response

Status
- `200 OK`

Body

```json
{
  "userId": 1,
  "username": "haru",
  "displayName": "하루",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Error Responses

#### `400 Bad Request`

입력 검증 실패.

#### `409 Conflict`

```json
{
  "timestamp": "2026-03-26T01:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Username is already taken.",
  "details": [],
  "path": "/api/auth/signup"
}
```

## POST `/api/auth/login`

아이디/비밀번호로 로그인하고 access token을 발급한다.

### Request Body

```json
{
  "username": "haru",
  "password": "secret123"
}
```

### Success Response

Status
- `200 OK`

Body

```json
{
  "userId": 1,
  "username": "haru",
  "displayName": "하루",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Error Responses

#### `401 Unauthorized`

- 아이디/비밀번호가 잘못되었거나
- soft delete 된 회원인 경우

```json
{
  "timestamp": "2026-03-26T01:05:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password.",
  "details": [],
  "path": "/api/auth/login"
}
```

## POST `/api/auth/logout`

서버 저장 세션 없이 동작하므로, 로그아웃은 클라이언트가 현재 access token을 폐기하는 의미다.

설명
- 이 엔드포인트 자체는 토큰이 없어도 호출할 수 있다.
- 현재 프론트는 서버 `logout` 호출 없이 로컬 토큰만 제거하는 방식으로 로그아웃한다.

### Success Response

Status
- `204 No Content`

## GET `/api/auth/me`

현재 access token에 해당하는 회원 정보를 반환한다.

### Success Response

Status
- `200 OK`

Body

```json
{
  "userId": 1,
  "username": "haru",
  "displayName": "하루"
}
```

### Error Responses

#### `401 Unauthorized`

- `Authorization` 헤더가 없거나
- access token이 잘못되었거나
- access token의 회원이 soft delete 상태인 경우

## DELETE `/api/auth/me`

현재 로그인한 회원을 soft delete 처리한다.

설명
- 회원 레코드는 삭제되지 않고 `deleted_at`만 기록된다.
- 탈퇴 직후 현재 access token으로도 더 이상 보호 대상 API에 접근할 수 없다.
- 클론, 음성, 대화, 논쟁, 오디오 자산 같은 기존 소유 데이터는 삭제되지 않는다.
- soft delete 된 회원은 같은 아이디로 다시 로그인할 수 없다.

### Success Response

Status
- `204 No Content`

### Error Responses

#### `401 Unauthorized`

- `Authorization` 헤더가 없거나
- access token이 잘못되었거나
- 이미 soft delete 된 회원 토큰인 경우

## POST `/api/system-prompt`

설문 응답을 받아 OpenAI를 통해 시스템 프롬프트를 생성한다.
생성에 성공하면 결과와 요청 요약을 현재 로그인한 회원의 클론 이력으로 저장한다.

내부 생성 순서
- 1단계: 설문 응답으로 사용자 모사용 `systemPrompt`를 생성한다.
- 2단계: 생성된 `systemPrompt`를 바탕으로 `alias`를 생성한다.
- 3단계: 같은 `systemPrompt`를 바탕으로 `shortDescription`를 생성한다.
- `alias`가 비어 있으면 서버는 `"새 클론"`을 기본값으로 사용한다.
- `shortDescription`이 비어 있으면 서버는 `systemPrompt` 앞부분을 축약해 사용한다.

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

- 인증 필요
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
  "alias": "차분한 조력자",
  "shortDescription": "차분하고 구조적인 설명을 선호하는 클론",
  "systemPrompt": "사용자와 대화할 때는 차분하고 사려 깊은 톤을 유지하세요..."
}
```

### Error Responses

#### `400 Bad Request`

입력 JSON 구조가 잘못되었거나 검증에 실패한 경우.

#### `401 Unauthorized`

인증 실패.

#### `502 Bad Gateway`

OpenAI API 호출이 실패했거나, 응답 본문에서 모델 출력 텍스트를 추출할 수 없는 경우.

#### `500 Internal Server Error`

서버 설정 누락 또는 내부 처리 오류.

대표 사례
- `OPENAI_API_KEY` 미설정
- OpenAI 요청 직렬화 실패

## GET `/api/clones`

클론 목록을 조회한다.

Query Parameter
- `scope=mine|public`
- 기본값은 `mine`

### Success Response

Status
- `200 OK`

Body

```json
[
  {
    "cloneId": 12,
    "createdAt": "2026-03-25T11:20:00Z",
    "alias": "차분한 조력자",
    "shortDescription": "차분하고 구조적인 설명을 선호하는 클론",
    "isPublic": false,
    "ownerDisplayName": "하루"
  }
]
```

설명
- 각 클론은 `prompt_generation_logs`의 한 레코드를 의미한다.
- `scope=mine`이면 현재 로그인 회원 소유 클론만 반환된다.
- `scope=public`이면 공개된 클론만 반환된다.
- 공개 클론 목록에는 다른 회원 자산도 포함될 수 있다.

## PATCH `/api/clones/{cloneId}/visibility`

클론 공개 여부를 변경한다.

### Request Body

```json
{
  "isPublic": true
}
```

### Request Rules

- 인증 필요
- 소유자만 변경 가능
- `isPublic`은 필수다.

### Success Response

Status
- `200 OK`

Body

```json
{
  "cloneId": 12,
  "isPublic": true
}
```

## POST `/api/voices`

현재 로그인한 회원이 업로드한 음성 샘플 파일을 DashScope에 등록하고, 이후 채팅 TTS에서 사용할 등록 음성 ID를 반환한다.

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

- 인증 필요
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
- 이 등록 음성은 생성한 회원 본인만 조회하고 사용할 수 있다.

## GET `/api/voices`

음성 목록을 조회한다.

Query Parameter
- `scope=mine|public`
- 기본값은 `mine`

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
    "createdAt": "2026-03-25T11:21:00Z",
    "isPublic": false,
    "ownerDisplayName": "하루"
  }
]
```

설명
- 현재 서버의 `DASHSCOPE_TTS_MODEL`과 호환되는 등록 음성만 반환된다.
- `scope=mine`이면 현재 로그인 회원 소유 음성만 반환된다.
- `scope=public`이면 공개된 음성만 반환된다.
- 공개 음성 목록에는 다른 회원 자산도 포함될 수 있다.

## PATCH `/api/voices/{registeredVoiceId}/visibility`

음성 공개 여부를 변경한다.

### Request Body

```json
{
  "isPublic": true
}
```

### Request Rules

- 인증 필요
- 소유자만 변경 가능
- `isPublic`은 필수다.

### Success Response

Status
- `200 OK`

Body

```json
{
  "registeredVoiceId": 5,
  "isPublic": true
}
```

## POST `/api/chat/messages`

현재 로그인한 회원의 클론을 기반으로 채팅 응답을 생성한다.
새 대화를 시작하거나, 기존 `conversationId`를 넘겨 같은 대화를 이어갈 수 있다.
선택적으로 등록된 음성을 함께 지정하면, 어시스턴트 응답의 TTS 오디오도 함께 내려준다.
성공하면 사용자 메시지와 어시스턴트 메시지가 모두 현재 회원 소유의 대화 이력으로 저장된다.

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

- 인증 필요
- `message`는 비어 있으면 안 된다.
- 새 대화를 시작할 때는 `promptGenerationLogId`가 필요하다.
- 기존 대화를 이어갈 때는 `conversationId`를 사용한다.
- `registeredVoiceId`는 선택 값이다.
- `promptGenerationLogId`, `registeredVoiceId`는 현재 로그인한 회원 소유이거나 공개 자산이어야 한다.
- `conversationId`는 항상 현재 로그인한 회원 소유여야 한다.
- `registeredVoiceId`가 있으면 해당 등록 음성으로 DashScope TTS를 생성한다.
- TTS는 DashScope realtime websocket을 통해 생성된다.
- TTS 입력 텍스트는 DashScope 제한에 맞추기 위해 UTF-8 기준 600바이트 이하로 분할되며, 가능하면 600바이트 이전의 마지막 `.` 기준으로 끊는다.
- `conversationId`가 있으면 해당 대화에 연결된 시스템 프롬프트를 기준으로 응답을 생성한다.
- 등록 음성은 현재 서버가 사용하는 `DASHSCOPE_TTS_MODEL`과 같은 모델로 등록된 경우에만 사용할 수 있다.
- OpenAI로 보내는 채팅 컨텍스트는 아래 순서로 구성된다.
  - `system`: 클론 시스템 프롬프트
  - 최근 `OPENAI_CHAT_HISTORY_TURNS`턴의 히스토리
  - `system`: 채팅 생성 지시문
  - `user`: 이번 사용자 메시지

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

## POST `/api/chat/messages/stream`

채팅 응답을 NDJSON 스트림으로 내려준다. 텍스트를 먼저 전달하고, 이후 PCM 오디오 청크를 순차적으로 보낸다.
스트리밍 버전도 동일하게 현재 로그인한 회원의 리소스만 사용할 수 있다.

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

프론트 참고
- 현재 프론트는 `message`를 먼저 화면에 추가하고, `done` 시점에 누적 PCM을 WAV로 합쳐 마지막 어시스턴트 메시지에 `<audio>` URL을 연결한다.

## GET `/api/chat/conversations`

현재 로그인한 회원의 채팅 기록 목록을 최신순으로 조회한다.

### Success Response

Status
- `200 OK`

Body

```json
[
  {
    "conversationId": 31,
    "cloneId": 12,
    "cloneAlias": "차분한 조력자",
    "createdAt": "2026-03-26T03:00:00Z",
    "latestMessagePreview": "조금 더 짧고 체크리스트 형태로 바꿔줘.",
    "messageCount": 6
  }
]
```

## GET `/api/chat/conversations/{conversationId}`

현재 로그인한 회원의 특정 채팅 기록 전체를 조회한다.

### Success Response

Status
- `200 OK`

Body

```json
{
  "conversationId": 31,
  "cloneId": 12,
  "cloneAlias": "차분한 조력자",
  "cloneShortDescription": "차분하고 구조적인 설명을 선호하는 클론",
  "createdAt": "2026-03-26T03:00:00Z",
  "messages": [
    {
      "role": "user",
      "content": "오늘 일정 정리해줘.",
      "createdAt": "2026-03-26T03:00:01Z",
      "ttsAudioUrl": null,
      "ttsVoiceId": null
    },
    {
      "role": "assistant",
      "content": "좋아요. 우선순위 기준으로 정리해볼게요.",
      "createdAt": "2026-03-26T03:00:02Z",
      "ttsAudioUrl": "https://cdn.example.com/audio/chat-31.mp3",
      "ttsVoiceId": "voice-id"
    }
  ]
}
```

## POST `/api/debates`

현재 로그인한 회원의 두 클론과 주제를 받아 논쟁 세션을 시작하고, 첫 번째 발언만 생성한다.
이후 브라우저는 오디오 재생이 끝날 때마다 다음 턴 API를 호출해 논쟁을 계속 이어갈 수 있다.
생성된 논쟁 세션은 현재 로그인한 회원 소유로 저장된다.

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

### Request Rules

- 인증 필요
- `cloneAId`, `cloneBId`는 서로 다른 값이어야 한다.
- `cloneAVoiceId`, `cloneBVoiceId`는 모두 필요하다.
- `topic`은 비어 있으면 안 된다.
- `cloneAId`, `cloneBId`, `cloneAVoiceId`, `cloneBVoiceId`는 현재 로그인한 회원 소유이거나 공개 자산이어야 한다.
- 첫 응답은 항상 `CLONE_A`의 발언이다.
- 각 발언은 선택한 등록 음성으로 TTS를 생성한다.
- 각 등록 음성은 현재 서버의 `DASHSCOPE_TTS_MODEL`과 호환되어야 한다.
- OpenAI로 보내는 논쟁 컨텍스트는 현재 발언하는 클론 기준으로 아래 순서로 구성된다.
  - `system`: 현재 클론의 시스템 프롬프트
  - 최근 `OPENAI_CHAT_HISTORY_TURNS`턴의 논쟁 히스토리
  - 히스토리에서 현재 클론의 과거 발언은 `user`, 상대 클론 발언은 `assistant` 역할로 매핑된다.
  - 마지막 `system`: 논쟁 생성 지시문
- 논쟁 생성 지시문에는 현재 주제가 반드시 포함된다.
- 서버는 클론 A/B에 고정된 찬반 역할을 부여하지 않는다. 각 클론은 자신의 성격 프롬프트와 대화 흐름에 따라 자유롭게 동의, 반박, 유보, 관점 전환을 할 수 있다.

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

## POST `/api/debates/stream`

논쟁 세션을 만들고 첫 턴을 NDJSON 스트림으로 내려준다. 첫 이벤트는 항상 `CLONE_A`의 발언이며, 이후 PCM 오디오 청크와 완료 이벤트가 이어진다.

프론트 참고
- 프론트는 첫 턴 스트림이 끝난 뒤 `/api/debates/{id}/next/stream`를 반복 호출해 논쟁을 이어간다.
- 더 이상 별도 `stop` API는 없고, 클라이언트가 다음 턴 요청을 중단하면 논쟁도 종료된다.

## POST `/api/debates/{debateSessionId}/next`

기존 논쟁 세션의 다음 턴 1개를 생성한다.
세션은 `CLONE_A -> CLONE_B -> CLONE_A -> ...` 순서로 계속 이어진다.

추가 규칙
- 인증 필요
- `debateSessionId`는 현재 로그인한 회원 소유여야 한다.

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

## POST `/api/debates/{debateSessionId}/next/stream`

기존 논쟁 세션의 다음 턴 1개를 NDJSON 스트림으로 내려준다.
브라우저는 마지막 오디오 재생이 끝난 뒤 이 엔드포인트를 다시 호출해 논쟁을 이어간다.

### Debate Stream Event Types

```json
{"type":"turn","debateSessionId":8,"topic":"원격근무가 대면근무보다 더 효율적인가?","turn":{"turnIndex":1,"speaker":"CLONE_A","cloneId":12,"content":"저는 원격근무가 더 효율적이라고 봅니다..."}}
{"type":"audio_chunk","audioFormat":"pcm_s16le","sampleRate":24000,"channels":1,"chunkBase64":"..."}
{"type":"done","debateSessionId":8,"turnIndex":1,"ttsVoiceId":"voice-id","hasAudio":true}
```

## GET `/api/debates`

현재 로그인한 회원의 논쟁 기록 목록을 최신순으로 조회한다.

### Success Response

Status
- `200 OK`

Body

```json
[
  {
    "debateSessionId": 8,
    "cloneAId": 12,
    "cloneAAlias": "차분한 조력자",
    "cloneBId": 13,
    "cloneBAlias": "냉철한 반론가",
    "topic": "원격근무가 대면근무보다 더 효율적인가?",
    "createdAt": "2026-03-26T03:30:00Z",
    "turnCount": 4
  }
]
```

## GET `/api/debates/{debateSessionId}`

현재 로그인한 회원의 특정 논쟁 기록 전체를 조회한다.

### Success Response

Status
- `200 OK`

Body

```json
{
  "debateSessionId": 8,
  "cloneAId": 12,
  "cloneAAlias": "차분한 조력자",
  "cloneAShortDescription": "차분하고 구조적인 설명을 선호하는 클론",
  "cloneAVoiceId": 5,
  "cloneBId": 13,
  "cloneBAlias": "냉철한 반론가",
  "cloneBShortDescription": "비판적으로 따져 묻는 클론",
  "cloneBVoiceId": 6,
  "topic": "원격근무가 대면근무보다 더 효율적인가?",
  "createdAt": "2026-03-26T03:30:00Z",
  "turns": [
    {
      "turnIndex": 1,
      "speaker": "CLONE_A",
      "cloneId": 12,
      "content": "저는 원격근무가 더 효율적이라고 봅니다.",
      "createdAt": "2026-03-26T03:30:01Z",
      "ttsAudioUrl": null,
      "ttsVoiceId": null
    }
  ]
}
```

### Common Error Responses For Protected APIs

#### `401 Unauthorized`

```json
{
  "timestamp": "2026-03-26T02:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authorization header is required.",
  "details": [],
  "path": "/api/clones"
}
```

#### `404 Not Found`

존재하지 않는 리소스이거나 다른 회원 소유 리소스인 경우.

## Environment

필수 또는 권장 환경 변수는 [backend/.env.example](./.env.example)에 정리되어 있다.

- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `OPENAI_BASE_URL`
- `OPENAI_CHAT_HISTORY_TURNS`
- `APP_AUTH_JWT_SECRET`
- `APP_AUTH_JWT_ACCESS_TOKEN_EXPIRATION_MINUTES`
- `APP_CORS_ALLOWED_ORIGINS`
- `DASHSCOPE_API_KEY`
- `DASHSCOPE_BASE_URL`
- `DASHSCOPE_REALTIME_URL`
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
- `APP_BOOTSTRAP_DEFAULT_ACCOUNT_ENABLED`
- `APP_BOOTSTRAP_DEFAULT_ACCOUNT_USERNAME`
- `APP_BOOTSTRAP_DEFAULT_ACCOUNT_PASSWORD`
- `APP_BOOTSTRAP_DEFAULT_ACCOUNT_DISPLAY_NAME`
- `APP_BOOTSTRAP_DEFAULT_ACCOUNT_VOICE_SAMPLE_PATHS`
- `APP_BOOTSTRAP_DEFAULT_ACCOUNT_VOICE_ALIASES`

참고
- `integrationTest` Gradle 태스크는 `backend/.env` 파일이 있으면 그 값을 읽어 테스트 프로세스 환경변수로 주입한다.
- 현재 `integrationTest`는 회원가입으로 JWT를 발급받은 뒤, 실제 OpenAI, 실제 DashScope, 실제 MySQL을 호출한다.
- `integrationTest`의 음성 등록 샘플은 외부 합성 대신 `backend/src/test/resources/sample/haru.wav` 파일을 사용한다.
- `integrationTest`는 soft delete 후 기존 토큰이 보호 API에서 거부되는지도 함께 검증한다.
- `integrationTest`는 공개 클론/공개 음성을 다른 회원이 사용한 뒤, 소유자가 비공개로 되돌리면 기존 기록은 유지되고 신규 사용만 차단되는 흐름도 검증한다.
- OpenAI 호출은 모두 `POST /v1/chat/completions` 형식을 사용한다.
- 시스템 프롬프트 생성은 한 번에 JSON을 받는 방식이 아니라 `systemPrompt -> alias -> shortDescription` 순서의 단계형 OpenAI 호출로 처리된다.
- 채팅 이어가기 시에는 시스템 프롬프트를 항상 포함하고, 과거 대화는 최근 `OPENAI_CHAT_HISTORY_TURNS`턴만 OpenAI로 전송한다.
- 논쟁도 시스템 프롬프트를 항상 포함하고, 최근 `OPENAI_CHAT_HISTORY_TURNS`턴만 OpenAI로 전송한다.
- `S3_ENABLED=true` 이고 S3 설정이 유효하면, DashScope에서 받은 TTS 오디오는 ffmpeg로 MP3 인코딩 후 S3에 업로드된다.
- 프론트 응답은 기존과 동일하게 Base64 오디오를 포함하고, S3 업로드는 서버 내부 저장 및 이력 관리 용도로 수행된다.
- 스트리밍 엔드포인트는 DashScope realtime websocket에서 PCM 청크를 받아 NDJSON으로 전달한다.
- OpenAI, DashScope 음성 등록 HTTP 요청, DashScope realtime 연결, DashScope realtime 이벤트 대기는 모두 20초 무응답 기준으로 실패 처리된다.
- `APP_BOOTSTRAP_DEFAULT_ACCOUNT_ENABLED=true` 이면 서버 시작 시 지정한 기본 계정이 없을 때만 기본 계정, 기본 클론 2개, 기본 음성 2개를 자동 등록한다.
- 이 부트스트랩은 별도 API를 노출하지 않고 애플리케이션 시작 시 내부적으로만 동작한다.
- 기본 음성 부트스트랩은 실제 `/api/voices`와 같은 DashScope 등록 로직을 사용하므로, 샘플 파일 경로와 DashScope 설정이 유효해야 한다.
- `APP_BOOTSTRAP_DEFAULT_ACCOUNT_VOICE_SAMPLE_PATHS`는 절대경로 또는 상대경로를 받을 수 있고, 상대경로는 현재 작업 디렉터리 우선, 없으면 `backend` 기준으로 해석한다.

## Persistence Side Effects

### 시스템 프롬프트 생성 성공 시

`prompt_generation_logs` 테이블에 아래 정보가 저장된다.

- 소유 회원(`user_id`)
- 생성 시각
- 사용한 모델명
- 설문 응답 JSON
- 생성된 별칭
- 생성된 간단한 설명
- 생성된 시스템 프롬프트 본문
- 공개 여부(`is_public`)

### 채팅 응답 생성 성공 시

아래 테이블에 대화 이력이 저장된다.

- `chat_conversations`
- `chat_messages`

저장되는 정보
- 소유 회원(`user_id`)
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
- 소유 회원(`user_id`)
- DashScope 등록 음성 ID
- 사용자 표시용 `displayName`
- 대상 TTS 모델명
- 내부 생성된 `preferredName`
- 업로드한 원본 파일명
- 업로드 파일 MIME 타입
- 생성 시각
- 공개 여부(`is_public`)

### 클론 논쟁 생성 성공 시

아래 테이블에 논쟁 정보가 저장된다.

- `debate_sessions`
- `debate_turns`

저장되는 정보
- 소유 회원(`user_id`)
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
- 소유 회원(`user_id`)
- 스토리지 제공자명
- 버킷 이름
- 오브젝트 키
- 오브젝트 URL
- DashScope 원본 오디오 MIME 타입
- ffmpeg 인코딩 후 저장된 MIME 타입
- 원본/저장 바이트 크기
- DashScope 등록 음성 ID
- 생성 시각
