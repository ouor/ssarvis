import { useCallback, useEffect, useState } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import { ApiError } from '../../lib/api/client'
import { EmptyState } from '../../components/shared/EmptyState'
import { LoadingState } from '../../components/shared/LoadingState'
import { PageHeader } from '../../components/shared/PageHeader'
import { useAuth } from '../../hooks/useAuth'
import { getProfilePosts, getPublicProfilePosts } from '../../features/feed/api'
import { toFeedPostViewModel } from '../../features/feed/mappers'
import type { FeedPostViewModel } from '../../features/feed/types'
import {
  getProfileByUsername,
  getPublicProfileByUsername,
} from '../../features/profile-settings/api'
import { ProfileHeader } from '../../features/profile-settings/components/ProfileHeader'
import { ProfilePostsCard } from '../../features/profile-settings/components/ProfilePostsCard'
import type { ProfileSummary } from '../../features/profile-settings/types'
import { ROUTES } from '../../lib/constants/routes'
import { getRequiredAccessToken, SessionRequiredError } from '../../lib/api/session'
import { getDemoProfilePosts, getDemoSuggestedUsers, getDemoUser } from '../../lib/demo/adapters'
import { OwnProfileContent } from './ProfilePage'

export function UserProfilePage() {
  const { username } = useParams()
  const { currentUser, isAuthenticated, isDemo } = useAuth()
  const [profile, setProfile] = useState<ProfileSummary | null>(null)
  const [posts, setPosts] = useState<FeedPostViewModel[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  if (!username) {
    return <Navigate to={ROUTES.home} replace />
  }

  const isOwner =
    (isAuthenticated || isDemo) &&
    (currentUser ?? getDemoUser()).username === username

  if (isOwner) {
    return <OwnProfileContent />
  }

  const loadProfile = useCallback(async () => {
    if (isDemo) {
      const demoUser = getDemoSuggestedUsers().find((user) => user.username === username)
      setProfile(
        demoUser
          ? {
              ...demoUser,
              me: false,
              following: false,
            }
          : null,
      )
      setPosts(demoUser ? getDemoProfilePosts(demoUser.userId) : [])
      setError(demoUser ? null : '프로필을 찾을 수 없어요.')
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(null)

    try {
      if (isAuthenticated) {
        const token = getRequiredAccessToken(
          '세션 정보가 없어 프로필을 불러올 수 없습니다.',
        )
        const profileResponse = await getProfileByUsername(token, username)
        const postsResponse = await getProfilePosts(token, profileResponse.userId)

        setProfile(profileResponse)
        setPosts(postsResponse.map(toFeedPostViewModel))
        return
      }

      const [profileResponse, postsResponse] = await Promise.all([
        getPublicProfileByUsername(username),
        getPublicProfilePosts(username),
      ])

      setProfile(profileResponse)
      setPosts(postsResponse.map(toFeedPostViewModel))
    } catch (loadError) {
      if (loadError instanceof SessionRequiredError) {
        setError(loadError.message)
      } else if (loadError instanceof ApiError && loadError.status === 403) {
        setError('비공개 계정이라 로그인 후에만 확인할 수 있어요.')
      } else {
        setError('프로필을 불러오지 못했습니다.')
      }
    } finally {
      setIsLoading(false)
    }
  }, [isAuthenticated, isDemo, username])

  useEffect(() => {
    void loadProfile()
  }, [loadProfile])

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="Profile"
        title={`${profile?.displayName ?? username}님의 프로필`}
        subtitle="사용자 정보와 게시물을 한눈에 확인하세요."
      />
      {isLoading ? (
        <LoadingState
          title="프로필을 불러오고 있어요"
          copy="사용자 정보와 게시물을 준비하고 있습니다."
        />
      ) : error ? (
        <EmptyState title="프로필을 불러올 수 없어요" copy={error} />
      ) : profile ? (
        <>
          <ProfileHeader
            user={{
              userId: profile.userId,
              username: profile.username,
              displayName: profile.displayName,
              visibility: profile.visibility,
            }}
          />
          <ProfilePostsCard
            posts={posts}
            currentUserId={currentUser?.userId}
            emptyCopy="아직 공개된 게시물이 없습니다."
          />
        </>
      ) : (
        <EmptyState title="프로필을 찾을 수 없어요" copy="다른 사용자를 확인해보세요." />
      )}
    </div>
  )
}
