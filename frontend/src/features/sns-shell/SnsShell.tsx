import { useState } from 'react'
import type { ReactNode } from 'react'
import type { CurrentUser } from '../clone-studio/types'
import './shell.css'

export type SnsShellTab = 'home' | 'search' | 'dm' | 'profile' | 'settings'

type SnsShellProps = {
  currentUser: CurrentUser
  deactivating: boolean
  onDeactivate: () => Promise<void>
  onLogout: () => void
  profileContent: ReactNode
}

const shellTabs: Array<{ id: SnsShellTab; label: string; eyebrow: string; title: string; description: string }> = [
  {
    id: 'home',
    label: 'Home',
    eyebrow: 'Sprint 2 Preview',
    title: '피드와 게시물 경험을 위한 자리입니다.',
    description: '다음 단계에서 홈 피드, 게시물 카드, 공개/비공개 계정 정책이 이 영역을 중심으로 구체화됩니다.',
  },
  {
    id: 'search',
    label: 'Search',
    eyebrow: 'Sprint 1 Scaffold',
    title: '사용자 검색 흐름의 진입점을 미리 열어둡니다.',
    description: '팔로우와 계정 공개성 정책이 들어오면 검색 결과와 프로필 접근 로직이 이 화면부터 연결됩니다.',
  },
  {
    id: 'dm',
    label: 'DM',
    eyebrow: 'Sprint 3 Preview',
    title: '사람 간 DM이 새 제품의 기본 대화 구조가 됩니다.',
    description: '다음 단계에서는 기존 클론 채팅과 분리된 DM 목록과 대화창이 이 구역에 만들어집니다.',
  },
  {
    id: 'profile',
    label: 'Profile',
    eyebrow: 'Current Workspace',
    title: '기존 스튜디오 기능은 우선 프로필 하위 작업 공간에서 유지합니다.',
    description: '클론, 보이스, 라이브 세션, 논쟁 기능은 재설계 전까지 이 영역에서 계속 검증하며 가져갑니다.',
  },
  {
    id: 'settings',
    label: 'Settings',
    eyebrow: 'Sprint 6 Preview',
    title: '자동응답과 AI 가시성 설정이 들어올 자리입니다.',
    description: '나중에는 자동응답 모드, AI 응답 숨김, 공개성 관련 개인 설정이 이 화면에 모입니다.',
  },
]

function SnsShell({ currentUser, deactivating, onDeactivate, onLogout, profileContent }: SnsShellProps) {
  const [activeTab, setActiveTab] = useState<SnsShellTab>('profile')
  const activeShellTab = shellTabs.find((tab) => tab.id === activeTab) ?? shellTabs[0]

  return (
    <main className="sns-shell">
      <section className="sns-shell-header">
        <div className="sns-shell-copy">
          <p className="sns-shell-kicker">SSARVIS Reboot</p>
          <h1>SNS와 사람 간 DM 중심 구조로 전환하는 중입니다.</h1>
          <p>
            지금 단계에서는 새 앱 셸을 먼저 열고, 기존 클론 스튜디오를 프로필 하위 작업 공간으로 유지해 다음 단계의
            전환 리스크를 줄입니다.
          </p>
        </div>

        <section className="sns-account-bar">
          <div>
            <p className="sns-shell-kicker">Signed In</p>
            <strong>{currentUser.displayName}</strong>
            <span>@{currentUser.username}</span>
          </div>
          <div className="sns-account-actions">
            <button className="secondary-button" disabled={deactivating} onClick={onLogout} type="button">
              로그아웃
            </button>
            <button className="secondary-button" disabled={deactivating} onClick={() => void onDeactivate()} type="button">
              {deactivating ? '탈퇴 처리 중...' : '회원 탈퇴'}
            </button>
          </div>
        </section>
      </section>

      <nav aria-label="Primary" className="sns-shell-nav">
        {shellTabs.map((tab) => (
          <button
            key={tab.id}
            aria-pressed={tab.id === activeTab}
            className={`sns-shell-tab ${tab.id === activeTab ? 'sns-shell-tab-active' : ''}`}
            onClick={() => setActiveTab(tab.id)}
            type="button"
          >
            <span>{tab.label}</span>
          </button>
        ))}
      </nav>

      <section className="sns-shell-stage">
        <header className="sns-shell-stage-copy">
          <p className="sns-shell-kicker">{activeShellTab.eyebrow}</p>
          <h2>{activeShellTab.title}</h2>
          <p>{activeShellTab.description}</p>
        </header>

        {activeTab === 'profile' ? (
          profileContent
        ) : (
          <section className="sns-shell-placeholder">
            <div>
              <strong>{activeShellTab.label}</strong>
              <p>이 화면은 기반 정리 단계에서 정보 구조와 진입 흐름을 먼저 고정하기 위해 추가된 플레이스홀더입니다.</p>
            </div>
            <div>
              <strong>다음 연결 대상</strong>
              <p>{activeShellTab.description}</p>
            </div>
          </section>
        )}
      </section>
    </main>
  )
}

export default SnsShell
