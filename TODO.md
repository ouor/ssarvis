# TODO

## 목적

이 문서는 현재 구현 상태를 [PRD.md](/C:/Users/hurwy/Codes/ssarvis/PRD.md)와 비교해 확인된 미비점을 정리한 실행용 백로그다.  
완료된 PLAN 1~8 이후에도 PRD 기준으로 아직 비어 있는 범위만 다룬다.

## 기준

- 기준 문서: [PRD.md](/C:/Users/hurwy/Codes/ssarvis/PRD.md)
- 검토 시점: 2026-03-28
- 현재 판단: 핵심 SNS/DM/AI 자동응답 흐름은 대부분 구현됨
- 남은 공백: Settings 정보 구조 정리

## 우선순위 요약

1. Settings 화면 구조를 PRD 기준으로 정리

## 완료 1. 프로필 편집 기능 추가

### 배경

PRD는 사용자가 자신의 프로필을 편집할 수 있다고 정의한다.  
현재 구현은 공개성 변경만 가능하고, 표시 이름이나 추가 프로필 정보 수정 흐름은 없다.

### 현재 상태

- 백엔드 프로필 API는 조회와 공개성 변경 중심
- 프론트 `Profile` 탭도 공개성 변경과 내 게시물, AI 자산 관리 중심

### 반영 내용

- 백엔드
  - `PATCH /api/profiles/me` 추가
  - `displayName` 수정 request/response 반영
  - `displayName` 공백/길이 검증 추가
- 프론트
  - `Profile` 탭에 프로필 편집 카드 추가
  - 표시 이름 입력, 저장 중 상태, 실패 메시지 처리
  - 저장 후 상단 계정 바와 프로필 카드 즉시 반영
- 테스트
  - 백엔드 서비스/컨트롤러 테스트 추가
  - 프론트 프로필 저장 UI 테스트 추가

### 완료 조건

- 완료

### 관련 파일

- [FollowController.java](/C:/Users/hurwy/Codes/ssarvis/backend/src/main/java/com/ssarvis/backend/follow/FollowController.java)
- [FollowService.java](/C:/Users/hurwy/Codes/ssarvis/backend/src/main/java/com/ssarvis/backend/follow/FollowService.java)
- [UserProfileResponse.java](/C:/Users/hurwy/Codes/ssarvis/backend/src/main/java/com/ssarvis/backend/follow/UserProfileResponse.java)
- [SnsShell.tsx](/C:/Users/hurwy/Codes/ssarvis/frontend/src/features/sns-shell/SnsShell.tsx)

## 완료 2. 타 사용자 프로필/게시물 탐색 UX 추가

### 배경

PRD는 사용자가 다른 사용자의 프로필과 게시물을 탐색한다고 정의한다.  
현재 백엔드는 관련 API를 갖고 있지만 프론트에서는 검색 결과에서 팔로우/DM만 가능하고 프로필 진입 UX가 없다.

### 현재 상태

- 백엔드
  - 타 사용자 프로필 조회 API 존재
  - 타 사용자 게시물 조회 API 존재
- 프론트
  - `Search` 탭은 검색 결과 카드만 제공
  - 타 사용자 프로필 상세 화면 또는 패널 진입 없음

### 반영 내용

- 프론트
  - 검색 결과에서 `프로필 보기` 동선 추가
  - 같은 `Search` 탭 안에서 타 사용자 프로필 상세와 게시물 목록 표시
  - 프로필 상세 내에서 팔로우/언팔로우와 DM 시작 연결
- 백엔드
  - 기존 `GET /api/profiles/{id}`, `GET /api/profiles/{id}/posts` 재사용
- 테스트
  - 검색 결과에서 프로필 진입 후 게시물 조회와 팔로우/DM 연결 테스트 추가

### 완료 조건

- 완료

### 관련 파일

- [FollowController.java](/C:/Users/hurwy/Codes/ssarvis/backend/src/main/java/com/ssarvis/backend/follow/FollowController.java)
- [PostController.java](/C:/Users/hurwy/Codes/ssarvis/backend/src/main/java/com/ssarvis/backend/post/PostController.java)
- [SnsShell.tsx](/C:/Users/hurwy/Codes/ssarvis/frontend/src/features/sns-shell/SnsShell.tsx)
- [shell.css](/C:/Users/hurwy/Codes/ssarvis/frontend/src/features/sns-shell/shell.css)

## TODO 1. Settings 정보 구조 정리

### 배경

PRD는 `Settings`를 공개성, 자동응답, AI 표시/숨김 관련 개인 설정 화면으로 정의한다.  
현재 구현은 자동응답 설정만 `Settings`에 있고, 공개성은 `Profile`, AI 숨김은 DM 내부 동작으로 흩어져 있다.

### 현재 상태

- `Settings` 탭
  - 자동응답 설정만 존재
- `Profile` 탭
  - 공개 계정/비공개 계정 전환 존재
- `DM` 화면
  - AI 묶음 숨기기/다시 보기 존재

### 필요한 작업

- 제품 구조 정리
  - 공개성을 `Profile`에 남길지 `Settings`로 이동할지 결정
  - PRD를 현재 UX에 맞게 수정할지, 구현을 PRD에 맞출지 선택
- 프론트
  - 결정된 정보 구조에 맞춰 탭 책임 재배치
  - AI 숨김 관련 개인 설정이 별도 전역 설정을 의미하는지, 현재처럼 대화 단위 조작만 의미하는지 문구 정리
- 문서
  - [PRD.md](/C:/Users/hurwy/Codes/ssarvis/PRD.md), [API_SPEC.md](/C:/Users/hurwy/Codes/ssarvis/backend/API_SPEC.md), [GUIDE.md](/C:/Users/hurwy/Codes/ssarvis/frontend/GUIDE.md) 간 서술 일치화

### 완료 조건

- `Profile`과 `Settings`의 책임이 문서와 구현에서 일치한다
- 사용자가 계정 공개성, 자동응답, AI 관련 개인 설정 위치를 헷갈리지 않는다

### 관련 파일

- [PRD.md](/C:/Users/hurwy/Codes/ssarvis/PRD.md)
- [SnsShell.tsx](/C:/Users/hurwy/Codes/ssarvis/frontend/src/features/sns-shell/SnsShell.tsx)
- [API_SPEC.md](/C:/Users/hurwy/Codes/ssarvis/backend/API_SPEC.md)
- [GUIDE.md](/C:/Users/hurwy/Codes/ssarvis/frontend/GUIDE.md)

## 후순위 검토 항목

아래는 현재 당장 버그나 누락으로 보기는 어렵지만, 다음 설계 때 다시 점검할 가치가 있다.

- 비공개 계정의 검색 노출 세부 정책 고정
- 음성 메시지 UX 단순화 여부 검토
- 프로필 응답에 게시물 수, 대표 클론/보이스 상태 등 요약 정보 추가 여부
- 프로필 편집 범위에 이미지, 소개글 같은 필드를 넣을지 결정

## 메모

- 본 문서는 구현 완료 항목이 아니라 `PRD 대비 남은 공백`만 정리한다
- 다음 작업은 `TODO 1`로 남은 Settings 책임 정리를 문서와 구현 양쪽에서 맞추는 것이다
