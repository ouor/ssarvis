import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import moreHorizontalIcon from '../../../assets/more-horizontal.svg'
import { Avatar } from '../../../components/ui/Avatar'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Textarea } from '../../../components/ui/Textarea'
import { ROUTES } from '../../../lib/constants/routes'
import type { FeedPostViewModel } from '../types'

type FeedCardProps = {
  post: FeedPostViewModel
  canManage?: boolean
  onEditPost?: (postId: number, content: string) => Promise<boolean> | boolean
  onDeletePost?: (postId: number) => Promise<boolean> | boolean
}

export function FeedCard({
  post,
  canManage = false,
  onEditPost,
  onDeletePost,
}: FeedCardProps) {
  const [isEditing, setIsEditing] = useState(false)
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const [value, setValue] = useState(post.content)
  const [isSaving, setIsSaving] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setValue(post.content)
  }, [post.content])

  async function handleSave() {
    if (!onEditPost) {
      return
    }

    const nextValue = value.trim()

    if (!nextValue) {
      setError('내용을 입력해주세요.')
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      const wasSuccessful = await onEditPost(post.id, nextValue)

      if (wasSuccessful) {
        setIsEditing(false)
        setIsMenuOpen(false)
      } else {
        setError('게시글 수정에 실패했습니다.')
      }
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDelete() {
    if (!onDeletePost) {
      return
    }

    setIsDeleting(true)
    setError(null)

    try {
      const wasSuccessful = await onDeletePost(post.id)

      if (!wasSuccessful) {
        setError('게시글 삭제에 실패했습니다.')
      } else {
        setIsMenuOpen(false)
      }
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <Card className="feed-card">
      <div className="entity-row">
        <Avatar name={post.author.displayName} />
        <div className="entity-meta stack-sm">
          <div className="entity-row">
            <div className="entity-meta stack-sm">
              <div className="entity-title">
                <strong>{post.author.displayName}</strong>
                <span className="meta-line">@{post.author.username}</span>
              </div>
              <Link
                to={ROUTES.postDetail.replace(':postId', String(post.id))}
                className="feed-card-link"
              >
                {post.postedAt}
              </Link>
            </div>
            {canManage ? (
              <div className="feed-card-menu">
                <Button
                  variant="secondary"
                  className="ui-button-icon-only"
                  aria-label="게시글 더보기"
                  title="게시글 더보기"
                  onClick={() => setIsMenuOpen((current) => !current)}
                >
                  <img
                    src={moreHorizontalIcon}
                    alt=""
                    aria-hidden="true"
                    className="ui-button-icon"
                  />
                </Button>
                {isMenuOpen ? (
                  <div className="feed-card-menu-panel">
                    <button
                      type="button"
                      className="feed-card-menu-item"
                      onClick={() => {
                        setIsEditing(true)
                        setIsMenuOpen(false)
                        setError(null)
                      }}
                    >
                      수정
                    </button>
                    <button
                      type="button"
                      className="feed-card-menu-item danger"
                      onClick={() => void handleDelete()}
                      disabled={isDeleting}
                    >
                      {isDeleting ? '삭제 중...' : '삭제'}
                    </button>
                  </div>
                ) : null}
              </div>
            ) : null}
          </div>
          {isEditing ? (
            <div className="stack-sm">
              <Textarea
                value={value}
                onChange={(event) => setValue(event.target.value)}
              />
              {error ? <p className="meta-line">{error}</p> : null}
              <div className="feed-card-actions">
                <Button
                  variant="secondary"
                  onClick={() => void handleSave()}
                  disabled={isSaving}
                >
                  {isSaving ? '저장 중...' : '저장'}
                </Button>
                <Button
                  variant="ghost"
                  onClick={() => {
                    setIsEditing(false)
                    setValue(post.content)
                    setError(null)
                  }}
                >
                  취소
                </Button>
              </div>
            </div>
          ) : (
            <p className="muted-copy">{post.content}</p>
          )}
        </div>
      </div>
    </Card>
  )
}
