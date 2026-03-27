# Frontend Guide

이 문서는 현재 프론트엔드 구현을 빠르게 이해하고 유지보수할 수 있도록 핵심 흐름과 주의점을 정리한 가이드다.

- 인증 및 사용자 전용 데이터 로딩
- 친구 및 공유 자산(`내 것 / 친구 / 공개`) 사용 흐름
- 실시간 PCM 재생
- 음성 입력(Web Speech API)
- 설문 표시 및 처리
- 테스트 관점에서 주의할 포인트

기준 경로
- 프로젝트 루트: `frontend`
- 앱 루트: [App.tsx](./src/App.tsx)
- 메인 화면: [CloneStudioPage.tsx](./src/pages/CloneStudioPage.tsx)
- 화면 상태 오케스트레이션: [useCloneStudio.ts](./src/features/clone-studio/hooks/useCloneStudio.ts)
- 친구 워크스페이스 훅: [useFriendWorkspace.ts](./src/features/clone-studio/hooks/useFriendWorkspace.ts)

## 1. 인증 및 사용자 전용 데이터 로딩

### 관련 파일

- 앱 루트: [App.tsx](./src/App.tsx)
- 인증 화면: [AuthPage.tsx](./src/features/auth/AuthPage.tsx)
- API 유틸: [api.ts](./src/features/clone-studio/api.ts)
- 메인 화면: [CloneStudioPage.tsx](./src/pages/CloneStudioPage.tsx)
- 사용자 기준 상태 로딩: [useCloneStudio.ts](./src/features/clone-studio/hooks/useCloneStudio.ts)
- 친구 상태 및 액션: [useFriendWorkspace.ts](./src/features/clone-studio/hooks/useFriendWorkspace.ts)
- 저장된 채팅/논쟁 기록 열기: [LiveSessionPanel.tsx](./src/features/clone-studio/components/LiveSessionPanel.tsx)

### 현재 구조

인증 상태는 `App.tsx`가 소유하고, 클론/음성/채팅/논쟁 상태는 `CloneStudioPage -> useCloneStudio`가 소유한다.

역할 분리
- `App.tsx`
  - 로그인/회원가입 제출
  - 저장된 JWT로 `/api/auth/me` 호출
  - 인증 만료 이벤트 처리
  - 로그아웃/회원 탈퇴 후 로그인 화면 복귀
- `useCloneStudio`
  - 현재 로그인 회원 기준으로 클론/음성 목록 재로딩
  - `mine` / `friend` / `public` 자산을 분리 로딩하고 선택용 목록으로 다시 조합
  - 현재 로그인 회원 기준으로 채팅/논쟁 기록 목록 재로딩
  - 사용자 전환 시 라이브 세션/모달/임시 입력 상태 정리
- `useFriendWorkspace`
  - 친구 목록, 받은 요청, 보낸 요청 로딩
  - 사용자 검색, 요청 전송/수락/거절/취소/해제
  - 친구 관계 변경 후 자산 목록 재동기화 콜백 호출

### 현재 동작

1. 앱 시작 시 `App.tsx`가 로컬 스토리지의 access token으로 `/api/auth/me` 호출
2. 성공하면 `currentUser`를 채우고 `CloneStudioPage`를 렌더링
3. 실패하면 토큰을 제거하고 인증 화면으로 돌아감
4. 이후 보호 API는 `apiFetch(...)`를 통해 항상 `Authorization: Bearer ...`를 자동 부착
5. 어떤 보호 API에서든 `401`이 오면 `authExpiredEventName` 이벤트를 발행하고, 앱 루트가 이를 받아 자동 로그아웃
6. 사용자 정보가 바뀌면 `useCloneStudio(currentUser)`가 기존 세션을 정리하고 해당 사용자 기준의 내 자산, 친구 자산, 공개 자산을 다시 불러옴
7. Live 탭 왼쪽 기록 목록에서 이전 채팅/논쟁을 다시 열 수 있음
8. Friends 탭을 열면 친구 목록, 받은 요청, 보낸 요청을 별도 로딩함
9. Friends 탭 관련 액션은 `useFriendWorkspace`가 전담하고, 친구 수락/해제 후에는 자산 목록을 다시 동기화함

실제 세션 복구 흐름 예시

```ts
const response = await apiFetch(`${apiBaseUrl}/api/auth/me`)
if (!response.ok) {
  clearStoredAccessToken()
  setCurrentUser(null)
  return
}

const me: CurrentUser = await response.json()
setCurrentUser(me)
```

### 수정 시 주의점

- 인증 상태와 클론 스튜디오 상태를 한 훅에 합치지 않는다. 지금 구조는 “계정 레벨”과 “작업 화면 레벨”이 분리돼 있어서 회귀를 줄인다.
- 보호 API 호출은 직접 `fetch`보다 `apiFetch(...)`를 우선 사용한다.
- JSON 응답 보호 API는 `fetchJsonOrThrow(...)`를 우선 사용해 에러 처리 형식을 맞춘다.
- 회원 탈퇴는 서버 세션을 지우는 개념이 아니라 soft delete + 로컬 토큰 제거 흐름이다.
- 사용자 전환 시 이전 사용자의 채팅/논쟁 상태가 남지 않도록 `useCloneStudio`의 `currentUser.userId` 의존 효과를 유지한다.
- 기록 상세를 열 때도 직접 `fetch`보다 `apiFetch(...)`와 훅 내부 로딩 함수를 통해 일관되게 처리한다.

## 2. 실시간 PCM 재생

### 관련 파일

- 재생기: [PcmStreamPlayer.ts](./src/utils/PcmStreamPlayer.ts)
- 채팅 스트림 처리: [useCloneStudio.ts](./src/features/clone-studio/hooks/useCloneStudio.ts)
- 라이브 세션 UI: [LiveSessionPanel.tsx](./src/features/clone-studio/components/LiveSessionPanel.tsx)

### 현재 동작

백엔드의 스트리밍 엔드포인트는 NDJSON으로 아래 이벤트를 내려준다.

- `message` 또는 `turn`: 텍스트 본문
- `audio_chunk`: PCM 청크(Base64)
- `done`: 오디오 종료
- `error`: 스트림 오류

프론트는 `audio_chunk`를 받을 때마다 `PcmStreamPlayer`에 전달하고, `done`이 오면 재생을 마무리한 뒤 전체 WAV URL을 만들어 `<audio>` 컨트롤에도 연결한다.

### 구현 흐름

1. 스트림 요청 시작
   - 채팅: `streamChatMessage(...)`
   - 논쟁: `streamDebateTurn(...)`
2. `readNdjsonStream(...)`가 이벤트를 순서대로 읽는다.
3. `audio_chunk` 이벤트마다 `player.configure(sampleRate, channels)` 후 `player.appendBase64Chunk(...)` 호출
4. 내부적으로 `PcmStreamPlayer`는:
   - Base64 PCM을 바이트 배열로 복원
   - 16-bit 샘플 정렬이 깨지지 않도록 홀수 바이트를 보정
   - `AudioContext`용 `AudioBuffer`를 생성
   - 이전 청크 뒤에 이어지도록 `nextStartTime` 기준으로 예약 재생
5. `done` 이벤트가 오면 `player.finish()`로 마지막 source 종료를 기다린 뒤 `player.buildWavUrl()`로 전체 WAV blob URL 생성

실제 스트림 처리 예시

```ts
await readNdjsonStream(response, async (streamEvent) => {
  if (streamEvent.type === 'audio_chunk') {
    player.configure(streamEvent.sampleRate, streamEvent.channels)
    await player.appendBase64Chunk(streamEvent.chunkBase64)
    return
  }

  if (streamEvent.type === 'done') {
    await player.finish()
    const wavUrl = player.buildWavUrl()
    // 마지막 메시지 또는 턴에 wavUrl 연결
  }
})
```

### `PcmStreamPlayer`의 핵심 책임

- 실시간 재생
- 청크 누적 저장
- PCM -> `AudioBuffer` 변환
- 최종 WAV blob URL 생성

중요 포인트
- 서버가 보내는 포맷은 `pcm_s16le` 기준이다.
- `sampleRate`, `channels`는 서버 이벤트 값을 그대로 신뢰한다.
- 청크 경계에서 1바이트가 남을 수 있어 `trailingByte`로 이어붙인다.
- `dispose()`는 살아 있는 `AudioBufferSourceNode`를 직접 `stop()`하고 `AudioContext`를 닫는다.
- `buildWavUrl()`은 누적 PCM으로 새 blob URL을 만들고, `dispose()`는 마지막 object URL도 revoke한다.

실제 PCM -> `AudioBuffer` 변환 예시

```ts
const totalSamples = Math.floor(bytes.byteLength / 2)
const frameCount = Math.floor(totalSamples / this.channels)
const audioBuffer = this.audioContext.createBuffer(this.channels, frameCount, this.sampleRate)
const view = new DataView(bytes.buffer, bytes.byteOffset, frameCount * this.channels * 2)

for (let channel = 0; channel < this.channels; channel += 1) {
  const channelData = audioBuffer.getChannelData(channel)
  for (let frame = 0; frame < frameCount; frame += 1) {
    const sampleIndex = (frame * this.channels + channel) * 2
    channelData[frame] = view.getInt16(sampleIndex, true) / 32768
  }
}
```

### 수정 시 주의점

- 재생 품질 문제가 생기면 먼저 서버가 보내는 `sampleRate`, `channels`, `audioFormat`을 확인한다.
- 긴 음성이 끝까지 재생되지 않으면 `done` 이벤트 수신 시점과 `finish()` 대기 로직을 확인한다.
- `<audio>` 재생이 `blob: ... ERR_FILE_NOT_FOUND`를 내면 URL revoke 타이밍 문제일 가능성이 높다.
- 페이지를 실제로 떠날 때는 `pagehide`에서 스트림 abort, player dispose, 렌더된 `<audio>` pause까지 함께 정리한다.

## 2-1. 친구 및 공유 자산(`내 것 / 친구 / 공개`) 사용 흐름

### 관련 파일

- 상태 조합 및 visibility 토글: [useCloneStudio.ts](./src/features/clone-studio/hooks/useCloneStudio.ts)
- 클론 목록 UI: [CloneGridSection.tsx](./src/features/clone-studio/components/CloneGridSection.tsx)
- 클론 액션 모달: [CloneActionsModal.tsx](./src/features/clone-studio/components/modals/CloneActionsModal.tsx)
- 음성 선택 모달: [VoicePickerModal.tsx](./src/features/clone-studio/components/modals/VoicePickerModal.tsx)
- 논쟁 설정 모달: [DebateSetupModal.tsx](./src/features/clone-studio/components/modals/DebateSetupModal.tsx)
- 친구 탭 UI: [FriendPanel.tsx](./src/features/clone-studio/components/FriendPanel.tsx)
- 탭 전환 UI: [StudioTabs.tsx](./src/features/clone-studio/components/StudioTabs.tsx)
- 공통 자산 메타 UI: [AssetMeta.tsx](./src/features/clone-studio/components/AssetMeta.tsx)

### 현재 구조

클론과 음성은 각각 세 목록으로 관리한다.

- `mineClones` / `friendClones` / `publicClones`
- `mineVoices` / `friendVoices` / `publicVoices`

선택용으로는 훅 내부에서 중복 제거 후 `clones`, `voices`를 다시 만든다.  
덕분에 화면은 `내 것 / 친구 / 공개`로 나눠 보여주고, 채팅/논쟁 설정에서는 “현재 사용 가능한 전체 자산”을 일관되게 참조할 수 있다.

### 현재 동작

1. `loadClones()`는 `/api/clones?scope=mine`, `/api/clones?scope=friend`, `/api/clones?scope=public`를 함께 호출
2. `loadVoices()`는 `/api/voices?scope=mine`, `/api/voices?scope=friend`, `/api/voices?scope=public`를 함께 호출
3. 클론 화면은 `내 클론`, `친구 클론`, `공개 클론` 섹션으로 분리 렌더링
4. 음성 선택 모달은 `내 음성`, `친구 음성`, `공개 음성` 그룹으로 분리 렌더링
5. 친구/공개 자산 카드와 옵션에는 작성자 `displayName`을 표시해 같은 이름 자산을 구분한다
6. `공개/비공개 전환` 버튼은 내 자산에만 노출되고, 친구 자산에는 절대 보이면 안 된다
7. 친구 자산은 비공개라도 사용 가능하지만, 관리 권한은 여전히 소유자에게만 있다
8. 친구나 공개 자산을 사용해 시작한 채팅/논쟁도 결과 기록 자체는 현재 로그인 사용자 소유로 저장
9. 자산 배지/작성자/토글 버튼 행은 `AssetMeta`로 공통 렌더링한다

### 수정 시 주의점

- 친구/공개 자산은 “사용 가능”이지 “관리 가능”이 아니다. 소유자가 아닌 자산에는 visibility 버튼이 보이면 안 된다.
- visibility 토글 후에는 목록뿐 아니라 현재 열려 있는 모달의 `selectedClone`도 같이 갱신해야 버튼 문구와 배지가 즉시 맞는다.
- 선택용 `clones`, `voices`는 `mine + friend + public` 합본이라, UI 문구와 소유권 판단은 반드시 원본 분리 목록(`mineClones`, `friendClones`, `mineVoices`, `friendVoices`)을 기준으로 해야 한다.
- 논쟁 설정 select 옵션은 공개 여부, 작성자, 친구 자산 여부를 함께 보여줘야 같은 이름의 자산을 구분하기 쉽다.
- 친구 또는 공개 자산 사용 중 `403/404`가 오면 “자산이 비공개로 바뀌었거나 친구 관계/접근 권한이 사라졌을 수 있다”는 식의 친화적 메시지로 바꾸는 편이 UX가 좋다.

## 2-2. 친구 탭과 관계 관리

### 관련 파일

- 상태 로딩 및 액션: [useCloneStudio.ts](./src/features/clone-studio/hooks/useCloneStudio.ts)
- 친구 패널 UI: [FriendPanel.tsx](./src/features/clone-studio/components/FriendPanel.tsx)
- 탭 UI: [StudioTabs.tsx](./src/features/clone-studio/components/StudioTabs.tsx)

### 현재 구조

친구 기능은 별도 탭으로 분리되어 있고, 실제 데이터와 액션은 `useFriendWorkspace`가 들고 있다.

- `friends`
- `receivedFriendRequests`
- `sentFriendRequests`
- `friendSearchQuery`
- `friendSearchResults`
- `friendActionKey`

친구 탭 진입 시에만 친구 관련 API를 로딩하므로, 클론/라이브 탭에서는 불필요한 네트워크 요청을 줄일 수 있다.  
또 친구 수락/해제처럼 자산 접근 범위가 바뀌는 액션은 완료 후 자산 목록 재동기화 콜백을 호출한다.

### 현재 동작

1. 사용자가 Friends 탭으로 이동
2. `useFriendWorkspace`의 `useEffect`가 `/api/friends`, `/api/friends/requests/received`, `/api/friends/requests/sent`를 함께 호출
3. 검색 폼 제출 시 `/api/friends/users/search?query=...` 호출
4. 요청 보내기, 수락, 거절, 취소, 친구 해제 후에는 `loadFriendData()`를 다시 호출해 화면을 새로고침
5. 친구가 생기거나 해제되면 친구 탭 데이터와 함께 클론/음성 목록도 다시 반영된다

### 수정 시 주의점

- 친구 액션 버튼은 동시에 여러 번 눌리지 않도록 `friendActionKey` 기반 비활성화를 유지한다.
- 친구 검색 결과에서는 본인, 이미 친구인 사용자, 이미 pending 상태인 관계를 어떻게 표시할지 UI 일관성을 지킨다.
- 친구 해제는 기존 기록을 지우는 기능이 아니므로, Friends 탭 문구와 Live 탭 동작을 혼동하지 않게 한다.
- 친구 관계가 바뀐 뒤 자산 사용이 실패하면, 친구 자산 접근 권한이 사라졌을 가능성까지 고려한 메시지를 유지한다.

## 3. 음성 입력(Web Speech API)

### 관련 파일

- 음성 입력 훅: [useSpeechInput.ts](./src/features/clone-studio/hooks/useSpeechInput.ts)
- 라이브 세션 상태 연결: [useCloneStudio.ts](./src/features/clone-studio/hooks/useCloneStudio.ts)
- 입력 UI: [LiveSessionPanel.tsx](./src/features/clone-studio/components/LiveSessionPanel.tsx)
- 타입 선언: [web-speech.d.ts](./src/types/web-speech.d.ts)

### 왜 별도 훅으로 분리했는가

음성 인식은 채팅 흐름과 직접 관계는 있지만, 아래 문제 때문에 별도 훅으로 분리하는 편이 안전하다.

- 브라우저별 `SpeechRecognition` 편차가 큼
- `onend`가 예고 없이 자주 호출될 수 있음
- 마이크 음량 기반 silence 감지까지 같이 관리해야 함
- 채팅 상태 변경이 음성 인식 세션 종료로 연결되면 쉽게 버그가 남

그래서 현재는 `useCloneStudio`가 직접 `SpeechRecognition` 인스턴스를 다루지 않고, `useSpeechInput`이 아래 계약만 제공한다.

- `supported`
- `listening`
- `error`
- `toggle()`
- `stop()`
- `clearError()`

### 현재 동작

마이크 버튼을 누르면:

1. 현재 입력창 값을 `baseInput`으로 잡는다.
2. Web Speech API 인식을 시작한다.
3. 동시에 Web Audio API analyser로 마이크 입력 RMS를 측정한다.
4. 인식 도중 `final transcript`와 `interim transcript`를 합쳐 입력창 값을 계속 갱신한다.
5. 브라우저가 인식 세션을 중간에 끊어도, 사용자가 아직 말하는 흐름이면 `onend`에서 자동 재시작한다.
6. 실제 말소리가 감지된 뒤 일정 시간 조용해지면 silence 종료로 판단하고 인식을 멈춘다.

시작/중지 진입점 예시

```ts
async function toggle() {
  if (shouldKeepListeningRef.current || listening) {
    await stop()
    return
  }

  await start()
}
```

### 구현 포인트

`useSpeechInput`은 두 계층으로 나뉜다.

- Web Speech API 계층
  - `SpeechRecognition` 생성
  - `continuous = true`
  - `interimResults = true`
  - `onresult`, `onerror`, `onend` 처리
- Web Audio API 계층
  - `getUserMedia(...)`
  - `AudioContext + AnalyserNode`
  - 시간 영역 샘플로 RMS 계산
  - silence threshold 및 silence duration 판정

### 재시작 전략

현재 구현은 인식 세션이 예상보다 일찍 닫히는 브라우저를 고려해, 세션이 끝날 때마다 새 `SpeechRecognition` 인스턴스를 다시 시작하는 방식으로 운용한다.

핵심 변수
- `sessionIdRef`: 오래된 세션 콜백 무시용
- `shouldKeepListeningRef`: 사용자가 계속 말하는 중인지 여부
- `restartTimeoutRef`: 짧은 지연 후 재시작

이 전략 덕분에 `onend`가 자주 와도 사용자가 직접 중지하지 않는 한 인식을 이어가기 쉽다.

실제 `onend` 재시작 예시

```ts
recognition.onend = () => {
  if (recognitionRef.current === recognition) {
    recognitionRef.current = null
  }

  if (sessionId !== sessionIdRef.current) {
    return
  }

  if (shouldKeepListeningRef.current) {
    scheduleRestart(sessionId)
    return
  }

  setListening(false)
  void stopSilenceMonitoring()
}
```

### silence 종료 전략

현재 기준값
- threshold: `0.018`
- duration: `1400ms`

의미
- 실제로 한 번 이상 말소리가 감지된 뒤
- RMS가 threshold 아래로 충분히 오래 떨어지면
- 그 시점을 “말이 끝난 시점”으로 보고 자동 종료

실제 silence 판정 예시

```ts
currentAnalyser.getFloatTimeDomainData(sampleBuffer)
let sumSquares = 0
for (let index = 0; index < sampleBuffer.length; index += 1) {
  const sample = sampleBuffer[index]
  sumSquares += sample * sample
}

const rms = Math.sqrt(sumSquares / sampleBuffer.length)
const now = window.performance.now()

if (rms >= SILENCE_THRESHOLD) {
  heardSpeechRef.current = true
  silenceStartedAtRef.current = null
} else if (heardSpeechRef.current) {
  if (silenceStartedAtRef.current == null) {
    silenceStartedAtRef.current = now
  } else if (now - silenceStartedAtRef.current >= SILENCE_DURATION_MS) {
    shouldKeepListeningRef.current = false
    recognitionRef.current?.stop()
  }
}
```

튜닝 포인트
- 주변 소음이 크면 threshold를 높인다.
- 말을 천천히 끊어 하는 사용자가 많으면 duration을 늘린다.
- 종료가 너무 늦으면 threshold를 낮추기보다 duration을 먼저 줄이는 편이 안전하다.

### 수정 시 주의점

- `SpeechRecognition` 인스턴스를 상태 변경마다 재생성하지 말고, 한 cycle 단위로 관리한다.
- `stop()` 호출 시 `onend`에서 다시 시작되지 않게 `shouldKeepListeningRef`를 먼저 내린다.
- 마이크 스트림을 멈출 때는 `MediaStreamTrack.stop()`과 `AudioContext.close()`를 함께 정리한다.
- 브라우저 지원 여부는 `window.SpeechRecognition ?? window.webkitSpeechRecognition` 기준으로 본다.

## 4. 설문 표시 및 처리

### 관련 파일

- 설문 모달 UI: [CreateCloneModal.tsx](./src/features/clone-studio/components/modals/CreateCloneModal.tsx)
- 설문 상태 및 제출 처리: [useCloneStudio.ts](./src/features/clone-studio/hooks/useCloneStudio.ts)
- 질문 소스: `public/questions.json`

### 현재 구조

설문은 메인 화면에 바로 노출되지 않고, “새 클론 만들기” 모달 안에서 처리된다.

흐름

1. 사용자가 클론 탭에서 “새 클론 만들기” 클릭
2. `CreateCloneModal` 열림
3. 질문 목록은 앱 초기 로드 시 `questions.json`에서 읽어둠
4. 사용자는 각 질문마다 선택지 하나를 고름
5. 모든 질문 응답이 채워지면 제출 버튼 활성화
6. 제출 시 `/api/system-prompt`로 전송
7. 성공하면 새 클론을 목록 앞에 추가

### 상태 구조

`useCloneStudio`가 아래 상태를 가진다.

- `questions: Question[]`
- `answers: Record<number, string>`
- `loadingQuestions`
- `questionError`
- `answeredCount`
- `canCreateClone`
- `creatingClone`
- `createCloneError`

### 질문 로드 방식

앱 마운트 시 `questionAssetPath`를 통해 정적 JSON을 불러온다.

장점
- 백엔드 의존 없이 설문 문항 수정 가능
- 프론트만 빌드해서 질문 실험 가능

주의점
- 질문 순서는 배열 인덱스를 기준으로 처리된다.
- `answers`도 인덱스 기반이라, 질문 순서를 바꾸면 저장 매핑도 함께 바뀐다.

### 응답 처리 방식

`CreateCloneModal`은 질문/선택지 렌더링만 담당하고, 실제 상태 변경은 부모가 담당한다.

핵심 인터페이스
- `answers[index] === choice`로 현재 선택 여부 표시
- `onAnswerChange(index, choice)`로 상위 상태 갱신

이 구조 덕분에 UI 컴포넌트는 비교적 단순하고, 제출 조건 계산은 훅에 모아둘 수 있다.

실제 질문 렌더링 예시

```tsx
{questions.map((question, index) => (
  <fieldset key={question.question} className="question-card">
    <legend>{question.question}</legend>
    <div className="choice-grid">
      {question.choices.map((choice) => (
        <label key={choice}>
          <input
            checked={answers[index] === choice}
            name={`question-${index}`}
            onChange={() => onAnswerChange(index, choice)}
            type="radio"
            value={choice}
          />
          <span>{choice}</span>
        </label>
      ))}
    </div>
  </fieldset>
))}
```

### 제출 페이로드

제출 시 프론트는 다음 형태로 요청한다.

```json
{
  "answers": [
    {
      "question": "질문 원문",
      "answer": "선택한 응답"
    }
  ]
}
```

구성 방식
- 질문 원문은 `questions[index].question`
- 응답은 `answers[index]`

즉, 서버는 프론트가 이미 “질문 + 선택한 답” 쌍으로 정리한 데이터를 받는다.

실제 제출 예시

```ts
const response = await apiFetch(`${apiBaseUrl}/api/system-prompt`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    answers: questions.map((question, index) => ({
      question: question.question,
      answer: answers[index],
    })),
  }),
})
```

### 수정 시 주의점

- 질문을 추가하면 `canCreateClone` 계산은 자동으로 더 엄격해진다.
- 질문 타입을 자유입력형으로 바꾸려면 `CreateCloneModal`과 `answers` 구조를 함께 바꿔야 한다.
- 현재는 단일 선택만 지원하므로, 복수 선택을 넣으려면 `Record<number, string>` 구조부터 수정해야 한다.

## 5. 사용자 전용 라이브 세션 주의점

### 사용자 전환

현재 구현은 `useCloneStudio(currentUser)`가 `currentUser.userId`가 바뀔 때 아래를 모두 초기화한다.

- 클론/음성 목록
- 친구 목록/친구 요청/검색 결과
- 선택 중인 모달 상태
- 채팅 입력 및 메시지
- 논쟁 상태
- 등록 중인 음성 파일과 별칭
- 활성 `AbortController`
- 활성 `PcmStreamPlayer`
- 채팅/논쟁 기록 목록

### 저장된 기록 다시 열기

현재 Live 탭은 새 세션만 처리하지 않고, 서버에 저장된 이전 기록도 함께 보여준다.

- `GET /api/chat/conversations`
- `GET /api/chat/conversations/{id}`
- `GET /api/debates`
- `GET /api/debates/{id}`

채팅 기록을 열면:
- 기존 메시지를 그대로 렌더링
- `conversationId`를 유지한 채 다시 메시지를 보낼 수 있음

논쟁 기록을 열면:
- 저장된 턴을 읽기 전용으로 다시 표시
- 현재는 자동 루프를 다시 붙이지 않고 기록 열람 중심으로 동작

이 동작 덕분에 다른 사용자가 같은 브라우저에서 로그인해도 이전 사용자의 라이브 세션이 섞이지 않는다.

### 페이지 이탈

`visibilitychange`는 탭 전환/최소화에서도 발생해서 너무 공격적이었기 때문에, 현재는 `pagehide`만 사용한다.

정리 시점
- 브라우저 새로고침
- 다른 URL로 이동
- 탭 닫기
- 뒤로가기/앞으로가기 등으로 현재 문서를 떠나는 경우

정리 내용
- 채팅/논쟁 스트림 abort
- `PcmStreamPlayer.dispose()`
- 이미 렌더된 `<audio>` pause
- 음성 인식 stop

### 탈퇴/인증 만료

- 회원 탈퇴는 `App.tsx`가 `DELETE /api/auth/me`를 호출한 뒤 토큰을 제거하고 로그인 화면으로 복귀한다.
- 인증 만료는 `apiFetch`가 `401`을 감지해 전역 이벤트를 발행하고, `App.tsx`가 이를 받아 동일하게 로그인 화면으로 돌린다.
- 유지보수 시 “인증 종료”와 “페이지 이탈”은 서로 다른 정리 경로라는 점을 구분하는 것이 중요하다.

## 6. 추천 유지보수 원칙

- 스트리밍 오디오와 음성 인식은 각각 독립 훅/유틸로 유지한다.
- `useCloneStudio`는 세션 오케스트레이션과 사용자 전용 상태까지만 맡기고, 브라우저 API 세부 구현은 더 넣지 않는다.
- 친구 관계 관리처럼 독립된 화면/도메인은 별도 훅으로 분리하고, 상위 훅에는 재동기화 콜백만 남긴다.
- PCM 포맷, silence threshold, restart delay 같은 값은 상수로 한곳에 모아둔다.
- 인증 토큰 처리와 401 공통 처리는 `api.ts`에 모은다.
- 브라우저별 편차가 큰 기능은 UI 문구와 fallback을 먼저 준비한 뒤 기능을 확장한다.

## 7. 빠른 체크리스트

- 로그인했는데 바로 로그인 화면으로 돌아간다
  - `/api/auth/me` 응답 코드와 `apiFetch(...)`의 401 처리 확인
  - 로컬 스토리지 토큰 값과 JWT 만료 여부 확인
- 다른 사용자 데이터가 잠깐 보인다
  - `useCloneStudio(currentUser)`의 `currentUser.userId` 의존 효과가 유지되는지 확인
  - 사용자 전환 시 `clearActiveSession()`과 목록 재로딩이 모두 호출되는지 확인
- 친구/공개 자산이 선택 목록에 안 보인다
  - `/api/clones?scope=friend|public`, `/api/voices?scope=friend|public` 응답과 `mine/friend/public` 병합 로직 확인
  - DTO에 `isPublic`, `ownerDisplayName`이 내려오는지 확인
- 친구 탭이 비어 있거나 요청 액션 후 갱신되지 않는다
  - Friends 탭 진입 시 `loadFriendData()`가 호출되는지 확인
  - 요청/수락/거절/취소/해제 후 재조회가 일어나는지 확인

- PCM이 깨져 들린다
  - 서버 `sampleRate/channels`와 `PcmStreamPlayer.configure(...)` 전달값 확인
- 논쟁 종료 후에도 소리가 계속 난다
  - `handleDebateExit()` abort, `pagehide` 정리, `PcmStreamPlayer.dispose()` 호출 여부 확인
- 친구/공개 자산으로 시작하려는데 바로 실패한다
  - 자산이 방금 비공개로 전환됐는지 확인
  - 친구 관계가 방금 해제됐는지 확인
  - 프론트에서 `formatAssetAccessError(...)`를 거쳐 친화적 메시지로 바꿔 보여주는지 확인
- 음성 인식이 바로 꺼진다
  - `useSpeechInput`의 `onend` 재시작과 silence threshold 확인
- 설문 제출 버튼이 안 켜진다
  - `answeredCount`와 `questions.length` 비교, `answers[index]` 누락 여부 확인
- 질문 로드 실패
  - `questions.json` 경로와 Vite public asset 경로 확인

## 8. 테스트 포인트

최근 구현 기준으로 회귀가 나기 쉬운 핵심 시나리오는 아래와 같다.

- 인증
  - 저장된 token으로 `/api/auth/me` 세션 복구
  - `401` 응답 시 자동 로그아웃
  - 회원 탈퇴 후 로그인 화면 복귀
- 자산 범위
  - `mine / friend / public` 목록이 각각 올바르게 보이는지
  - 친구/공개 자산에는 작성자 표시가 나오는지
  - 내 자산에만 visibility 토글이 보이는지
- 친구 관계
  - 친구 탭 진입 시 목록 로딩
  - 사용자 검색
  - 요청 보내기 / 수락 / 거절 / 취소 / 친구 해제 후 재조회
- 라이브 사용 흐름
  - 공개 클론/음성으로 채팅 시작
  - 친구 클론/음성으로 채팅 시작
  - 친구 클론/음성 조합으로 논쟁 시작
  - 저장된 채팅/논쟁 기록 다시 열기
- 권한 상실 시 UX
  - 공개 자산이 비공개로 바뀐 경우 친화적 오류 메시지 표시
  - 친구 해제 등으로 친구 자산 접근 권한이 사라진 경우 친화적 오류 메시지 표시
- 실시간 정리
  - 논쟁 종료 시 abort와 오디오 정리
  - `pagehide` 시 스트림, 플레이어, `<audio>` 정리

현재 프론트 테스트 범위

- [App.test.tsx](./src/App.test.tsx)
  - 로그인, 세션 복구, 인증 만료, 회원 탈퇴
- [api.test.ts](./src/features/clone-studio/api.test.ts)
  - access token 저장/전송, `401` 처리, `fetchJsonOrThrow`, 자산 접근 오류 메시지 포맷팅
- [CloneStudioPage.test.tsx](./src/pages/CloneStudioPage.test.tsx)
  - 사용자 전환
  - 친구 탭 액션
  - `내 것 / 친구 / 공개` 자산 섹션 렌더링
  - 공개 자산 채팅
  - 친구 자산 채팅
  - 친구 자산 논쟁
  - 공개/친구 자산 권한 상실 시 메시지 처리

테스트 유지보수 시 주의점

- `useCloneStudio`는 마운트 시 클론, 음성, 채팅 기록, 논쟁 기록을 함께 불러오므로, 테스트 fetch mock에 `scope=friend`까지 빠짐없이 포함해야 한다.
- jsdom은 `HTMLMediaElement.pause()`를 구현하지 않아 경고가 나올 수 있지만, 현재 테스트 실패 원인은 아니다.
- 텍스트가 카드와 모달에 동시에 나타나면 `getByText`보다 `getAllByText`가 더 안전할 수 있다.
