# Frontend Guide

이 문서는 현재 프론트엔드 구현을 빠르게 이해하고 유지보수하기 위한 실전 가이드다.  
기준 시점은 `SNS + 사람 간 DM` 메인 흐름과, `Profile` 하위에 남아 있는 레거시 클론 스튜디오가 공존하는 상태다.

## 1. 현재 화면 구조

핵심 파일
- 앱 루트: [App.tsx](./src/App.tsx)
- 메인 페이지: [CloneStudioPage.tsx](./src/pages/CloneStudioPage.tsx)
- 새 SNS 앱 셸: [SnsShell.tsx](./src/features/sns-shell/SnsShell.tsx)
- SNS 셸 스타일: [shell.css](./src/features/sns-shell/shell.css)
- 레거시 프로필 워크스페이스 훅: [useCloneStudio.ts](./src/features/clone-studio/hooks/useCloneStudio.ts)

현재 정보 구조
- `Home`
  - 게시물 작성
  - 피드 조회
- `Search`
  - 사용자 검색
  - 팔로우
  - DM 시작
- `DM`
  - 사람 간 DM 목록
  - 사람 간 DM 상세
- `Profile`
  - 계정 공개성
  - 내 게시물
  - 내 AI 프로필 자산 요약
  - 레거시 클론 스튜디오 작업공간
- `Settings`
  - 아직 플레이스홀더

중요 포인트
- 메인 내비게이션은 이미 SNS 중심이다.
- 클론/보이스/논쟁은 메인 제품이 아니라 `Profile` 하위 작업공간이다.

## 2. 상태 소유권

현재 상태는 두 층으로 나뉜다.

- `App.tsx`
  - 인증 상태
  - 세션 복구
  - 로그아웃
  - 회원 탈퇴
- `SnsShell.tsx`
  - SNS 메인 탭 상태
  - 프로필 공개성
  - 자동응답 설정
  - 사용자 검색
  - 팔로우 액션
  - 게시물 작성/피드
  - 사람 간 DM 목록/상세/전송
- `useCloneStudio.ts`
  - 프로필 하위 레거시 워크스페이스 상태
  - 설문 기반 클론 생성
  - 보이스 등록
  - 클론 대화
  - 논쟁
  - 친구 기반 레거시 자산 탐색

핵심 원칙
- 인증 상태를 `useCloneStudio`로 내리지 않는다.
- SNS 메인 흐름 상태를 `useCloneStudio`에 더 넣지 않는다.
- `useCloneStudio`는 점진적으로 줄어드는 레거시 오케스트레이터로 본다.

## 3. 인증과 공통 API 유틸

관련 파일
- [App.tsx](./src/App.tsx)
- [api.ts](./src/features/clone-studio/api.ts)

핵심 함수
- `getStoredAccessToken()`
- `storeAccessToken()`
- `clearStoredAccessToken()`
- `apiFetch(...)`
- `fetchJsonOrThrow(...)`
- `readErrorMessage(...)`

중요 동작
- `apiFetch(...)`는 저장된 access token을 자동으로 `Authorization` 헤더에 붙인다.
- 보호 API가 `401`을 반환하면 토큰을 지우고 `ssarvis:auth-expired` 이벤트를 발행한다.
- `App.tsx`는 이 이벤트를 받아 로그인 화면으로 되돌린다.

실전 규칙
- 보호 API는 직접 `fetch`보다 `apiFetch(...)`를 우선 사용한다.
- JSON 응답 API는 `fetchJsonOrThrow(...)`를 우선 사용한다.
- 새 SNS API도 같은 유틸을 그대로 재사용한다.

## 4. SNS 메인 셸 구현 포인트

관련 파일
- [SnsShell.tsx](./src/features/sns-shell/SnsShell.tsx)
- [shell.css](./src/features/sns-shell/shell.css)

`SnsShell.tsx`가 가진 주요 로컬 상태
- `activeTab`
- `profile`, `profileError`, `visibilityUpdating`
- `searchQuery`, `searchResults`, `searchLoading`, `searchError`
- `feedPosts`, `feedLoading`, `feedError`, `postDraft`, `postSubmitting`
- `dmThreads`, `dmThreadsLoading`, `dmError`
- `selectedThread`, `selectedThreadLoading`
- `dmDraft`, `dmSubmitting`

현재 연결된 SNS API
- `GET /api/profiles/me`
  - 현재는 초기 상태를 `currentUser`에서 받고, 공개성 변경은 PATCH만 사용
- `PATCH /api/profiles/me/visibility`
- `GET /api/profiles/me/auto-reply`
- `PATCH /api/profiles/me/auto-reply`
- `GET /api/follows/users/search`
- `POST /api/follows/{targetUserId}`
- `DELETE /api/follows/{targetUserId}`
- `GET /api/posts/feed`
- `POST /api/posts`
- `GET /api/profiles/me/posts`
- `GET /api/dms/threads`
- `POST /api/dms/threads`
- `GET /api/dms/threads/{threadId}`
- `POST /api/dms/threads/{threadId}/messages`

중요 포인트
- `Search`에서 `DM 시작`을 누르면 즉시 `DM` 탭으로 이동한다.
- `DM` 탭은 사람 간 DM 전용이다.
- 서버가 조건을 만족하면 `DM` 탭 메시지 사이에 AI 프록시 응답이 함께 내려온다.

## 5. Profile 탭의 이중 구조

관련 파일
- [CloneStudioPage.tsx](./src/pages/CloneStudioPage.tsx)

`Profile` 탭은 두 부분으로 나뉜다.

1. 새 제품 기준 영역
- 계정 공개성 카드
- 내 게시물 패널
- `내 AI 프로필 자산` 패널

2. 레거시 작업공간
- `StudioHero`
- `StudioTabs`
- 클론/친구/라이브 세션 패널
- 각종 모달

`내 AI 프로필 자산` 패널 의미
- 클론과 보이스가 이제 독립 캐릭터가 아니라 내 계정의 대표 자산이라는 사실을 상단에서 명시한다.
- `mineClones[0]`, `mineVoices[0]`만 대표 자산처럼 보여준다.

주의
- 프로필 탭에서 보이는 “내 AI 프로필 자산” 요약과 아래 스튜디오 카드가 같은 내용을 중복해서 보여줄 수 있다.
- 테스트나 선택자 구현 시 텍스트 중복을 고려해야 한다.

## 6. 클론/보이스의 단일 자산 정책

관련 파일
- [useCloneStudio.ts](./src/features/clone-studio/hooks/useCloneStudio.ts)
- [CloneGridSection.tsx](./src/features/clone-studio/components/CloneGridSection.tsx)
- [VoicePickerModal.tsx](./src/features/clone-studio/components/modals/VoicePickerModal.tsx)

현재 정책
- 사용자당 대표 클론 1개
- 사용자당 대표 보이스 1개

프론트에서 이 정책이 드러나는 지점
- `mineClones`는 실질적으로 1개만 오는 것을 전제한다.
- `mineVoices`도 실질적으로 1개만 오는 것을 전제한다.
- “새 클론 만들기” 문구가 이미 `내 클론 다시 만들기`로 바뀔 수 있다.
- 프로필 요약 패널에 “새로 만들면 기존 자산을 갱신한다”는 문구가 있다.

중요 포인트
- 현재 `friendClones`, `publicClones`, `friendVoices`, `publicVoices`는 레거시 체험 흐름 때문에 남아 있다.
- 메인 제품 기준으로는 “내 자산 관리”가 먼저고, 외부 공개 자산 탐색은 점차 축소될 영역이다.

## 7. 사람 간 DM 구현 포인트

관련 파일
- [SnsShell.tsx](./src/features/sns-shell/SnsShell.tsx)

주요 함수
- `loadDmThreads()`
- `handleStartDm(targetUserId)`
- `handleOpenThread(threadId)`
- `handleDmSubmit(event)`

동작 요약
- `Search` 탭에서 `DM 시작`
  - `POST /api/dms/threads`
  - 성공 시 `selectedThread` 저장
  - `activeTab = 'dm'`
  - 목록 재조회
- `DM` 탭 진입 시
  - `GET /api/dms/threads`
- 목록 클릭 시
  - `GET /api/dms/threads/{threadId}`
- 메시지 전송 시
  - `POST /api/dms/threads/{threadId}/messages`
  - 보낸 사람 메시지를 먼저 detail에 반영
  - 이어서 thread detail을 다시 불러와 AI 자동응답까지 함께 반영
  - 목록 재조회

주의
- 같은 이름이 DM 목록과 상세 헤더에 동시에 나타날 수 있다.
- 테스트에서 `getByText`보다 `getAllByText`가 더 안전한 경우가 있다.
- AI 응답은 `aiGenerated` 플래그로 구분하고, UI에서는 `AI` 배지를 붙인다.

## 8. Settings 탭과 자동응답

관련 파일
- [SnsShell.tsx](./src/features/sns-shell/SnsShell.tsx)

현재 로컬 상태
- `autoReplySettings`
- `autoReplyLoading`
- `autoReplyUpdating`
- `autoReplyError`

주요 함수
- `loadAutoReplySettings()`
- `handleAutoReplyModeChange(mode)`

현재 동작
- `Settings` 탭 진입 시 `GET /api/profiles/me/auto-reply`
- 버튼 선택 시 `PATCH /api/profiles/me/auto-reply`
- 모드 값은 다음처럼 쓴다.
  - `ALWAYS`
  - `AWAY`
  - `OFF`

주의
- 부재중 판정은 프론트 타이머가 아니라 서버의 `lastActivityAt` 기준이다.
- 프론트는 마지막 활동 시각을 표시만 하고 판정하지 않는다.

## 9. 게시물과 공개성 구현 포인트

관련 파일
- [SnsShell.tsx](./src/features/sns-shell/SnsShell.tsx)

핵심 함수
- `loadFeedPosts()`
- `loadMyPosts()`
- `handlePostSubmit(event)`
- `handleVisibilityChange(nextVisibility)`

동작 요약
- `Home` 탭 첫 진입 시 피드를 1회 로드한다.
- `Profile` 탭에서 `내 게시물 불러오기` 버튼으로 내 게시물을 가져온다.
- 게시물 작성 성공 시
  - `feedPosts` 앞에 prepend
  - `myPosts` 앞에 prepend
- 공개성 변경은 `PATCH /api/profiles/me/visibility`

주의
- 공개/비공개 규칙은 프론트가 아니라 서버가 최종 판정한다.
- 프론트는 UI 힌트만 준다.

## 10. 레거시 클론 스튜디오 오케스트레이션

관련 파일
- [useCloneStudio.ts](./src/features/clone-studio/hooks/useCloneStudio.ts)

현재 `useCloneStudio` 책임
- 설문 질문 로드
- 클론 목록 로드
- 보이스 목록 로드
- 채팅 기록 로드
- 논쟁 기록 로드
- 모달 상태
- 라이브 채팅 스트림
- 라이브 논쟁 스트림
- 음성 입력 훅 연결
- 레거시 친구 워크스페이스 연결

현재 로딩 API
- `/api/clones?scope=mine|friend|public`
- `/api/voices?scope=mine|friend|public`
- `/api/chat/conversations`
- `/api/debates`

주의
- 이 훅은 이미 크다.
- 새 SNS 기능을 여기에 더 추가하지 않는다.
- 앞으로 DM 자동응답을 붙일 때도 `SnsShell`과 별도 훅으로 나누는 편이 안전하다.

## 11. 스트리밍 오디오와 음성 입력

관련 파일
- [PcmStreamPlayer.ts](./src/utils/PcmStreamPlayer.ts)
- [useSpeechInput.ts](./src/features/clone-studio/hooks/useSpeechInput.ts)

이 부분은 아직 레거시 작업공간에서만 적극 사용된다.

현재 구조
- 클론 채팅과 논쟁 스트림은 NDJSON + PCM 청크 방식
- `PcmStreamPlayer`가 PCM을 재생하고 최종 WAV URL을 만든다
- `useSpeechInput`이 Web Speech API와 silence 감지를 감싼다

중요 포인트
- `pagehide`에서 정리한다.
- `HTMLMediaElement.pause()`는 jsdom에서 경고만 나고 테스트 실패 원인은 아니다.

## 12. 테스트할 때 꼭 알아둘 점

관련 파일
- [CloneStudioPage.test.tsx](./src/pages/CloneStudioPage.test.tsx)
- [App.test.tsx](./src/App.test.tsx)
- [api.test.ts](./src/features/clone-studio/api.test.ts)

실전 포인트
- `CloneStudioPage`는 마운트 시 매우 많은 API를 한 번에 호출한다.
- 테스트 fetch mock에는 보통 아래가 필요하다.
  - `/questions.json`
  - `/api/clones?scope=mine|friend|public`
  - `/api/voices?scope=mine|friend|public`
  - `/api/chat/conversations`
  - `/api/debates`
- SNS 탭 테스트에는 추가로 아래가 들어간다.
  - `/api/follows/users/search`
  - `/api/follows/{id}`
  - `/api/posts/feed`
  - `/api/posts`
  - `/api/profiles/me/posts`
  - `/api/profiles/me/visibility`
  - `/api/profiles/me/auto-reply`
  - `/api/dms/threads`
  - `/api/dms/threads/{id}`
  - `/api/dms/threads/{id}/messages`

선택자 팁
- 같은 텍스트가 프로필 요약 패널과 카드에 동시에 보일 수 있다.
- 같은 이름이 DM 목록과 DM 상세 헤더에 동시에 보일 수 있다.
- 이런 경우 `getAllByText(...)`, `findAllByText(...)`, 더 좁은 `closest(...)` 사용이 안전하다.

## 13. 유지보수 원칙

- 새 SNS 기능은 `SnsShell` 기준으로 추가한다.
- 레거시 기능 확장은 꼭 필요한 경우만 `useCloneStudio`에 넣는다.
- 인증과 공통 에러 처리는 `api.ts`에 모은다.
- 사람이 주체인 DM과 클론 체험용 채팅은 도메인을 섞지 않는다.
- “내 대표 클론/보이스 1개” 정책을 깨는 UI는 만들지 않는다.
- 자동응답 판정은 프론트가 아니라 서버 정책에 맡긴다.
- 문서와 테스트에서 메인 제품 흐름과 레거시 작업공간을 명시적으로 구분한다.

## 14. 빠른 체크리스트

- 검색은 되는데 DM 시작이 실패한다
  - 대상 계정이 비공개인지
  - 이미 팔로우 중인지
  - `/api/dms/threads` 응답 코드를 확인
- DM은 보내졌는데 AI 답장이 안 온다
  - 상대 사용자의 자동응답 모드가 `ALWAYS` 또는 `AWAY`인지 확인
  - 상대 사용자의 대표 클론이 있는지 확인
  - 상대 사용자의 마지막 활동 시각이 최근 3분 이내인지 확인
- 프로필에서 클론/보이스가 여러 개처럼 보인다
  - `mineClones[0]`, `mineVoices[0]` 요약 패널과 아래 레거시 카드가 중복 표시되는지 먼저 확인
  - 서버 `scope=mine` 응답이 1개만 오는지 확인
- 게시물 작성 후 프로필/피드가 안 갱신된다
  - `handlePostSubmit`에서 `feedPosts`, `myPosts` 둘 다 갱신하는지 확인
- 공개성 토글 후 UI가 안 바뀐다
  - `PATCH /api/profiles/me/visibility` 응답이 `profile` 상태에 반영되는지 확인
- 자동응답 토글 후 UI가 안 바뀐다
  - `PATCH /api/profiles/me/auto-reply` 응답이 `autoReplySettings` 상태에 반영되는지 확인
- 테스트가 텍스트 중복으로 깨진다
  - `getByText` 대신 `getAllByText`로 바꿀 후보인지 먼저 확인
