# SSARVIS

SSARVIS는 설문 응답을 바탕으로 사용자를 모사하는 클론 시스템 프롬프트를 만들고, 그 클론과 대화하거나 두 클론을 서로 논쟁시키는 풀스택 서비스입니다.  
채팅과 논쟁에는 사용자 등록 음성을 붙일 수 있고, TTS는 프론트에서 실시간 PCM 스트림으로 재생되며 서버에서는 ffmpeg 인코딩 후 S3에도 저장할 수 있습니다.

## 주요 기능

- 설문 응답으로 클론 생성
  - OpenAI를 사용해 사용자 모사용 `systemPrompt` 생성
  - 생성된 `systemPrompt`로 `alias`, `shortDescription`를 단계적으로 생성
- 클론과 1:1 대화
  - 최근 `OPENAI_CHAT_HISTORY_TURNS`턴만 컨텍스트에 포함
  - 선택적으로 DashScope 등록 음성으로 TTS 생성
  - 스트리밍 응답에서 PCM 청크를 즉시 재생
- 클론 간 논쟁
  - 두 클론, 두 목소리, 주제를 선택해 턴 단위로 진행
  - 브라우저가 다음 턴을 요청하는 구조
  - 별도 stop API 없이, 클라이언트가 다음 턴 요청을 멈추면 종료
- 음성 등록
  - 사용자가 음성 샘플을 업로드해 DashScope 커스텀 보이스 등록
  - 사용자 별칭(`displayName`)과 내부용 `preferredName` 분리 관리
- 음성 입력
  - 프론트에서 Web Speech API + Web Audio API silence 감지로 받아쓰기 지원
- 오디오 저장
  - 프론트 응답은 기존처럼 Base64 오디오를 유지
  - 서버는 별도로 생성된 오디오를 ffmpeg로 인코딩하고 S3 및 DB에 저장 가능

## 구성

- 프론트엔드: React 19, TypeScript, Vite
- 백엔드: Spring Boot, JPA, MySQL
- LLM: OpenAI Chat Completions API
- TTS / 보이스 등록: DashScope
- 오디오 후처리: ffmpeg
- 오브젝트 스토리지: S3 호환 스토리지 지원

프로젝트 구조

- [backend](./backend)
- [frontend](./frontend)
- 참고 자료: [_ref](./_ref)

## 현재 동작 개요

### 클론 생성

1. 프론트가 설문 응답을 `/api/system-prompt`로 전송
2. 백엔드가 OpenAI에 3단계로 요청
   - `systemPrompt`
   - `alias`
   - `shortDescription`
3. 결과를 `prompt_generation_logs`에 저장
4. 프론트에서 새 클론 카드로 즉시 반영

### 채팅

OpenAI 컨텍스트는 아래 순서로 구성됩니다.

1. 클론 시스템 프롬프트
2. 최근 `OPENAI_CHAT_HISTORY_TURNS`턴의 대화 히스토리
3. 채팅 생성 지시문
4. 현재 사용자 메시지

### 논쟁

OpenAI 컨텍스트는 현재 발화하는 클론 기준으로 아래 순서로 구성됩니다.

1. 현재 클론의 시스템 프롬프트
2. 최근 `OPENAI_CHAT_HISTORY_TURNS`턴의 논쟁 히스토리
   - 현재 클론의 과거 발언은 `user`
   - 상대 클론의 과거 발언은 `assistant`
3. 현재 주제를 포함한 논쟁 생성 지시문

논쟁은 고정 찬반 역할을 미리 배정하지 않고, 각 클론이 자신의 성격 프롬프트와 앞선 대화 흐름에 따라 자연스럽게 입장을 전개합니다.

### TTS

- DashScope realtime websocket으로 PCM 오디오 수신
- 프론트는 PCM 청크를 바로 재생
- 긴 텍스트는 UTF-8 600바이트 제한을 넘기지 않도록 분할
  - 가능하면 600바이트 이전 마지막 `.` 기준으로 자름
- 전체 오디오는 WAV로 합쳐 응답에도 사용하고, 서버에서는 별도 인코딩/저장 가능

## 요구 사항

- Java 17
- Node.js 20 이상 권장
- MySQL
- ffmpeg
- OpenAI API 키
- DashScope API 키
- 선택 사항: S3 또는 MinIO 같은 S3 호환 스토리지

## 환경 변수

실행에 필요한 환경 변수 예시는 [backend/.env.example](./backend/.env.example)에 있습니다.

핵심 변수

- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `OPENAI_BASE_URL`
- `OPENAI_CHAT_HISTORY_TURNS`
- `DASHSCOPE_API_KEY`
- `DASHSCOPE_BASE_URL`
- `DASHSCOPE_REALTIME_URL`
- `DASHSCOPE_TTS_MODEL`
- `FFMPEG_PATH`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

선택 변수

- `S3_ENABLED`
- `S3_BUCKET`
- `S3_REGION`
- `S3_ENDPOINT`
- `S3_ACCESS_KEY`
- `S3_SECRET_KEY`
- `S3_KEY_PREFIX`
- `S3_PATH_STYLE_ACCESS`
- `S3_PUBLIC_BASE_URL`

참고

- `backend/.env`는 `bootRun`과 `integrationTest`에서 자동으로 읽힙니다.
- 프론트는 기본적으로 `http://localhost:8080`의 백엔드를 대상으로 동작합니다.

## 로컬 실행

### 1. 백엔드 설정

`backend/.env`를 준비합니다.

예시 흐름

1. [backend/.env.example](./backend/.env.example)를 참고해 `backend/.env` 생성
2. MySQL 데이터베이스 준비
3. OpenAI / DashScope 키 입력
4. 필요 시 S3 설정 입력

### 2. 백엔드 실행

작업 디렉터리: [backend](./backend)

```powershell
.\gradlew bootRun
```

기본 포트

- Backend: `http://localhost:8080`

### 3. 프론트 실행

작업 디렉터리: [frontend](./frontend)

```powershell
npm install
npm run dev
```

기본 포트

- Frontend: `http://localhost:5173`

## 테스트

백엔드 단위 테스트

```powershell
cd backend
.\gradlew test
```

백엔드 통합 테스트

```powershell
cd backend
.\gradlew integrationTest --rerun-tasks
```

프론트 빌드 검증

```powershell
cd frontend
npm run build
```

통합 테스트 특징

- 실제 OpenAI 호출
- 실제 DashScope 호출
- 실제 MySQL 사용
- `backend/src/test/resources/sample/haru.wav` 샘플 파일로 음성 등록 테스트 수행
- 외부 HTTP 요청, realtime 연결, realtime 이벤트 대기는 20초 무응답 시 실패

## 문서

- 백엔드 API 문서: [API_SPEC.md](./backend/API_SPEC.md)
- 프론트 구현 가이드: [GUIDE.md](./frontend/GUIDE.md)

## 주요 저장 테이블

- `prompt_generation_logs`
  - 생성된 클론의 `alias`, `shortDescription`, `system_prompt`, 설문 응답 저장
- `registered_voices`
  - 등록 음성 정보 저장
- `chat_conversations`
  - 채팅 세션 저장
- `chat_messages`
  - 사용자/클론 메시지 저장
- `debate_sessions`
  - 논쟁 세션 저장
- `debate_turns`
  - 논쟁 발언 저장
- `generated_audio_assets`
  - 인코딩 및 업로드된 오디오 메타데이터 저장

## 프론트 화면 구조

- 클론 탭
  - 새 클론 만들기
  - 기존 클론 목록
  - 클론 액션 모달
- 라이브 탭
  - 클론과 채팅
  - 음성 입력 버튼
  - 클론 간 논쟁 진행 상태

## 현재 알려진 특성

- Web Speech API는 브라우저 구현 차이가 커서 Chrome 계열에서 가장 안정적입니다.
- 음성 인식은 silence 감지 기반으로 자동 종료되므로, 주변 소음이 크면 종료 시점이 달라질 수 있습니다.
- 오래전에 다른 DashScope 모델로 등록한 음성은 현재 TTS 모델과 호환되지 않을 수 있으며, 이 경우 다시 등록해야 합니다.

## 라이선스

현재 저장소에는 별도 라이선스 파일이 없습니다.
