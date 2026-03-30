import { useCallback, useEffect, useState } from 'react'
import { ContextPanel } from '../../components/shared/ContextPanel'
import { EmptyState } from '../../components/shared/EmptyState'
import { LoadingState } from '../../components/shared/LoadingState'
import { PageHeader } from '../../components/shared/PageHeader'
import { Button } from '../../components/ui/Button'
import refreshIcon from '../../assets/refresh.svg'
import { Card } from '../../components/ui/Card'
import { Chip } from '../../components/ui/Chip'
import { useAuth } from '../../hooks/useAuth'
import { createPost, getFeed } from '../../features/feed/api'
import { FeedList } from '../../features/feed/components/FeedList'
import { toFeedPostViewModel } from '../../features/feed/mappers'
import type { FeedPostViewModel } from '../../features/feed/types'
import { PostComposer } from '../../features/post-compose/components/PostComposer'
import {
  getRequiredAccessToken,
  SessionRequiredError,
} from '../../lib/api/session'
import {
  getDemoFeedPosts,
  getDemoStudioStatus,
  getDemoSuggestedUsers,
  getDemoUser,
} from '../../lib/demo/adapters'

export function HomePage() {
  const { currentUser, isDemo } = useAuth()
  const [posts, setPosts] = useState<FeedPostViewModel[]>(() => getDemoFeedPosts())
  const [isLoading, setIsLoading] = useState(!isDemo)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [composerError, setComposerError] = useState<string | null>(null)

  const loadFeed = useCallback(async () => {
    if (isDemo) {
      setPosts(
        getDemoFeedPosts(),
      )
      setIsLoading(false)
      setError(null)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const token = getRequiredAccessToken(
        '세션 정보가 없어 피드를 불러올 수 없습니다.',
      )
      const response = await getFeed(token)
      setPosts(response.map(toFeedPostViewModel))
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setError(error.message)
      } else {
        setError('피드를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.')
      }
    } finally {
      setIsLoading(false)
    }
  }, [isDemo])

  useEffect(() => {
    void loadFeed()
  }, [loadFeed])

  async function handleCreatePost(content: string) {
    if (isDemo) {
      setPosts((current) => [
        {
          id: Date.now(),
          author: currentUser ?? getDemoUser(),
          postedAt: '방금 전',
          content,
        },
        ...current,
      ])
      setComposerError(null)
      return true
    }

    setIsSubmitting(true)
    setComposerError(null)

    try {
      const token = getRequiredAccessToken(
        '로그인 세션이 없어 게시할 수 없습니다.',
      )
      const response = await createPost(token, content)
      setPosts((current) => [toFeedPostViewModel(response), ...current])
      return true
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setComposerError(error.message)
        return false
      }

      setComposerError('게시물 작성에 실패했습니다. 잠시 후 다시 시도해주세요.')
      return false
    } finally {
      setIsSubmitting(false)
    }
  }

  const user = currentUser ?? getDemoUser()
  const suggestedUsers = getDemoSuggestedUsers()
  const studioStatus = getDemoStudioStatus()

  return (
    <>
      <div className="page-shell">
        <PageHeader
          eyebrow="Home"
          title="친구들의 소식과 내 이야기를 바로 확인하세요"
          subtitle="새 글을 남기고, 다른 사람의 근황을 보고, 내 스튜디오 상태도 한눈에 살펴볼 수 있습니다."
          actions={
            <Button
              variant="secondary"
              className="ui-button-icon-only"
              onClick={() => void loadFeed()}
              aria-label="피드 새로고침"
              title="피드 새로고침"
            >
              <img src={refreshIcon} alt="" aria-hidden="true" className="ui-button-icon" />
            </Button>
          }
        />
        <PostComposer
          isSubmitting={isSubmitting}
          error={composerError}
          onSubmit={handleCreatePost}
        />
        {isLoading ? (
          <LoadingState
            title="피드를 불러오는 중입니다"
            copy="최신 게시물과 추천 정보를 불러오고 있습니다."
          />
        ) : error ? (
          <EmptyState title="피드를 불러올 수 없어요" copy={error} />
        ) : posts.length === 0 ? (
          <EmptyState
            title="아직 게시물이 없어요"
            copy="첫 게시물을 남겨서 Home 피드의 리듬을 만들어보세요."
          />
        ) : (
          <FeedList posts={posts} />
        )}
      </div>
      <ContextPanel>
        <Card className="stack-md">
          <h2 className="section-title">{user.displayName}</h2>
          <div className="meta-line">@{user.username}</div>
          <div className="entity-title">
            <Chip tone="success">{user.visibility}</Chip>
            <Chip tone="accent">AWAY</Chip>
            {isDemo ? <Chip tone="warm">DEMO</Chip> : null}
          </div>
        </Card>
        <Card className="stack-md">
          <h2 className="section-title">내 스튜디오</h2>
          <Chip tone="success">{studioStatus.cloneVisibility}</Chip>
          <p className="muted-copy">
            내 클론과 보이스 준비 상태를 여기서 빠르게 확인할 수 있습니다.
          </p>
        </Card>
        <Card className="stack-md">
          <h2 className="section-title">추천 사람</h2>
          {suggestedUsers.map((user) => (
            <div key={user.userId} className="entity-title">
              <span>{user.displayName}</span>
              <span className="meta-line">@{user.username}</span>
            </div>
          ))}
        </Card>
      </ContextPanel>
    </>
  )
}
