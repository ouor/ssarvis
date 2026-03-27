import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { apiBaseUrl, fetchJsonOrThrow } from '../api'
import type { FriendRequestSummary, FriendSummary, StudioTab, UserSearchResponse } from '../types'

type UseFriendWorkspaceOptions = {
  activeTab: StudioTab
  currentUserId: number
  onRelationshipsChanged?: () => Promise<void> | void
}

export function useFriendWorkspace({ activeTab, currentUserId, onRelationshipsChanged }: UseFriendWorkspaceOptions) {
  const [friends, setFriends] = useState<FriendSummary[]>([])
  const [receivedFriendRequests, setReceivedFriendRequests] = useState<FriendRequestSummary[]>([])
  const [sentFriendRequests, setSentFriendRequests] = useState<FriendRequestSummary[]>([])
  const [friendLoadError, setFriendLoadError] = useState('')
  const [friendSearchQuery, setFriendSearchQuery] = useState('')
  const [friendSearchResults, setFriendSearchResults] = useState<UserSearchResponse[]>([])
  const [friendSearchError, setFriendSearchError] = useState('')
  const [friendSearchLoading, setFriendSearchLoading] = useState(false)
  const [friendActionKey, setFriendActionKey] = useState<string | null>(null)

  useEffect(() => {
    setFriends([])
    setReceivedFriendRequests([])
    setSentFriendRequests([])
    setFriendLoadError('')
    setFriendSearchQuery('')
    setFriendSearchResults([])
    setFriendSearchError('')
    setFriendSearchLoading(false)
    setFriendActionKey(null)
  }, [currentUserId])

  useEffect(() => {
    if (activeTab !== 'friends') {
      return
    }

    const controller = new AbortController()
    void loadFriendData(controller.signal)

    return () => {
      controller.abort()
    }
  }, [activeTab, currentUserId])

  async function loadFriendData(signal?: AbortSignal) {
    try {
      setFriendLoadError('')
      const [friendsData, receivedData, sentData] = await Promise.all([
        fetchJsonOrThrow<FriendSummary[]>(`${apiBaseUrl}/api/friends`, '친구 목록을 불러오지 못했습니다.', { signal }),
        fetchJsonOrThrow<FriendRequestSummary[]>(`${apiBaseUrl}/api/friends/requests/received`, '받은 요청을 불러오지 못했습니다.', { signal }),
        fetchJsonOrThrow<FriendRequestSummary[]>(`${apiBaseUrl}/api/friends/requests/sent`, '보낸 요청을 불러오지 못했습니다.', { signal }),
      ])

      if (signal?.aborted) {
        return
      }

      setFriends(friendsData)
      setReceivedFriendRequests(receivedData)
      setSentFriendRequests(sentData)
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }
      setFriendLoadError(error instanceof Error ? error.message : '친구 정보를 불러오는 중 오류가 발생했습니다.')
    }
  }

  async function refreshAfterRelationshipChange() {
    await loadFriendData()
    await onRelationshipsChanged?.()
  }

  async function handleFriendSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const query = friendSearchQuery.trim()
    if (!query) {
      setFriendSearchResults([])
      setFriendSearchError('검색어를 입력해 주세요.')
      return
    }

    setFriendSearchLoading(true)
    setFriendSearchError('')
    try {
      const data = await fetchJsonOrThrow<UserSearchResponse[]>(
        `${apiBaseUrl}/api/friends/users/search?query=${encodeURIComponent(query)}`,
        '사용자 검색에 실패했습니다.'
      )
      setFriendSearchResults(data)
    } catch (error) {
      setFriendSearchError(error instanceof Error ? error.message : '사용자 검색 중 오류가 발생했습니다.')
    } finally {
      setFriendSearchLoading(false)
    }
  }

  async function sendFriendRequest(receiverUserId: number) {
    const actionKey = `send-${receiverUserId}`
    setFriendActionKey(actionKey)
    setFriendSearchError('')
    try {
      await fetchJsonOrThrow(
        `${apiBaseUrl}/api/friends/requests`,
        '친구 요청을 보내지 못했습니다.',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ receiverUserId }),
        }
      )
      await loadFriendData()
      setFriendSearchResults((current) => current.filter((user) => user.userId !== receiverUserId))
    } catch (error) {
      setFriendSearchError(error instanceof Error ? error.message : '친구 요청을 보내는 중 오류가 발생했습니다.')
    } finally {
      setFriendActionKey(null)
    }
  }

  async function acceptFriendRequest(requestId: number) {
    const actionKey = `accept-${requestId}`
    setFriendActionKey(actionKey)
    setFriendLoadError('')
    try {
      await fetchJsonOrThrow(`${apiBaseUrl}/api/friends/requests/${requestId}/accept`, '친구 요청을 수락하지 못했습니다.', {
        method: 'POST',
      })
      await refreshAfterRelationshipChange()
    } catch (error) {
      setFriendLoadError(error instanceof Error ? error.message : '친구 요청을 수락하는 중 오류가 발생했습니다.')
    } finally {
      setFriendActionKey(null)
    }
  }

  async function rejectFriendRequest(requestId: number) {
    const actionKey = `reject-${requestId}`
    setFriendActionKey(actionKey)
    setFriendLoadError('')
    try {
      await fetchJsonOrThrow(`${apiBaseUrl}/api/friends/requests/${requestId}/reject`, '친구 요청을 거절하지 못했습니다.', {
        method: 'POST',
      })
      await loadFriendData()
    } catch (error) {
      setFriendLoadError(error instanceof Error ? error.message : '친구 요청을 거절하는 중 오류가 발생했습니다.')
    } finally {
      setFriendActionKey(null)
    }
  }

  async function cancelFriendRequest(requestId: number) {
    const actionKey = `cancel-${requestId}`
    setFriendActionKey(actionKey)
    setFriendLoadError('')
    try {
      await fetchJsonOrThrow(`${apiBaseUrl}/api/friends/requests/${requestId}/cancel`, '친구 요청을 취소하지 못했습니다.', {
        method: 'POST',
      })
      await loadFriendData()
    } catch (error) {
      setFriendLoadError(error instanceof Error ? error.message : '친구 요청을 취소하는 중 오류가 발생했습니다.')
    } finally {
      setFriendActionKey(null)
    }
  }

  async function unfriend(friendUserId: number) {
    const actionKey = `unfriend-${friendUserId}`
    setFriendActionKey(actionKey)
    setFriendLoadError('')
    try {
      await fetchJsonOrThrow(`${apiBaseUrl}/api/friends/${friendUserId}`, '친구 해제를 완료하지 못했습니다.', {
        method: 'DELETE',
      })
      await refreshAfterRelationshipChange()
    } catch (error) {
      setFriendLoadError(error instanceof Error ? error.message : '친구 해제 중 오류가 발생했습니다.')
    } finally {
      setFriendActionKey(null)
    }
  }

  return {
    friends,
    receivedFriendRequests,
    sentFriendRequests,
    friendLoadError,
    friendSearchQuery,
    setFriendSearchQuery,
    friendSearchResults,
    friendSearchError,
    friendSearchLoading,
    friendActionKey,
    handleFriendSearchSubmit,
    sendFriendRequest,
    acceptFriendRequest,
    rejectFriendRequest,
    cancelFriendRequest,
    unfriend,
    loadFriendData,
  }
}
