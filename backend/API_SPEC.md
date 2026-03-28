# Backend API Spec

Base URL
- Local: `http://localhost:8080`

Content Type
- Default Request: `application/json`
- Default Response: `application/json`

## 1. Current API Landscape

현재 백엔드는 두 층을 함께 가지고 있다.

- SNS 본체 API
  - 인증, 프로필, 팔로우, 게시물, 사람 간 DM
- 프로필 하위 레거시 워크스페이스 API
  - 설문 기반 클론 생성, 보이스 등록, 클론 대화, 논쟁

제품 기준 우선순위
- 메인 사용자 흐름은 `/api/profiles`, `/api/follows`, `/api/posts`, `/api/dms`
- `/api/system-prompt`, `/api/clones`, `/api/voices`, `/api/chat`, `/api/debates`는 현재 `Profile` 하위 작업공간을 위해 유지 중

## 2. Authentication

인증 방식
- JWT access token

인증 헤더

```http
Authorization: Bearer <access-token>
```

보호 대상 API
- `/api/auth/me` (`GET`, `DELETE`)
- `/api/follows/**`
- `/api/profiles/**`
- `/api/posts/**`
- `/api/dms/**`
- `/api/system-prompt`
- `/api/clones/**`
- `/api/voices/**`
- `/api/chat/**`
- `/api/debates/**`
- `/api/friends/**`

공통 규칙
- 보호 대상 API는 `Authorization` 헤더가 필요하다.
- access token이 없거나 잘못되었거나 만료되면 `401 Unauthorized`를 반환한다.
- soft delete 된 회원은 보호 대상 API를 사용할 수 없다.
- 회원 탈퇴는 hard delete가 아니라 `users.deleted_at`을 채우는 soft delete다.
- 다른 회원 리소스에 권한 없이 접근하면 보통 `403 Forbidden` 또는 `404 Not Found`가 반환된다.

## 3. Core Product Policies Reflected In APIs

- 계정 공개성은 `PUBLIC`, `PRIVATE` 두 가지다.
- 공개 계정은 검색 가능하고 누구나 DM 시작 가능하다.
- 비공개 계정은 신규 팔로우가 불가능하다.
- 비공개 계정은 이미 팔로우 중인 사용자만 게시물 조회 및 DM 가능하다.
- 사람 간 DM이 기본이며, 자동응답은 텍스트 DM에 먼저 붙어 있다.
- 클론과 보이스는 현재 구현상 `사용자당 1개 대표 자산`으로 운용된다.
  - `/api/system-prompt`는 내 최신 클론을 새로 추가하지 않고 갱신한다.
  - `/api/voices` 등록은 내 최신 보이스를 새로 추가하지 않고 갱신한다.
- 친구 기반 자산 공유 API는 레거시 워크스페이스용으로 아직 남아 있다.
- 모든 보호 API 요청은 서버가 현재 사용자의 `lastActivityAt`을 갱신한다.

## 4. Auth APIs

## POST `/api/auth/signup`

회원가입과 동시에 access token을 발급한다.

### Request Body

```json
{
  "username": "haru",
  "password": "secret123",
  "displayName": "하루"
}
```

### Success Response

```json
{
  "userId": 1,
  "username": "haru",
  "displayName": "하루",
  "visibility": "PUBLIC",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

## POST `/api/auth/login`

로그인과 동시에 access token을 발급한다.

### Request Body

```json
{
  "username": "haru",
  "password": "secret123"
}
```

### Success Response

```json
{
  "userId": 1,
  "username": "haru",
  "displayName": "하루",
  "visibility": "PUBLIC",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

## GET `/api/auth/me`

현재 로그인한 회원 정보를 반환한다.

### Success Response

```json
{
  "userId": 1,
  "username": "haru",
  "displayName": "하루",
  "visibility": "PUBLIC"
}
```

## DELETE `/api/auth/me`

현재 로그인한 회원을 soft delete 처리한다.

### Success Response

- `204 No Content`

## 5. Profile & Follow APIs

## GET `/api/profiles/me`

내 프로필 요약을 반환한다.

### Success Response

```json
{
  "userId": 1,
  "username": "haru",
  "displayName": "하루",
  "visibility": "PRIVATE",
  "me": true,
  "following": false
}
```

## PATCH `/api/profiles/me/visibility`

내 계정 공개성을 바꾼다.

### Request Body

```json
{
  "visibility": "PRIVATE"
}
```

## GET `/api/profiles/me/auto-reply`

내 자동응답 설정과 마지막 활동 시각을 반환한다.

### Success Response

```json
{
  "mode": "AWAY",
  "lastActivityAt": "2026-03-28T00:00:00Z"
}
```

`mode` 값
- `ALWAYS`
- `AWAY`
- `OFF`

## PATCH `/api/profiles/me/auto-reply`

내 자동응답 설정을 변경한다.

### Request Body

```json
{
  "mode": "ALWAYS"
}
```

### Success Response

```json
{
  "mode": "ALWAYS",
  "lastActivityAt": "2026-03-28T00:00:00Z"
}
```

### Success Response

```json
{
  "userId": 1,
  "username": "haru",
  "displayName": "하루",
  "visibility": "PRIVATE",
  "me": true,
  "following": false
}
```

## GET `/api/profiles/{profileUserId}`

다른 사용자의 프로필 요약을 반환한다.

### Success Response

```json
{
  "userId": 2,
  "username": "miso",
  "displayName": "미소",
  "visibility": "PUBLIC",
  "me": false,
  "following": true
}
```

## GET `/api/follows`

내가 현재 팔로우 중인 사용자 목록을 반환한다.

### Success Response

```json
[
  {
    "userId": 2,
    "username": "miso",
    "displayName": "미소",
    "visibility": "PUBLIC",
    "following": true
  }
]
```

## GET `/api/follows/users/search`

SNS 검색 결과를 반환한다.

Query Parameter
- `query`

### Success Response

```json
[
  {
    "userId": 2,
    "username": "miso",
    "displayName": "미소",
    "visibility": "PUBLIC",
    "following": false
  }
]
```

검색 규칙
- 빈 검색어면 빈 배열을 반환한다.
- 공개 계정은 검색 결과에 노출된다.
- 비공개 계정은 이미 팔로우 중인 경우만 검색 결과에 남는다.

## POST `/api/follows/{targetUserId}`

지정한 사용자를 팔로우한다.

### Success Response

```json
{
  "userId": 2,
  "username": "miso",
  "displayName": "미소",
  "visibility": "PUBLIC",
  "following": true
}
```

규칙
- 공개 계정만 신규 팔로우 가능하다.
- 비공개 계정은 신규 팔로우가 불가능하다.

## DELETE `/api/follows/{targetUserId}`

지정한 사용자를 언팔로우한다.

### Success Response

- `204 No Content`

## 6. Post APIs

## POST `/api/posts`

게시물을 생성한다.

### Request Body

```json
{
  "content": "오늘의 기록"
}
```

### Success Response

```json
{
  "postId": 11,
  "ownerUserId": 1,
  "ownerUsername": "haru",
  "ownerDisplayName": "하루",
  "ownerVisibility": "PUBLIC",
  "content": "오늘의 기록",
  "createdAt": "2026-03-28T00:00:00Z"
}
```

## GET `/api/posts/feed`

현재 사용자 기준으로 볼 수 있는 피드 게시물을 반환한다.

### Success Response

```json
[
  {
    "postId": 21,
    "ownerUserId": 2,
    "ownerUsername": "miso",
    "ownerDisplayName": "미소",
    "ownerVisibility": "PUBLIC",
    "content": "피드 게시물",
    "createdAt": "2026-03-28T00:00:00Z"
  }
]
```

피드 규칙
- 공개 계정 게시물은 조회 가능하다.
- 비공개 계정 게시물은 이미 팔로우 중인 경우만 조회 가능하다.

## GET `/api/profiles/me/posts`

내 게시물 목록을 반환한다.

## GET `/api/profiles/{profileUserId}/posts`

특정 프로필의 게시물 목록을 반환한다.

프로필 게시물 규칙
- 공개 계정이면 누구나 조회 가능
- 비공개 계정이면 이미 팔로우 중인 사용자만 조회 가능

## 7. Human DM APIs

## POST `/api/dms/threads`

사람 간 1:1 DM 스레드를 시작한다. 기존 스레드가 있으면 그 스레드를 반환한다.

### Request Body

```json
{
  "targetUserId": 2
}
```

### Success Response

```json
{
  "threadId": 10,
  "otherParticipant": {
    "userId": 2,
    "username": "miso",
    "displayName": "미소",
    "visibility": "PUBLIC"
  },
  "createdAt": "2026-03-28T00:00:00Z",
  "messages": [],
  "hiddenBundleMessageIds": []
}
```

규칙
- 자기 자신과 DM을 시작할 수 없다.
- 공개 계정은 누구나 DM 가능하다.
- 비공개 계정은 이미 팔로우 중인 사용자만 DM 가능하다.

## GET `/api/dms/threads`

내 DM 스레드 목록을 반환한다.

### Success Response

```json
[
  {
    "threadId": 10,
    "otherParticipant": {
      "userId": 2,
      "username": "miso",
      "displayName": "미소",
      "visibility": "PUBLIC"
    },
    "createdAt": "2026-03-28T00:00:00Z",
    "latestMessagePreview": "최근 메시지",
    "latestMessageCreatedAt": "2026-03-28T00:01:00Z"
  }
]
```

## GET `/api/dms/threads/{threadId}`

특정 DM 스레드 전체를 반환한다.

### Success Response

```json
{
  "threadId": 10,
  "otherParticipant": {
    "userId": 2,
    "username": "miso",
    "displayName": "미소",
    "visibility": "PUBLIC"
  },
  "createdAt": "2026-03-28T00:00:00Z",
  "messages": [
    {
      "messageId": 30,
      "senderUserId": 1,
      "senderDisplayName": "하루",
      "aiGenerated": false,
      "bundleRootMessageId": null,
      "content": "안녕!",
      "createdAt": "2026-03-28T00:01:00Z"
    }
  ],
  "hiddenBundleMessageIds": [30]
}
```

## POST `/api/dms/threads/{threadId}/messages`

사람 메시지를 DM 스레드에 추가한다.

### Request Body

```json
{
  "content": "안녕!"
}
```

### Success Response

```json
{
  "messageId": 30,
  "senderUserId": 1,
  "senderDisplayName": "하루",
  "aiGenerated": false,
  "bundleRootMessageId": null,
  "content": "안녕!",
  "createdAt": "2026-03-28T00:01:00Z"
}
```

현재 상태
- 사람이 보낸 DM은 서버가 상대 사용자의 자동응답 설정을 평가한 뒤, 필요하면 같은 스레드에 AI 프록시 응답을 추가 저장한다.
- 자동응답 조건은 `ALWAYS` 또는 `AWAY(마지막 활동 시각 기준 3분 초과)`다.
- 상대 사용자의 대표 클론이 없으면 자동응답은 생략된다.
- AI가 생성한 메시지는 다시 자동응답을 유발하지 않는다.

`GET /api/dms/threads/{threadId}` 메시지 응답 예시

```json
{
  "messageId": 30,
  "senderUserId": 2,
  "senderDisplayName": "미소",
  "aiGenerated": true,
  "bundleRootMessageId": 29,
  "content": "지금은 잠깐 자리를 비웠어요.",
  "createdAt": "2026-03-28T00:01:10Z"
}
```

메시지 필드 의미
- `aiGenerated`
  - `true`면 사용자 대리 AI가 보낸 메시지다.
- `bundleRootMessageId`
  - AI 응답 묶음에 속하지 않은 일반 메시지는 `null`
  - AI 응답을 유발한 사람 메시지는 자기 자신의 `messageId`
  - 해당 AI 응답은 그 사람 메시지의 `messageId`
- `hiddenBundleMessageIds`
  - 현재 로그인한 사용자 기준으로 숨겨 둔 AI 응답 묶음 루트 메시지 id 목록이다.

## POST `/api/dms/threads/{threadId}/bundles/{bundleRootMessageId}/hide`

현재 사용자 기준으로 `유발 메시지 + AI 응답` 묶음을 숨긴다.

### Success Response

```json
{
  "bundleRootMessageId": 30,
  "hidden": true
}
```

규칙
- 숨김은 메시지 삭제가 아니라 개인 UI 가시성 상태 저장이다.
- 숨길 수 있는 대상은 AI 응답을 실제로 유발한 사람 메시지뿐이다.
- 상대방 화면에는 영향을 주지 않는다.

## DELETE `/api/dms/threads/{threadId}/bundles/{bundleRootMessageId}/hide`

현재 사용자 기준으로 숨긴 AI 응답 묶음을 다시 표시한다.

### Success Response

```json
{
  "bundleRootMessageId": 30,
  "hidden": false
}
```

## POST `/api/dms/messages/{messageId}/tts`

지정한 DM 텍스트 메시지를 발화자의 대표 보이스로 합성한다.

### Success Response

```json
{
  "messageId": 30,
  "voiceId": "voice-demo",
  "audioMimeType": "audio/wav",
  "audioBase64": "UklGRg=="
}
```

규칙
- DM 참여자만 해당 메시지 음성에 접근할 수 있다.
- 발화자에게 대표 보이스가 없으면 `404 Not Found`가 반환될 수 있다.
- 이 API는 텍스트 DM 청취용이며, 메시지 원문 자체는 수정하지 않는다.

## 8. Clone APIs For Profile Workspace

설명
- 이 API들은 현재 `Profile` 하위 작업공간용이다.
- 메인 SNS/DM 탭의 주 흐름이 아니라 보조 설정/체험 기능이다.
- `scope=mine`은 현재 사용자 최신 클론 1개만 반환한다.

## POST `/api/system-prompt`

설문 응답으로 내 대표 클론을 생성하거나 갱신한다.

### Request Body

```json
{
  "answers": [
    {
      "question": "평소 말투는 어떤 편인가요?",
      "answer": "차분하고 정리해서 말함"
    }
  ]
}
```

### Success Response

```json
{
  "promptGenerationLogId": 12,
  "alias": "차분한 조력자",
  "shortDescription": "차분하고 구조적인 설명을 선호하는 클론",
  "systemPrompt": "사용자와 대화할 때는 차분하고 사려 깊은 톤을 유지하세요..."
}
```

핵심 규칙
- 새 레코드를 무한히 추가하는 것이 아니라 내 최신 클론을 갱신하는 방식으로 운용된다.
- 갱신 시 대표 클론의 공개 여부는 기본적으로 `false`로 돌아간다.

## GET `/api/clones`

Query Parameter
- `scope=mine|friend|public`

### Success Response

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

규칙
- `scope=mine`은 내 대표 클론 1개만 반환한다.
- `scope=friend`와 `scope=public`은 기존 레거시 자산 탐색 흐름을 위해 유지된다.

## PATCH `/api/clones/{cloneId}/visibility`

내 대표 클론의 공개 여부를 바꾼다.

### Request Body

```json
{
  "isPublic": true
}
```

## 9. Voice APIs For Profile Workspace

설명
- 이 API들도 현재 `Profile` 하위 작업공간용이다.
- `scope=mine`은 현재 사용자 최신 보이스 1개만 반환한다.

## POST `/api/voices`

보이스 샘플을 등록하거나 내 대표 보이스를 갱신한다.

Content Type
- Request: `multipart/form-data`

Form field
- `sample`
- `alias` (optional)

### Success Response

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

핵심 규칙
- 새 레코드를 무한히 추가하는 것이 아니라 내 최신 보이스를 갱신하는 방식으로 운용된다.
- 갱신 시 대표 보이스의 공개 여부는 기본적으로 `false`로 돌아간다.

## GET `/api/voices`

Query Parameter
- `scope=mine|friend|public`

### Success Response

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

규칙
- `scope=mine`은 내 대표 보이스 1개만 반환한다.
- 현재 서버의 `DASHSCOPE_TTS_MODEL`과 호환되는 보이스만 반환한다.

## PATCH `/api/voices/{registeredVoiceId}/visibility`

내 대표 보이스의 공개 여부를 바꾼다.

### Request Body

```json
{
  "isPublic": true
}
```

## 10. Legacy Chat / Debate APIs

설명
- `/api/chat/**`, `/api/debates/**`, `/api/friends/**`는 아직 프로필 하위 워크스페이스에서 사용된다.
- 메인 SNS 제품 흐름은 아니지만 현재 코드베이스에서 동작 중이므로 삭제 전까지 유지 문서화한다.

핵심 요약
- `/api/chat/messages`, `/api/chat/messages/stream`
  - 클론 기반 1:1 대화
- `/api/chat/conversations`, `/api/chat/conversations/{conversationId}`
  - 클론 대화 기록
- `/api/debates/stream`, `/api/debates/{debateSessionId}/next/stream`
  - 두 클론 간 논쟁 스트림
- `/api/debates`, `/api/debates/{debateSessionId}`
  - 논쟁 기록
- `/api/friends/**`
  - 기존 친구 요청/수락/해제 흐름

주의
- 새 SNS 정책은 `follow` 기반이지만, 레거시 워크스페이스 내부 자산 공유는 아직 `friend` API를 사용한다.

## 11. Common Error Shapes

### `401 Unauthorized`

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

### `403 Forbidden`

대표 사례
- 비공개 계정에 신규 DM 시도
- 비공개 계정을 신규 팔로우 시도
- DM 참여자가 아닌 스레드 접근 시도

### `404 Not Found`

대표 사례
- 존재하지 않는 리소스
- 읽기 권한이 없는 다른 사용자 자산

## 12. Persistence Notes

현재 주요 저장 테이블
- `users`
- `follows`
- `posts`
- `dm_threads`
- `dm_messages`
- `prompt_generation_logs`
- `registered_voices`
- `chat_conversations`
- `chat_messages`
- `debate_sessions`
- `debate_turns`
- `generated_audio_assets`
- `friend_requests`

제품 전환 관점 메모
- SNS 메인 도메인은 `follows`, `posts`, `dm_*`
- AI 프로필 자산은 `prompt_generation_logs`, `registered_voices`
- 레거시 실시간 체험 기능은 `chat_*`, `debate_*`

## 13. Environment

필수 또는 권장 환경 변수는 [backend/.env.example](./.env.example)에 정리되어 있다.

주요 항목
- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `APP_AUTH_JWT_SECRET`
- `DASHSCOPE_API_KEY`
- `DASHSCOPE_TTS_MODEL`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

참고
- `APP_BOOTSTRAP_DEFAULT_ACCOUNT_ENABLED=true`면 기본 계정, 기본 클론, 기본 음성을 시드할 수 있다.
- 기본 클론/보이스 시드는 현재 프로필 워크스페이스의 레거시 체험 흐름과 연결된다.
