import { useCallback, useEffect, useRef, useState } from 'react'
import { EmptyState } from '../../components/shared/EmptyState'
import { LoadingState } from '../../components/shared/LoadingState'
import { PageHeader } from '../../components/shared/PageHeader'
import { Card } from '../../components/ui/Card'
import { useAuth } from '../../hooks/useAuth'
import {
  followUser,
  getFollowingUsers,
  searchUsers,
  unfollowUser,
} from '../../features/people-search/api'
import { PeopleResultList } from '../../features/people-search/components/PeopleResultList'
import { PeopleSearchBar } from '../../features/people-search/components/PeopleSearchBar'
import type { ProfileSummary } from '../../features/profile-settings/types'
import {
  getRequiredAccessToken,
  SessionRequiredError,
} from '../../lib/api/session'
import { getDemoPeopleResults } from '../../lib/demo/adapters'

export function PeoplePage() {
  const { isDemo } = useAuth()
  const [query, setQuery] = useState('')
  const [users, setUsers] = useState<ProfileSummary[]>(getDemoPeopleResults)
  const [isLoading, setIsLoading] = useState(!isDemo)
  const [busyUserId, setBusyUserId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const loadRequestIdRef = useRef(0)

  const loadUsers = useCallback(async () => {
    const requestId = ++loadRequestIdRef.current

    if (isDemo) {
      if (requestId === loadRequestIdRef.current) {
        setUsers(
          getDemoPeopleResults(),
        )
        setIsLoading(false)
        setError(null)
      }
      return
    }

    if (requestId === loadRequestIdRef.current) {
      setIsLoading(true)
      setError(null)
    }

    try {
      const token = getRequiredAccessToken(
        '세션 정보가 없어 사람 목록을 불러올 수 없습니다.',
      )
      const response = query.trim()
        ? await searchUsers(token, query.trim())
        : await getFollowingUsers(token)

      if (requestId === loadRequestIdRef.current) {
        setUsers(response)
      }
    } catch (error) {
      if (requestId === loadRequestIdRef.current) {
        if (error instanceof SessionRequiredError) {
          setError(error.message)
        } else {
          setError('사람 목록을 불러오지 못했습니다.')
        }
      }
    } finally {
      if (requestId === loadRequestIdRef.current) {
        setIsLoading(false)
      }
    }
  }, [isDemo, query])

  useEffect(() => {
    void loadUsers()
  }, [loadUsers])

  async function handleToggleFollow(user: ProfileSummary) {
    if (isDemo) {
      setUsers((current) =>
        current.map((item) =>
          item.userId === user.userId
            ? { ...item, following: !item.following }
            : item,
        ),
      )
      return
    }

    setBusyUserId(user.userId)
    setError(null)

    try {
      const token = getRequiredAccessToken(
        '세션 정보가 없어 팔로우 상태를 바꿀 수 없습니다.',
      )
      if (user.following) {
        await unfollowUser(token, user.userId)
        setUsers((current) =>
          current.map((item) =>
            item.userId === user.userId
              ? { ...item, following: false }
              : item,
          ),
        )
      } else {
        const response = await followUser(token, user.userId)
        setUsers((current) =>
          current.map((item) =>
            item.userId === user.userId ? response : item,
          ),
        )
      }
    } catch (error) {
      if (error instanceof SessionRequiredError) {
        setError(error.message)
      } else {
        setError('팔로우 상태 변경에 실패했습니다.')
      }
    } finally {
      setBusyUserId(null)
    }
  }

  return (
    <div className="page-shell">
      <PageHeader
        eyebrow="People"
        title="관계를 넓히되, 프로필 공개 정책은 분명하게"
        subtitle="검색 경험은 부드럽게, 결과 카드는 명확하게 설계해 공개 프로필과 비공개 프로필의 차이를 바로 인지할 수 있게 합니다."
      />
      <Card className="stack-md">
        <h2 className="section-title">사람 찾기</h2>
        <PeopleSearchBar value={query} onChange={setQuery} />
        {error ? <p className="meta-line">{error}</p> : null}
      </Card>
      {isLoading ? (
        <LoadingState
          title="사람 목록을 불러오는 중입니다"
          copy="검색어와 팔로우 관계를 기준으로 탐색 가능한 사용자를 정리하고 있어요."
        />
      ) : users.length === 0 ? (
        <EmptyState
          title={query.trim() ? '검색 결과가 없어요' : '아직 팔로우한 사람이 없어요'}
          copy={
            query.trim()
              ? '다른 이름이나 아이디로 다시 검색해보세요.'
              : '검색어를 입력해 새로운 사람을 찾아보세요.'
          }
        />
      ) : (
        <PeopleResultList
          users={users}
          busyUserId={busyUserId}
          onToggleFollow={handleToggleFollow}
        />
      )}
    </div>
  )
}
