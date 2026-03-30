import { useCallback, useEffect, useState } from 'react'
import { EmptyState } from '../../components/shared/EmptyState'
import { LoadingState } from '../../components/shared/LoadingState'
import { PageHeader } from '../../components/shared/PageHeader'
import { useAuth } from '../../hooks/useAuth'
import { deletePost, getMyPosts, updatePost } from '../../features/feed/api'
import { toFeedPostViewModel } from '../../features/feed/mappers'
import type { FeedPostViewModel } from '../../features/feed/types'
import { AutoReplyCard } from '../../features/profile-settings/components/AutoReplyCard'
import { AccountActionsCard } from '../../features/profile-settings/components/AccountActionsCard'
import { ProfileBasicsCard } from '../../features/profile-settings/components/ProfileBasicsCard'
import { ProfileHeader } from '../../features/profile-settings/components/ProfileHeader'
import { ProfilePostsCard } from '../../features/profile-settings/components/ProfilePostsCard'
import { VisibilityToggle } from '../../features/profile-settings/components/VisibilityToggle'
import {
  getAutoReplySettings,
  getMyProfile,
  updateAutoReplySettings,
  updateDisplayName,
  updateVisibility,
} from '../../features/profile-settings/api'
import type {
  AutoReplySettings,
  ProfileSummary,
} from '../../features/profile-settings/types'
import {
  getRequiredAccessToken,
  SessionRequiredError,
} from '../../lib/api/session'
import {
  getDemoAutoReplySettings,
  getDemoProfilePosts,
  getDemoProfileSummary,
  getDemoUser,
} from '../../lib/demo/adapters'

export function ProfilePage() {
  return <OwnProfileContent />
}

export function OwnProfileContent() {
  const { currentUser, isDemo, syncCurrentUser } = useAuth()
  const [profile, setProfile] = useState<ProfileSummary | null>(
    isDemo ? getDemoProfileSummary() : null,
  )
  const [autoReply, setAutoReply] = useState<AutoReplySettings>(
    getDemoAutoReplySettings(),
  )
  const [posts, setPosts] = useState<FeedPostViewModel[]>(() =>
    getDemoProfilePosts(getDemoUser().userId),
  )
  const [isLoading, setIsLoading] = useState(!isDemo)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [displayNameError, setDisplayNameError] = useState<string | null>(null)
  const [visibilityError, setVisibilityError] = useState<string | null>(null)
  const [autoReplyError, setAutoReplyError] = useState<string | null>(null)
  const [isSavingDisplayName, setIsSavingDisplayName] = useState(false)
  const [isSavingVisibility, setIsSavingVisibility] = useState(false)
  const [isSavingAutoReply, setIsSavingAutoReply] = useState(false)

  const user = profile ??
    (currentUser
      ? {
          userId: currentUser.userId,
          username: currentUser.username,
          displayName: currentUser.displayName,
          visibility: currentUser.visibility,
          me: true,
          following: false,
        }
      : {
          userId: getDemoUser().userId,
          username: getDemoUser().username,
          displayName: getDemoUser().displayName,
          visibility: getDemoUser().visibility,
          me: true,
          following: false,
        })

  const loadProfileData = useCallback(async () => {
    if (isDemo) {
      setLoadError(null)
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setLoadError(null)

    try {
      const token = getRequiredAccessToken(
        '세션 정보가 없어 프로필을 불러올 수 없습니다.',
      )
      const [profileResponse, autoReplyResponse, postsResponse] =
        await Promise.all([
          getMyProfile(token),
          getAutoReplySettings(token),
          getMyPosts(token),
        ])

      setProfile(profileResponse)
      setAutoReply(autoReplyResponse)
      setPosts(postsResponse.map(toFeedPostViewModel))
      syncCurrentUser({
        userId: profileResponse.userId,
        username: profileResponse.username,
        displayName: profileResponse.displayName,
        visibility: profileResponse.visibility,
      })
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setLoadError(error.message)
      } else {
        setLoadError('프로필 정보를 불러오지 못했습니다.')
      }
    } finally {
      setIsLoading(false)
    }
  }, [isDemo, syncCurrentUser])

  useEffect(() => {
    void loadProfileData()
  }, [loadProfileData])

  async function handleDisplayNameSave(displayName: string) {
    if (!displayName) {
      setDisplayNameError('표시 이름을 입력해주세요.')
      return false
    }

    if (displayName.length > 100) {
      setDisplayNameError('표시 이름은 100자 이하로 입력해주세요.')
      return false
    }

    if (isDemo) {
      setProfile((current) =>
        current ? { ...current, displayName } : current,
      )
      syncCurrentUser({
        userId: user.userId,
        username: user.username,
        displayName,
        visibility: user.visibility,
      })
      setDisplayNameError(null)
      return true
    }

    setIsSavingDisplayName(true)
    setDisplayNameError(null)

    try {
      const token = getRequiredAccessToken(
        '세션이 없어 표시 이름을 저장할 수 없습니다.',
      )
      const response = await updateDisplayName(token, displayName)
      setProfile(response)
      syncCurrentUser({
        userId: response.userId,
        username: response.username,
        displayName: response.displayName,
        visibility: response.visibility,
      })
      return true
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setDisplayNameError(error.message)
      } else {
        setDisplayNameError('표시 이름 저장에 실패했습니다.')
      }
      return false
    } finally {
      setIsSavingDisplayName(false)
    }
  }

  async function handleVisibilityChange(nextVisibility: 'PUBLIC' | 'PRIVATE') {
    if (nextVisibility === user.visibility) {
      return
    }

    if (isDemo) {
      setProfile((current) =>
        current ? { ...current, visibility: nextVisibility } : current,
      )
      syncCurrentUser({
        userId: user.userId,
        username: user.username,
        displayName: user.displayName,
        visibility: nextVisibility,
      })
      setVisibilityError(null)
      return
    }

    setIsSavingVisibility(true)
    setVisibilityError(null)

    try {
      const token = getRequiredAccessToken(
        '세션이 없어 공개성을 바꿀 수 없습니다.',
      )
      const response = await updateVisibility(token, nextVisibility)
      setProfile(response)
      syncCurrentUser({
        userId: response.userId,
        username: response.username,
        displayName: response.displayName,
        visibility: response.visibility,
      })
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setVisibilityError(error.message)
      } else {
        setVisibilityError('공개성 변경에 실패했습니다.')
      }
    } finally {
      setIsSavingVisibility(false)
    }
  }

  async function handleAutoReplyChange(mode: 'ALWAYS' | 'AWAY' | 'OFF') {
    if (mode === autoReply.mode) {
      return
    }

    if (isDemo) {
      setAutoReply((current) => ({ ...current, mode }))
      setAutoReplyError(null)
      return
    }

    setIsSavingAutoReply(true)
    setAutoReplyError(null)

    try {
      const token = getRequiredAccessToken(
        '세션이 없어 자동응답을 바꿀 수 없습니다.',
      )
      const response = await updateAutoReplySettings(token, mode)
      setAutoReply(response)
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setAutoReplyError(error.message)
      } else {
        setAutoReplyError('자동응답 설정 변경에 실패했습니다.')
      }
    } finally {
      setIsSavingAutoReply(false)
    }
  }

  async function handleEditPost(postId: number, content: string) {
    if (isDemo) {
      setPosts((current) =>
        current.map((post) => (post.id === postId ? { ...post, content } : post)),
      )
      return true
    }

    try {
      const token = getRequiredAccessToken(
        '세션이 없어 게시글을 수정할 수 없습니다.',
      )
      const response = await updatePost(token, postId, content)
      setPosts((current) =>
        current.map((post) =>
          post.id === postId ? toFeedPostViewModel(response) : post,
        ),
      )
      return true
    } catch {
      setLoadError('게시글 수정에 실패했습니다.')
      return false
    }
  }

  async function handleDeletePost(postId: number) {
    if (isDemo) {
      setPosts((current) => current.filter((post) => post.id !== postId))
      return true
    }

    try {
      const token = getRequiredAccessToken(
        '세션이 없어 게시글을 삭제할 수 없습니다.',
      )
      await deletePost(token, postId)
      setPosts((current) => current.filter((post) => post.id !== postId))
      return true
    } catch {
      setLoadError('게시글 삭제에 실패했습니다.')
      return false
    }
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Profile"
        title="내 프로필"
        subtitle="내 정보와 공개 범위를 정리하고, 작성한 게시물을 한곳에서 확인하세요."
      />
      {isLoading ? (
        <LoadingState
          title="프로필을 불러오고 있어요"
          copy="내 정보와 게시물을 준비하고 있습니다."
        />
      ) : loadError ? (
        <EmptyState title="프로필을 불러올 수 없어요" copy={loadError} />
      ) : (
        <>
          <ProfileHeader user={user} />
          <div className="two-col">
            <ProfileBasicsCard
              displayName={user.displayName}
              isSubmitting={isSavingDisplayName}
              error={displayNameError}
              onSubmit={handleDisplayNameSave}
            />
            <VisibilityToggle
              visibility={user.visibility}
              isSubmitting={isSavingVisibility}
              error={visibilityError}
              onChange={handleVisibilityChange}
            />
          </div>
          <div className="two-col">
            <AutoReplyCard
              mode={autoReply.mode}
              lastActivityAt={autoReply.lastActivityAt}
              isSubmitting={isSavingAutoReply}
              error={autoReplyError}
              onChange={handleAutoReplyChange}
            />
            <AccountActionsCard />
          </div>
          <ProfilePostsCard
            posts={posts}
            currentUserId={user.userId}
            onEditPost={handleEditPost}
            onDeletePost={handleDeletePost}
            emptyCopy="아직 작성한 게시물이 없습니다."
          />
        </>
      )}
    </div>
  )
}
