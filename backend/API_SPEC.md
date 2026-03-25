# Backend API Spec

Base URL
- Local: `http://localhost:8080`

Content Type
- Request: `application/json`
- Response: `application/json`

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

## POST `/api/chat/messages`

생성된 시스템 프롬프트를 기반으로 채팅 응답을 생성한다.
새 대화를 시작하거나, 기존 `conversationId`를 넘겨 같은 대화를 이어갈 수 있다.
성공하면 사용자 메시지와 어시스턴트 메시지가 모두 DB에 저장된다.

### Request Body

새 대화 시작:

```json
{
  "promptGenerationLogId": 12,
  "message": "내 말투에 맞춰 오늘 할 일을 정리해줘."
}
```

기존 대화 이어가기:

```json
{
  "conversationId": 31,
  "message": "조금 더 짧고 체크리스트 형태로 바꿔줘."
}
```

### Request Rules

- `message`는 비어 있으면 안 된다.
- 새 대화를 시작할 때는 `promptGenerationLogId`가 필요하다.
- 기존 대화를 이어갈 때는 `conversationId`를 사용한다.
- `conversationId`가 있으면 해당 대화에 연결된 시스템 프롬프트를 기준으로 응답을 생성한다.

### Success Response

Status
- `200 OK`

Body

```json
{
  "conversationId": 31,
  "assistantMessage": "좋아요. 오늘 해야 할 일을 우선순위 기준으로 짧게 정리해드릴게요..."
}
```

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

참조한 프롬프트 생성 로그 또는 대화가 없는 경우.

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

```json
{
  "timestamp": "2026-03-25T08:42:00Z",
  "status": 502,
  "error": "Bad Gateway",
  "message": "OpenAI response did not include output_text.",
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
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

참고
- `integrationTest` Gradle 태스크는 `backend/.env` 파일이 있으면 그 값을 읽어 테스트 프로세스 환경변수로 주입한다.

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
