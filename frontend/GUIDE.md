# Frontend Guide

이 문서는 현재 프론트엔드 구현 중 아래 3가지 기능을 빠르게 이해하고 유지보수할 수 있도록 정리한 가이드다.

- 실시간 PCM 재생
- 음성 입력(Web Speech API)
- 설문 표시 및 처리

기준 경로
- 프로젝트 루트: `frontend`
- 메인 화면: [CloneStudioPage.tsx](./src/pages/CloneStudioPage.tsx)
- 화면 상태 오케스트레이션: [useCloneStudio.ts](./src/features/clone-studio/hooks/useCloneStudio.ts)

## 1. 실시간 PCM 재생

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
- `dispose()`는 오디오 컨텍스트만 닫고, 현재 WAV URL은 즉시 revoke하지 않는다.

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

## 2. 음성 입력(Web Speech API)

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

## 3. 설문 표시 및 처리

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
const response = await fetch(`${apiBaseUrl}/api/system-prompt`, {
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

## 4. 추천 유지보수 원칙

- 스트리밍 오디오와 음성 인식은 각각 독립 훅/유틸로 유지한다.
- `useCloneStudio`는 세션 오케스트레이션까지만 맡기고, 브라우저 API 세부 구현은 더 넣지 않는다.
- PCM 포맷, silence threshold, restart delay 같은 값은 상수로 한곳에 모아둔다.
- 브라우저별 편차가 큰 기능은 UI 문구와 fallback을 먼저 준비한 뒤 기능을 확장한다.

## 5. 빠른 체크리스트

- PCM이 깨져 들린다
  - 서버 `sampleRate/channels`와 `PcmStreamPlayer.configure(...)` 전달값 확인
- 음성 인식이 바로 꺼진다
  - `useSpeechInput`의 `onend` 재시작과 silence threshold 확인
- 설문 제출 버튼이 안 켜진다
  - `answeredCount`와 `questions.length` 비교, `answers[index]` 누락 여부 확인
- 질문 로드 실패
  - `questions.json` 경로와 Vite public asset 경로 확인
