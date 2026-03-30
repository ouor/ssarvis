import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { EmptyState } from '../../components/shared/EmptyState'
import { LoadingState } from '../../components/shared/LoadingState'
import { PageHeader } from '../../components/shared/PageHeader'
import { useAuth } from '../../hooks/useAuth'
import { deletePost, getPost, updatePost } from '../../features/feed/api'
import { FeedCard } from '../../features/feed/components/FeedCard'
import { toFeedPostViewModel } from '../../features/feed/mappers'
import type { FeedPostViewModel } from '../../features/feed/types'
import { getRequiredAccessToken, SessionRequiredError } from '../../lib/api/session'
import { getDemoPost, getDemoUser } from '../../lib/demo/adapters'
import { ROUTES } from '../../lib/constants/routes'

export function PostDetailPage() {
  const { postId } = useParams()
  const navigate = useNavigate()
  const { currentUser, isDemo } = useAuth()
  const [post, setPost] = useState<FeedPostViewModel | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const numericPostId = Number(postId)
  const currentUserId = (currentUser ?? getDemoUser()).userId

  const loadPost = useCallback(async () => {
    if (!Number.isFinite(numericPostId)) {
      setError('유효하지 않은 게시글 주소입니다.')
      setIsLoading(false)
      return
    }

    if (isDemo) {
      const demoPost = getDemoPost(numericPostId)
      setPost(demoPost)
      setError(demoPost ? null : '게시글을 찾을 수 없어요.')
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      const token = getRequiredAccessToken(
        '세션 정보가 없어 게시글을 불러올 수 없습니다.',
      )
      const response = await getPost(token, numericPostId)
      setPost(toFeedPostViewModel(response))
    } catch (loadError) {
      if (loadError instanceof SessionRequiredError) {
        setError(loadError.message)
      } else {
        setError('게시글을 불러오지 못했습니다.')
      }
    } finally {
      setIsLoading(false)
    }
  }, [isDemo, numericPostId])

  useEffect(() => {
    void loadPost()
  }, [loadPost])

  async function handleEditPost(postIdToEdit: number, content: string) {
    if (isDemo) {
      setPost((current) =>
        current && current.id === postIdToEdit
          ? {
              ...current,
              content,
            }
          : current,
      )
      return true
    }

    try {
      const token = getRequiredAccessToken(
        '세션 정보가 없어 게시글을 수정할 수 없습니다.',
      )
      const response = await updatePost(token, postIdToEdit, content)
      setPost(toFeedPostViewModel(response))
      return true
    } catch {
      setError('게시글 수정에 실패했습니다.')
      return false
    }
  }

  async function handleDeletePost(postIdToDelete: number) {
    if (isDemo) {
      navigate(ROUTES.home)
      return true
    }

    try {
      const token = getRequiredAccessToken(
        '세션 정보가 없어 게시글을 삭제할 수 없습니다.',
      )
      await deletePost(token, postIdToDelete)
      navigate(ROUTES.home)
      return true
    } catch {
      setError('게시글 삭제에 실패했습니다.')
      return false
    }
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Post"
        title="게시글"
        subtitle="게시글 내용을 확인하고, 내가 작성한 글이라면 바로 수정하거나 삭제할 수 있습니다."
      />
      {isLoading ? (
        <LoadingState
          title="게시글을 불러오는 중입니다"
          copy="게시글 내용과 작성자 정보를 가져오고 있습니다."
        />
      ) : error ? (
        <EmptyState title="게시글을 불러올 수 없어요" copy={error} />
      ) : post ? (
        <FeedCard
          post={post}
          canManage={post.author.userId === currentUserId}
          onEditPost={handleEditPost}
          onDeletePost={handleDeletePost}
        />
      ) : (
        <EmptyState title="게시글을 찾을 수 없어요" copy="다시 목록으로 돌아가 다른 게시글을 확인해보세요." />
      )}
    </div>
  )
}
