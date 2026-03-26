import type { FormEvent } from 'react'
import type { ChatConversationSummary, CurrentUser, DebateSessionSummary, LiveChatState, LiveDebateState } from '../types'
import { formatCloneName } from '../utils'

type LiveSessionPanelProps = {
  currentUser: CurrentUser
  liveChat: LiveChatState | null
  liveDebate: LiveDebateState | null
  chatHistory: ChatConversationSummary[]
  chatHistoryLoadError: string
  debateHistory: DebateSessionSummary[]
  debateHistoryLoadError: string
  onChatSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onChatInputChange: (value: string) => void
  onChatSpeechToggle: () => void
  onOpenChatHistory: (conversationId: number) => Promise<void>
  onOpenDebateHistory: (debateSessionId: number) => Promise<void>
  onShowClones: () => void
  onDebateExit: () => Promise<void>
}

function LiveSessionPanel({
  currentUser,
  liveChat,
  liveDebate,
  chatHistory,
  chatHistoryLoadError,
  debateHistory,
  debateHistoryLoadError,
  onChatSubmit,
  onChatInputChange,
  onChatSpeechToggle,
  onOpenChatHistory,
  onOpenDebateHistory,
  onShowClones,
  onDebateExit,
}: LiveSessionPanelProps) {
  return (
    <section className="live-grid">
      <aside className="history-sidebar">
        <article className="history-panel">
          <div className="panel-heading">
            <div>
              <p className="panel-kicker">Chat History</p>
              <h2>이전 채팅</h2>
            </div>
          </div>
          <div className="history-list">
            {chatHistoryLoadError ? <p className="inline-error">{chatHistoryLoadError}</p> : null}
            {chatHistory.map((conversation) => (
              <button
                key={conversation.conversationId}
                className={`history-item${liveChat?.conversationId === conversation.conversationId ? ' history-item-active' : ''}`}
                onClick={() => void onOpenChatHistory(conversation.conversationId)}
                type="button"
              >
                <strong>{conversation.cloneAlias}</strong>
                <span>{new Date(conversation.createdAt).toLocaleString('ko-KR')}</span>
                <p>{conversation.latestMessagePreview || '메시지가 아직 없습니다.'}</p>
              </button>
            ))}
            {chatHistory.length === 0 && !chatHistoryLoadError ? (
              <article className="empty-card empty-inline">
                <strong>저장된 채팅이 없습니다.</strong>
                <p>새 채팅을 시작하면 이곳에 다시 열 수 있는 기록이 생깁니다.</p>
              </article>
            ) : null}
          </div>
        </article>

        <article className="history-panel">
          <div className="panel-heading">
            <div>
              <p className="panel-kicker">Debate History</p>
              <h2>이전 논쟁</h2>
            </div>
          </div>
          <div className="history-list">
            {debateHistoryLoadError ? <p className="inline-error">{debateHistoryLoadError}</p> : null}
            {debateHistory.map((debate) => (
              <button
                key={debate.debateSessionId}
                className={`history-item${liveDebate?.debateSessionId === debate.debateSessionId ? ' history-item-active' : ''}`}
                onClick={() => void onOpenDebateHistory(debate.debateSessionId)}
                type="button"
              >
                <strong>{debate.cloneAAlias} vs {debate.cloneBAlias}</strong>
                <span>{new Date(debate.createdAt).toLocaleString('ko-KR')}</span>
                <p>{debate.topic}</p>
              </button>
            ))}
            {debateHistory.length === 0 && !debateHistoryLoadError ? (
              <article className="empty-card empty-inline">
                <strong>저장된 논쟁이 없습니다.</strong>
                <p>논쟁을 시작하면 이곳에서 지난 턴을 다시 볼 수 있습니다.</p>
              </article>
            ) : null}
          </div>
        </article>
      </aside>

      <article className="live-panel">
        <div className="panel-heading">
          <div>
            <p className="panel-kicker">Live Session</p>
            <h2>
              {liveChat
                ? `${formatCloneName(liveChat.clone)}와 대화 중`
                : liveDebate
                  ? `${formatCloneName(liveDebate.cloneA)} vs ${formatCloneName(liveDebate.cloneB)}`
                  : '아직 시작된 세션이 없습니다'}
            </h2>
          </div>
        </div>

        {liveChat ? (
          <div className="conversation-shell">
            <div className="conversation-log">
              {liveChat.messages.length === 0 ? (
                <article className="empty-card empty-inline">
                  <strong>첫 메시지를 보내면 대화가 시작됩니다.</strong>
                  <p>선택한 클론과 목소리는 그대로 유지되고, 이후부터는 최근 대화 컨텍스트만 이어집니다.</p>
                </article>
              ) : null}
              {liveChat.messages.map((message, index) => (
                <article
                  key={`${message.role}-${index}-${message.content.slice(0, 24)}`}
                  className={message.role === 'user' ? 'bubble bubble-user' : 'bubble bubble-assistant'}
                >
                  <span className="bubble-role">{message.role === 'user' ? '나' : formatCloneName(liveChat.clone)}</span>
                  <p>{message.content}</p>
                  {message.ttsAudioDataUrl ? <audio controls src={message.ttsAudioDataUrl} /> : null}
                </article>
              ))}
            </div>

            <form className="composer" onSubmit={onChatSubmit}>
              <label className="composer-label" htmlFor="chat-input">
                메시지
              </label>
              <textarea
                id="chat-input"
                className="composer-input"
                onChange={(event) => onChatInputChange(event.target.value)}
                placeholder="클론에게 말을 걸어보세요."
                rows={4}
                value={liveChat.input}
              />
              {liveChat.error ? <p className="inline-error">{liveChat.error}</p> : null}
              {liveChat.speechError ? <p className="inline-error">{liveChat.speechError}</p> : null}
              <div className="composer-actions">
                <button className="secondary-button" onClick={onShowClones} type="button">
                  다른 클론 보기
                </button>
                <div className="composer-send-actions">
                  <button
                    aria-label={liveChat.speechListening ? '음성 인식 중지' : '음성 인식 시작'}
                    className={`mic-button${liveChat.speechListening ? ' mic-button-listening' : ''}`}
                    disabled={!liveChat.speechSupported || liveChat.submitting}
                    onClick={onChatSpeechToggle}
                    title={liveChat.speechSupported ? '음성으로 입력하기' : '이 브라우저는 음성 인식을 지원하지 않습니다.'}
                    type="button"
                  >
                    마이크
                  </button>
                  <button className="primary-button" disabled={liveChat.submitting} type="submit">
                    {liveChat.submitting ? '응답 생성 중...' : '보내기'}
                  </button>
                </div>
              </div>
            </form>
          </div>
        ) : null}

        {liveDebate ? (
          <div className="conversation-shell">
            <div className="debate-summary">
              <span>{liveDebate.topic}</span>
              <button className="secondary-button" onClick={() => void onDebateExit()} type="button">
                종료
              </button>
            </div>
            <div className="conversation-log">
              {liveDebate.turns.length === 0 ? (
                <article className="empty-card empty-inline">
                  <strong>첫 발언을 기다리는 중입니다.</strong>
                  <p>클론 A의 첫 응답이 도착하면 자동으로 재생이 시작됩니다.</p>
                </article>
              ) : null}
              {liveDebate.turns.map((turn) => {
                const speakerName =
                  turn.speaker === 'CLONE_A' ? formatCloneName(liveDebate.cloneA) : formatCloneName(liveDebate.cloneB)

                return (
                  <article key={turn.turnIndex} className="debate-turn-card">
                    <div className="debate-turn-topline">
                      <strong>{speakerName}</strong>
                      <span>{turn.turnIndex}턴</span>
                    </div>
                    <p>{turn.content}</p>
                    {turn.ttsAudioDataUrl ? <audio controls src={turn.ttsAudioDataUrl} /> : null}
                  </article>
                )
              })}
            </div>
            {liveDebate.error ? <p className="inline-error">{liveDebate.error}</p> : null}
          </div>
        ) : null}

        {!liveChat && !liveDebate ? (
          <article className="empty-card live-empty">
            <strong>{currentUser.displayName}님의 클론을 선택하면 이곳에서 세션이 시작됩니다.</strong>
            <p>내 계정의 클론이나 공개 클론으로 새 세션을 시작하거나, 왼쪽 기록 목록에서 이전 채팅과 논쟁을 다시 열어보세요.</p>
          </article>
        ) : null}
      </article>
    </section>
  )
}

export default LiveSessionPanel
