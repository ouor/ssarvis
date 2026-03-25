import type { FormEvent } from 'react'
import type { LiveChatState, LiveDebateState } from '../types'
import { formatCloneName } from '../utils'

type LiveSessionPanelProps = {
  liveChat: LiveChatState | null
  liveDebate: LiveDebateState | null
  onChatSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onChatInputChange: (value: string) => void
  onChatSpeechToggle: () => void
  onShowClones: () => void
  onDebateStop: () => Promise<void>
}

function LiveSessionPanel({
  liveChat,
  liveDebate,
  onChatSubmit,
  onChatInputChange,
  onChatSpeechToggle,
  onShowClones,
  onDebateStop,
}: LiveSessionPanelProps) {
  return (
    <section className="live-grid">
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
              <button className="secondary-button" disabled={liveDebate.stopping} onClick={() => void onDebateStop()} type="button">
                {liveDebate.stopping ? '중단 중...' : '중단'}
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
            <strong>클론을 선택하면 이곳에서 모든 대화가 시작됩니다.</strong>
            <p>첫 번째 탭에서 클론 카드를 눌러 채팅이나 논쟁 흐름을 열어보세요.</p>
          </article>
        ) : null}
      </article>
    </section>
  )
}

export default LiveSessionPanel
