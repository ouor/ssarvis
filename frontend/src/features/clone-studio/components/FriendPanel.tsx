import type { ChangeEvent, FormEvent } from 'react'
import type { FriendRequestSummary, FriendSummary, UserSearchResponse } from '../types'

type FriendPanelProps = {
  friendLoadError: string
  friendSearchError: string
  friendSearchLoading: boolean
  friendSearchQuery: string
  friendSearchResults: UserSearchResponse[]
  friends: FriendSummary[]
  friendActionKey: string | null
  receivedRequests: FriendRequestSummary[]
  sentRequests: FriendRequestSummary[]
  onAcceptRequest: (requestId: number) => Promise<void>
  onCancelRequest: (requestId: number) => Promise<void>
  onRejectRequest: (requestId: number) => Promise<void>
  onSearchQueryChange: (value: string) => void
  onSearchSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onSendRequest: (userId: number) => Promise<void>
  onUnfriend: (friendUserId: number) => Promise<void>
}

function FriendPanel({
  friendLoadError,
  friendSearchError,
  friendSearchLoading,
  friendSearchQuery,
  friendSearchResults,
  friends,
  friendActionKey,
  receivedRequests,
  sentRequests,
  onAcceptRequest,
  onCancelRequest,
  onRejectRequest,
  onSearchQueryChange,
  onSearchSubmit,
  onSendRequest,
  onUnfriend,
}: FriendPanelProps) {
  return (
    <section className="friend-grid">
      <article className="history-panel friend-search-panel">
        <div className="panel-heading">
          <div>
            <p className="panel-kicker">Friend Finder</p>
            <h2>사용자 검색</h2>
          </div>
        </div>
        <form className="composer" onSubmit={(event) => void onSearchSubmit(event)}>
          <label className="composer-label" htmlFor="friend-search-input">
            표시명 또는 아이디
          </label>
          <input
            id="friend-search-input"
            className="composer-input"
            onChange={(event: ChangeEvent<HTMLInputElement>) => onSearchQueryChange(event.target.value)}
            placeholder="예: miso 또는 미소"
            value={friendSearchQuery}
          />
          <div className="composer-actions">
            <span className="asset-owner">친구 요청을 보내고, 상대가 수락하면 비공개 클론과 목소리를 사용할 수 있습니다.</span>
            <button className="primary-button" disabled={friendSearchLoading} type="submit">
              {friendSearchLoading ? '검색 중...' : '사용자 검색'}
            </button>
          </div>
        </form>
        {friendSearchError ? <p className="panel-error">{friendSearchError}</p> : null}
        <div className="history-list">
          {friendSearchResults.length === 0 ? (
            <div className="empty-card empty-inline">
              <strong>검색 결과가 없습니다.</strong>
              <p>검색어를 입력하고 사용자 검색을 눌러 친구를 찾아보세요.</p>
            </div>
          ) : (
            friendSearchResults.map((user) => {
              const actionKey = `send-${user.userId}`
              return (
                <div key={user.userId} className="friend-item-card">
                  <div>
                    <strong>{user.displayName}</strong>
                    <span>@{user.username}</span>
                  </div>
                  <button
                    className="secondary-button"
                    disabled={friendActionKey === actionKey}
                    onClick={() => void onSendRequest(user.userId)}
                    type="button"
                  >
                    {friendActionKey === actionKey ? '요청 중...' : '친구 요청'}
                  </button>
                </div>
              )
            })
          )}
        </div>
      </article>

      <div className="friend-panel-stack">
        <article className="history-panel">
          <div className="panel-heading">
            <div>
              <p className="panel-kicker">Incoming</p>
              <h2>받은 요청</h2>
            </div>
          </div>
          {friendLoadError ? <p className="panel-error">{friendLoadError}</p> : null}
          <div className="history-list">
            {receivedRequests.length === 0 ? (
              <div className="empty-card empty-inline">
                <strong>받은 친구 요청이 없습니다.</strong>
                <p>새 요청이 들어오면 여기에서 수락하거나 거절할 수 있습니다.</p>
              </div>
            ) : (
              receivedRequests.map((request) => {
                const acceptKey = `accept-${request.friendRequestId}`
                const rejectKey = `reject-${request.friendRequestId}`
                return (
                  <div key={request.friendRequestId} className="friend-item-card friend-item-card-stack">
                    <div>
                      <strong>{request.requester.displayName}</strong>
                      <span>@{request.requester.username}</span>
                    </div>
                    <div className="friend-item-actions">
                      <button
                        className="primary-button"
                        disabled={friendActionKey === acceptKey || friendActionKey === rejectKey}
                        onClick={() => void onAcceptRequest(request.friendRequestId)}
                        type="button"
                      >
                        {friendActionKey === acceptKey ? '수락 중...' : '수락'}
                      </button>
                      <button
                        className="secondary-button"
                        disabled={friendActionKey === acceptKey || friendActionKey === rejectKey}
                        onClick={() => void onRejectRequest(request.friendRequestId)}
                        type="button"
                      >
                        {friendActionKey === rejectKey ? '거절 중...' : '거절'}
                      </button>
                    </div>
                  </div>
                )
              })
            )}
          </div>
        </article>

        <article className="history-panel">
          <div className="panel-heading">
            <div>
              <p className="panel-kicker">Outgoing</p>
              <h2>보낸 요청</h2>
            </div>
          </div>
          <div className="history-list">
            {sentRequests.length === 0 ? (
              <div className="empty-card empty-inline">
                <strong>보낸 친구 요청이 없습니다.</strong>
                <p>검색 결과에서 친구 요청을 보내면 여기에서 상태를 추적할 수 있습니다.</p>
              </div>
            ) : (
              sentRequests.map((request) => {
                const cancelKey = `cancel-${request.friendRequestId}`
                return (
                  <div key={request.friendRequestId} className="friend-item-card">
                    <div>
                      <strong>{request.receiver.displayName}</strong>
                      <span>@{request.receiver.username}</span>
                    </div>
                    <button
                      className="secondary-button"
                      disabled={friendActionKey === cancelKey}
                      onClick={() => void onCancelRequest(request.friendRequestId)}
                      type="button"
                    >
                      {friendActionKey === cancelKey ? '취소 중...' : '요청 취소'}
                    </button>
                  </div>
                )
              })
            )}
          </div>
        </article>

        <article className="history-panel">
          <div className="panel-heading">
            <div>
              <p className="panel-kicker">Connected</p>
              <h2>친구 목록</h2>
            </div>
          </div>
          <div className="history-list">
            {friends.length === 0 ? (
              <div className="empty-card empty-inline">
                <strong>아직 친구가 없습니다.</strong>
                <p>친구가 생기면 비공개 클론과 목소리도 함께 사용할 수 있습니다.</p>
              </div>
            ) : (
              friends.map((friend) => {
                const unfriendKey = `unfriend-${friend.user.userId}`
                return (
                  <div key={friend.user.userId} className="friend-item-card">
                    <div>
                      <strong>{friend.user.displayName}</strong>
                      <span>@{friend.user.username}</span>
                    </div>
                    <button
                      className="secondary-button"
                      disabled={friendActionKey === unfriendKey}
                      onClick={() => void onUnfriend(friend.user.userId)}
                      type="button"
                    >
                      {friendActionKey === unfriendKey ? '해제 중...' : '친구 해제'}
                    </button>
                  </div>
                )
              })
            )}
          </div>
        </article>
      </div>
    </section>
  )
}

export default FriendPanel
