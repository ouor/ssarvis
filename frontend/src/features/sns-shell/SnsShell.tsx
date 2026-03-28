import { useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { apiBaseUrl, apiFetch, fetchJsonOrThrow, readErrorMessage } from '../clone-studio/api'
import type { CurrentUser } from '../clone-studio/types'
import './shell.css'

export type SnsShellTab = 'home' | 'search' | 'dm' | 'profile' | 'settings'
type AccountVisibility = 'PUBLIC' | 'PRIVATE'

type FollowUserSummary = {
  userId: number
  username: string
  displayName: string
  visibility: AccountVisibility
  following: boolean
}

type UserProfile = {
  userId: number
  username: string
  displayName: string
  visibility: AccountVisibility
  me: boolean
  following: boolean
}

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
    eyebrow: 'Sprint 3 Preview',
    title: '피드와 게시물 경험을 위한 자리입니다.',
    description: '다음 단계에서 홈 피드, 게시물 카드, 공개/비공개 계정 정책이 이 영역을 중심으로 구체화됩니다.',
  },
  {
    id: 'search',
    label: 'Search',
    eyebrow: 'Sprint 2 Result',
    title: '공개 계정 탐색과 팔로우 구조를 먼저 연결합니다.',
    description: '이번 단계에서는 사용자 검색과 공개 계정 팔로우 동작을 실제 API와 연결해 SNS 관계 모델을 고정합니다.',
  },
  {
    id: 'dm',
    label: 'DM',
    eyebrow: 'Sprint 4 Preview',
    title: '사람 간 DM이 새 제품의 기본 대화 구조가 됩니다.',
    description: '다음 단계에서는 기존 클론 채팅과 분리된 DM 목록과 대화창이 이 구역에 만들어집니다.',
  },
  {
    id: 'profile',
    label: 'Profile',
    eyebrow: 'Sprint 2 Result',
    title: '기존 스튜디오를 유지하면서 계정 공개성을 먼저 고정합니다.',
    description: '이번 단계에서는 프로필 상단에서 공개/비공개 설정을 제어하고, 하위 작업 공간에서 기존 기능을 계속 유지합니다.',
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
  const [profile, setProfile] = useState<UserProfile>({
    userId: currentUser.userId,
    username: currentUser.username,
    displayName: currentUser.displayName,
    visibility: currentUser.visibility ?? 'PUBLIC',
    me: true,
    following: false,
  })
  const [profileError, setProfileError] = useState('')
  const [visibilityUpdating, setVisibilityUpdating] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<FollowUserSummary[]>([])
  const [searchLoading, setSearchLoading] = useState(false)
  const [searchError, setSearchError] = useState('')
  const [followActionUserId, setFollowActionUserId] = useState<number | null>(null)

  const activeShellTab = shellTabs.find((tab) => tab.id === activeTab) ?? shellTabs[0]

  async function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalizedQuery = searchQuery.trim()

    if (!normalizedQuery) {
      setSearchResults([])
      setSearchError('')
      return
    }

    setSearchLoading(true)
    setSearchError('')

    try {
      const data = await fetchJsonOrThrow<FollowUserSummary[]>(
        `${apiBaseUrl}/api/follows/users/search?query=${encodeURIComponent(normalizedQuery)}`,
        '사용자 검색에 실패했습니다.',
      )
      setSearchResults(data)
    } catch (error) {
      setSearchError(error instanceof Error ? error.message : '사용자 검색에 실패했습니다.')
    } finally {
      setSearchLoading(false)
    }
  }

  async function handleFollowToggle(result: FollowUserSummary) {
    setFollowActionUserId(result.userId)
    setSearchError('')

    try {
      const response = await apiFetch(`${apiBaseUrl}/api/follows/${result.userId}`, {
        method: result.following ? 'DELETE' : 'POST',
      })

      if (!response.ok) {
        throw new Error(
          await readErrorMessage(response, result.following ? '언팔로우에 실패했습니다.' : '팔로우에 실패했습니다.'),
        )
      }

      setSearchResults((current) =>
        current.map((item) => (item.userId === result.userId ? { ...item, following: !result.following } : item)),
      )
    } catch (error) {
      setSearchError(error instanceof Error ? error.message : '팔로우 상태를 변경하지 못했습니다.')
    } finally {
      setFollowActionUserId(null)
    }
  }

  async function handleVisibilityChange(nextVisibility: AccountVisibility) {
    if (profile?.visibility === nextVisibility) {
      return
    }

    setVisibilityUpdating(true)
    setProfileError('')

    try {
      const response = await apiFetch(`${apiBaseUrl}/api/profiles/me/visibility`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ visibility: nextVisibility }),
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, '공개성 설정을 변경하지 못했습니다.'))
      }

      const updatedProfile: UserProfile = await response.json()
      setProfile(updatedProfile)
    } catch (error) {
      setProfileError(error instanceof Error ? error.message : '공개성 설정을 변경하지 못했습니다.')
    } finally {
      setVisibilityUpdating(false)
    }
  }

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
          <>
            <section className="sns-shell-profile-card">
              <div>
                <strong>계정 공개성</strong>
                <p>공개 계정은 누구나 DM 가능하고 검색할 수 있습니다. 비공개 계정은 기존 팔로워만 접근할 수 있습니다.</p>
              </div>
              {profileError ? <p className="auth-error">{profileError}</p> : null}
              <div className="sns-shell-visibility-row">
                <button
                  className={`sns-visibility-button ${profile.visibility === 'PUBLIC' ? 'sns-visibility-button-active' : ''}`}
                  disabled={visibilityUpdating}
                  onClick={() => void handleVisibilityChange('PUBLIC')}
                  type="button"
                >
                  공개 계정
                </button>
                <button
                  className={`sns-visibility-button ${profile.visibility === 'PRIVATE' ? 'sns-visibility-button-active' : ''}`}
                  disabled={visibilityUpdating}
                  onClick={() => void handleVisibilityChange('PRIVATE')}
                  type="button"
                >
                  비공개 계정
                </button>
              </div>
            </section>
            {profileContent}
          </>
        ) : null}

        {activeTab === 'search' ? (
          <section className="sns-shell-search-panel">
            <form className="sns-shell-search-form" onSubmit={(event) => void handleSearchSubmit(event)}>
              <label className="auth-field">
                <span>사용자 검색</span>
                <input
                  onChange={(event) => setSearchQuery(event.target.value)}
                  placeholder="표시명 또는 아이디를 입력하세요"
                  value={searchQuery}
                />
              </label>
              <button className="auth-submit" disabled={searchLoading} type="submit">
                {searchLoading ? '검색 중...' : '검색'}
              </button>
            </form>

            {searchError ? <p className="auth-error">{searchError}</p> : null}

            <section className="sns-shell-search-results">
              {searchResults.map((result) => (
                <article className="sns-shell-search-card" key={result.userId}>
                  <div>
                    <strong>{result.displayName}</strong>
                    <span>@{result.username}</span>
                    <p>{result.visibility === 'PUBLIC' ? '공개 계정' : '비공개 계정'}</p>
                  </div>
                  <button
                    className="secondary-button"
                    disabled={followActionUserId === result.userId || result.visibility === 'PRIVATE' && !result.following}
                    onClick={() => void handleFollowToggle(result)}
                    type="button"
                  >
                    {result.following ? '언팔로우' : result.visibility === 'PRIVATE' ? '팔로우 불가' : '팔로우'}
                  </button>
                </article>
              ))}

              {!searchLoading && searchResults.length === 0 ? (
                <p className="sns-shell-muted">공개 계정 또는 이미 팔로우 중인 비공개 계정만 검색 결과에 표시됩니다.</p>
              ) : null}
            </section>
          </section>
        ) : null}

        {activeTab !== 'profile' && activeTab !== 'search' ? (
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
        ) : null}
      </section>
    </main>
  )
}

export default SnsShell
