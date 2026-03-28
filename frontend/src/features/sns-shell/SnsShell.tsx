import { useEffect, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { apiBaseUrl, apiFetch, fetchJsonOrThrow, readErrorMessage } from '../clone-studio/api'
import type { CurrentUser } from '../clone-studio/types'
import './shell.css'

export type SnsShellTab = 'home' | 'search' | 'dm' | 'profile' | 'settings'
type AccountVisibility = 'PUBLIC' | 'PRIVATE'
type AutoReplyMode = 'ALWAYS' | 'AWAY' | 'OFF'

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

type PostSummary = {
  postId: number
  ownerUserId: number
  ownerUsername: string
  ownerDisplayName: string
  ownerVisibility: AccountVisibility
  content: string
  createdAt: string
}

type DmParticipant = {
  userId: number
  username: string
  displayName: string
  visibility: AccountVisibility
}

type DmThreadSummary = {
  threadId: number
  otherParticipant: DmParticipant
  createdAt: string
  latestMessagePreview: string
  latestMessageCreatedAt?: string | null
}

type DmMessage = {
  messageId: number
  senderUserId: number
  senderDisplayName: string
  aiGenerated: boolean
  content: string
  createdAt: string
}

type DmThreadDetail = {
  threadId: number
  otherParticipant: DmParticipant
  createdAt: string
  messages: DmMessage[]
}

type SnsShellProps = {
  currentUser: CurrentUser
  deactivating: boolean
  onDeactivate: () => Promise<void>
  onLogout: () => void
  profileContent: ReactNode
}

type AutoReplySettings = {
  mode: AutoReplyMode
  lastActivityAt?: string | null
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
  const [feedPosts, setFeedPosts] = useState<PostSummary[]>([])
  const [feedLoading, setFeedLoading] = useState(false)
  const [feedError, setFeedError] = useState('')
  const [hasLoadedFeed, setHasLoadedFeed] = useState(false)
  const [myPosts, setMyPosts] = useState<PostSummary[]>([])
  const [myPostsLoading, setMyPostsLoading] = useState(false)
  const [myPostsError, setMyPostsError] = useState('')
  const [postDraft, setPostDraft] = useState('')
  const [postSubmitting, setPostSubmitting] = useState(false)
  const [dmThreads, setDmThreads] = useState<DmThreadSummary[]>([])
  const [dmThreadsLoading, setDmThreadsLoading] = useState(false)
  const [dmError, setDmError] = useState('')
  const [selectedThread, setSelectedThread] = useState<DmThreadDetail | null>(null)
  const [selectedThreadLoading, setSelectedThreadLoading] = useState(false)
  const [dmDraft, setDmDraft] = useState('')
  const [dmSubmitting, setDmSubmitting] = useState(false)
  const [dmActionUserId, setDmActionUserId] = useState<number | null>(null)
  const [autoReplySettings, setAutoReplySettings] = useState<AutoReplySettings>({ mode: 'OFF', lastActivityAt: null })
  const [autoReplyLoading, setAutoReplyLoading] = useState(false)
  const [autoReplyUpdating, setAutoReplyUpdating] = useState(false)
  const [autoReplyError, setAutoReplyError] = useState('')

  const activeShellTab = shellTabs.find((tab) => tab.id === activeTab) ?? shellTabs[0]

  useEffect(() => {
    if (activeTab !== 'home' || hasLoadedFeed) {
      return
    }

    void loadFeedPosts()
  }, [activeTab, hasLoadedFeed])

  useEffect(() => {
    if (activeTab !== 'dm') {
      return
    }

    void loadDmThreads()
  }, [activeTab])

  useEffect(() => {
    if (activeTab !== 'settings') {
      return
    }

    void loadAutoReplySettings()
  }, [activeTab])

  async function loadFeedPosts() {
    setFeedLoading(true)
    setFeedError('')

    try {
      const data = await fetchJsonOrThrow<PostSummary[]>(`${apiBaseUrl}/api/posts/feed`, '피드를 불러오지 못했습니다.')
      setFeedPosts(data)
      setHasLoadedFeed(true)
    } catch (error) {
      setFeedError(error instanceof Error ? error.message : '피드를 불러오지 못했습니다.')
    } finally {
      setFeedLoading(false)
    }
  }

  async function loadMyPosts() {
    setMyPostsLoading(true)
    setMyPostsError('')

    try {
      const data = await fetchJsonOrThrow<PostSummary[]>(`${apiBaseUrl}/api/profiles/me/posts`, '내 게시물을 불러오지 못했습니다.')
      setMyPosts(data)
    } catch (error) {
      setMyPostsError(error instanceof Error ? error.message : '내 게시물을 불러오지 못했습니다.')
    } finally {
      setMyPostsLoading(false)
    }
  }

  async function loadDmThreads() {
    setDmThreadsLoading(true)
    setDmError('')

    try {
      const data = await fetchJsonOrThrow<DmThreadSummary[]>(`${apiBaseUrl}/api/dms/threads`, 'DM 목록을 불러오지 못했습니다.')
      setDmThreads(data)
    } catch (error) {
      setDmError(error instanceof Error ? error.message : 'DM 목록을 불러오지 못했습니다.')
    } finally {
      setDmThreadsLoading(false)
    }
  }

  async function loadAutoReplySettings() {
    setAutoReplyLoading(true)
    setAutoReplyError('')

    try {
      const data = await fetchJsonOrThrow<AutoReplySettings>(`${apiBaseUrl}/api/profiles/me/auto-reply`, '자동응답 설정을 불러오지 못했습니다.')
      setAutoReplySettings(data)
    } catch (error) {
      setAutoReplyError(error instanceof Error ? error.message : '자동응답 설정을 불러오지 못했습니다.')
    } finally {
      setAutoReplyLoading(false)
    }
  }

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

  async function handleStartDm(targetUserId: number) {
    setDmActionUserId(targetUserId)
    setDmError('')

    try {
      const response = await apiFetch(`${apiBaseUrl}/api/dms/threads`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ targetUserId }),
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, 'DM을 시작하지 못했습니다.'))
      }

      const thread: DmThreadDetail = await response.json()
      setSelectedThread(thread)
      setDmDraft('')
      setActiveTab('dm')
      await loadDmThreads()
    } catch (error) {
      setDmError(error instanceof Error ? error.message : 'DM을 시작하지 못했습니다.')
    } finally {
      setDmActionUserId(null)
    }
  }

  async function handleOpenThread(threadId: number) {
    setSelectedThreadLoading(true)
    setDmError('')

    try {
      const data = await fetchJsonOrThrow<DmThreadDetail>(`${apiBaseUrl}/api/dms/threads/${threadId}`, 'DM을 불러오지 못했습니다.')
      setSelectedThread(data)
      setDmDraft('')
    } catch (error) {
      setDmError(error instanceof Error ? error.message : 'DM을 불러오지 못했습니다.')
    } finally {
      setSelectedThreadLoading(false)
    }
  }

  async function handleDmSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!selectedThread || !dmDraft.trim()) {
      return
    }

    setDmSubmitting(true)
    setDmError('')

    try {
      const response = await apiFetch(`${apiBaseUrl}/api/dms/threads/${selectedThread.threadId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: dmDraft.trim() }),
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, '메시지를 보내지 못했습니다.'))
      }

      const message: DmMessage = await response.json()
      setSelectedThread((current) =>
        current
          ? {
              ...current,
              messages: [...current.messages, message],
            }
          : current,
      )
      setDmDraft('')
      await handleOpenThread(selectedThread.threadId)
      await loadDmThreads()
    } catch (error) {
      setDmError(error instanceof Error ? error.message : '메시지를 보내지 못했습니다.')
    } finally {
      setDmSubmitting(false)
    }
  }

  async function handleAutoReplyModeChange(mode: AutoReplyMode) {
    if (autoReplySettings.mode === mode) {
      return
    }

    setAutoReplyUpdating(true)
    setAutoReplyError('')

    try {
      const response = await apiFetch(`${apiBaseUrl}/api/profiles/me/auto-reply`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ mode }),
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, '자동응답 설정을 저장하지 못했습니다.'))
      }

      const data: AutoReplySettings = await response.json()
      setAutoReplySettings(data)
    } catch (error) {
      setAutoReplyError(error instanceof Error ? error.message : '자동응답 설정을 저장하지 못했습니다.')
    } finally {
      setAutoReplyUpdating(false)
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

  async function handlePostSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const normalizedDraft = postDraft.trim()
    if (!normalizedDraft) {
      return
    }

    setPostSubmitting(true)
    setFeedError('')
    setMyPostsError('')

    try {
      const response = await apiFetch(`${apiBaseUrl}/api/posts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: normalizedDraft }),
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, '게시물을 올리지 못했습니다.'))
      }

      const createdPost: PostSummary = await response.json()
      setPostDraft('')
      setFeedPosts((current) => [createdPost, ...current.filter((post) => post.postId !== createdPost.postId)])
      setMyPosts((current) => [createdPost, ...current.filter((post) => post.postId !== createdPost.postId)])
      setHasLoadedFeed(true)
    } catch (error) {
      const message = error instanceof Error ? error.message : '게시물을 올리지 못했습니다.'
      setFeedError(message)
      setMyPostsError(message)
    } finally {
      setPostSubmitting(false)
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
            <section className="sns-shell-post-panel">
              <header className="sns-shell-post-header">
                <div>
                  <strong>내 게시물</strong>
                  <p>프로필 영역에서 내 게시물을 직접 확인하며 공개/비공개 정책이 어떻게 적용될지 검증합니다.</p>
                </div>
                <button className="secondary-button" disabled={myPostsLoading} onClick={() => void loadMyPosts()} type="button">
                  {myPostsLoading ? '불러오는 중...' : '내 게시물 불러오기'}
                </button>
              </header>
              {myPostsError ? <p className="auth-error">{myPostsError}</p> : null}
              {myPosts.map((post) => (
                <article className="sns-shell-post-card" key={post.postId}>
                  <strong>{post.content}</strong>
                  <span>@{post.ownerUsername}</span>
                </article>
              ))}
              {!myPostsLoading && myPosts.length === 0 ? <p className="sns-shell-muted">아직 불러온 게시물이 없습니다.</p> : null}
            </section>
            {profileContent}
          </>
        ) : null}

        {activeTab === 'home' ? (
          <section className="sns-shell-post-panel">
            <form className="sns-shell-post-form" onSubmit={(event) => void handlePostSubmit(event)}>
              <label className="auth-field">
                <span>새 게시물</span>
                <textarea
                  onChange={(event) => setPostDraft(event.target.value)}
                  placeholder="오늘 공유하고 싶은 생각을 적어보세요"
                  rows={4}
                  value={postDraft}
                />
              </label>
              <button className="auth-submit" disabled={postSubmitting} type="submit">
                {postSubmitting ? '게시 중...' : '게시하기'}
              </button>
            </form>

            {feedError ? <p className="auth-error">{feedError}</p> : null}
            {feedLoading ? <p className="sns-shell-muted">피드를 불러오는 중입니다.</p> : null}

            <section className="sns-shell-post-list">
              {feedPosts.map((post) => (
                <article className="sns-shell-post-card" key={post.postId}>
                  <div>
                    <strong>{post.ownerDisplayName}</strong>
                    <span>@{post.ownerUsername}</span>
                  </div>
                  <p>{post.content}</p>
                </article>
              ))}
            </section>

            {!feedLoading && feedPosts.length === 0 ? <p className="sns-shell-muted">피드에 표시할 게시물이 아직 없습니다.</p> : null}
          </section>
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
                  <div className="sns-shell-search-actions">
                    <button
                      className="secondary-button"
                      disabled={followActionUserId === result.userId || result.visibility === 'PRIVATE' && !result.following}
                      onClick={() => void handleFollowToggle(result)}
                      type="button"
                    >
                      {result.following ? '언팔로우' : result.visibility === 'PRIVATE' ? '팔로우 불가' : '팔로우'}
                    </button>
                    <button
                      className="secondary-button"
                      disabled={dmActionUserId === result.userId}
                      onClick={() => void handleStartDm(result.userId)}
                      type="button"
                    >
                      {dmActionUserId === result.userId ? 'DM 여는 중...' : 'DM 시작'}
                    </button>
                  </div>
                </article>
              ))}

              {!searchLoading && searchResults.length === 0 ? (
                <p className="sns-shell-muted">공개 계정 또는 이미 팔로우 중인 비공개 계정만 검색 결과에 표시됩니다.</p>
              ) : null}
            </section>
          </section>
        ) : null}

        {activeTab === 'dm' ? (
          <section className="sns-shell-dm-panel">
            <header className="sns-shell-post-header">
              <div>
                <strong>사람 간 DM</strong>
                <p>이번 단계에서는 클론 채팅과 분리된 1:1 사람 간 DM 구조를 먼저 검증합니다.</p>
              </div>
              <button className="secondary-button" disabled={dmThreadsLoading} onClick={() => void loadDmThreads()} type="button">
                {dmThreadsLoading ? '불러오는 중...' : 'DM 새로고침'}
              </button>
            </header>

            {dmError ? <p className="auth-error">{dmError}</p> : null}

            <div className="sns-shell-dm-layout">
              <section className="sns-shell-dm-list">
                {dmThreads.map((thread) => (
                  <button
                    className={`sns-shell-dm-thread ${selectedThread?.threadId === thread.threadId ? 'sns-shell-dm-thread-active' : ''}`}
                    key={thread.threadId}
                    onClick={() => void handleOpenThread(thread.threadId)}
                    type="button"
                  >
                    <strong>{thread.otherParticipant.displayName}</strong>
                    <span>@{thread.otherParticipant.username}</span>
                    <p>{thread.latestMessagePreview || '아직 주고받은 메시지가 없습니다.'}</p>
                  </button>
                ))}
                {!dmThreadsLoading && dmThreads.length === 0 ? <p className="sns-shell-muted">아직 시작한 DM이 없습니다.</p> : null}
              </section>

              <section className="sns-shell-dm-detail">
                {selectedThreadLoading ? <p className="sns-shell-muted">대화를 불러오는 중입니다.</p> : null}
                {selectedThread ? (
                  <>
                    <header className="sns-shell-dm-detail-header">
                      <strong>{selectedThread.otherParticipant.displayName}</strong>
                      <span>@{selectedThread.otherParticipant.username}</span>
                    </header>
                    <section className="sns-shell-dm-messages">
                      {selectedThread.messages.map((message) => (
                        <article
                          className={`sns-shell-dm-message ${message.senderUserId === currentUser.userId ? 'sns-shell-dm-message-mine' : ''} ${message.aiGenerated ? 'sns-shell-dm-message-ai' : ''}`}
                          key={message.messageId}
                        >
                          <div className="sns-shell-dm-message-header">
                            <strong>{message.senderDisplayName}</strong>
                            {message.aiGenerated ? <span className="sns-shell-ai-badge">AI</span> : null}
                          </div>
                          <p>{message.content}</p>
                        </article>
                      ))}
                      {selectedThread.messages.length === 0 ? <p className="sns-shell-muted">아직 메시지가 없습니다. 첫 메시지를 보내보세요.</p> : null}
                    </section>
                    <form className="sns-shell-dm-form" onSubmit={(event) => void handleDmSubmit(event)}>
                      <label className="auth-field">
                        <span>메시지</span>
                        <textarea
                          onChange={(event) => setDmDraft(event.target.value)}
                          placeholder="메시지를 입력하세요"
                          rows={3}
                          value={dmDraft}
                        />
                      </label>
                      <button className="auth-submit" disabled={dmSubmitting} type="submit">
                        {dmSubmitting ? '전송 중...' : '보내기'}
                      </button>
                    </form>
                  </>
                ) : (
                  <p className="sns-shell-muted">검색 탭에서 상대를 찾아 DM을 시작하거나, 왼쪽 목록에서 기존 대화를 선택하세요.</p>
                )}
              </section>
            </div>
          </section>
        ) : null}

        {activeTab === 'settings' ? (
          <section className="sns-shell-settings-panel">
            <header className="sns-shell-post-header">
              <div>
                <strong>자동응답 설정</strong>
                <p>텍스트 DM에서 내 클론이 언제 대신 답장할지 정합니다. 부재중 기준은 마지막 API 요청 후 3분입니다.</p>
              </div>
            </header>
            {autoReplyError ? <p className="auth-error">{autoReplyError}</p> : null}
            {autoReplyLoading ? <p className="sns-shell-muted">설정을 불러오는 중입니다.</p> : null}
            {!autoReplyLoading ? (
              <div className="sns-shell-visibility-row">
                <button
                  className={`sns-visibility-button ${autoReplySettings.mode === 'ALWAYS' ? 'sns-visibility-button-active' : ''}`}
                  disabled={autoReplyUpdating}
                  onClick={() => void handleAutoReplyModeChange('ALWAYS')}
                  type="button"
                >
                  항상
                </button>
                <button
                  className={`sns-visibility-button ${autoReplySettings.mode === 'AWAY' ? 'sns-visibility-button-active' : ''}`}
                  disabled={autoReplyUpdating}
                  onClick={() => void handleAutoReplyModeChange('AWAY')}
                  type="button"
                >
                  부재중일 때만
                </button>
                <button
                  className={`sns-visibility-button ${autoReplySettings.mode === 'OFF' ? 'sns-visibility-button-active' : ''}`}
                  disabled={autoReplyUpdating}
                  onClick={() => void handleAutoReplyModeChange('OFF')}
                  type="button"
                >
                  사용 안함
                </button>
              </div>
            ) : null}
            <p className="sns-shell-muted">
              마지막 활동 시각:{' '}
              {autoReplySettings.lastActivityAt ? new Date(autoReplySettings.lastActivityAt).toLocaleString('ko-KR') : '아직 기록 없음'}
            </p>
          </section>
        ) : null}

        {activeTab !== 'profile' && activeTab !== 'search' && activeTab !== 'dm' && activeTab !== 'settings' ? (
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
