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

OpenAI API 호출이 실패했거나, 응답 본문에 `output_text`가 없는 경우.

```json
{
  "timestamp": "2026-03-25T08:31:00Z",
  "status": 502,
  "error": "Bad Gateway",
  "message": "OpenAI request failed with status 500.",
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

## Environment

필수 또는 권장 환경 변수는 [backend/.env.example](/C:/Users/SSAFY/Codes/ssarvis/backend/.env.example)에 정리되어 있다.

- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `OPENAI_BASE_URL`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

## Persistence Side Effect

성공적으로 시스템 프롬프트가 생성되면 `prompt_generation_logs` 테이블에 아래 정보가 저장된다.

- 생성 시각
- 사용한 모델명
- 설문 응답 JSON
- 생성된 시스템 프롬프트 본문
