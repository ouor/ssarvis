type ChatMessage = {
  role: 'user' | 'assistant'
  content: string
}

type ResultPanelProps = {
  chatError: string
  chatInput: string
  chatMessages: ChatMessage[]
  chatSubmitting: boolean
  conversationId: number | null
  error: string
  onChatInputChange: (value: string) => void
  onChatSubmit: React.FormEventHandler<HTMLFormElement>
  systemPrompt: string
}

function ResultPanel({
  chatError,
  chatInput,
  chatMessages,
  chatSubmitting,
  conversationId,
  error,
  onChatInputChange,
  onChatSubmit,
  systemPrompt,
}: ResultPanelProps) {
  return (
    <aside className="result-panel">
      <div className="panel-header">
        <div>
          <p className="section-label">Result</p>
          <h2>생성된 시스템 프롬프트</h2>
        </div>
      </div>

      {error ? <div className="feedback error">{error}</div> : null}

      {systemPrompt ? (
        <div className="result-stack">
          <div className="prompt-output">
            <pre>{systemPrompt}</pre>
          </div>

          <section className="chat-panel">
            <div className="chat-header">
              <div>
                <p className="section-label">Chat</p>
                <h2>생성된 프롬프트로 대화하기</h2>
              </div>
              <span className="chat-status">
                {conversationId ? `대화 ID ${conversationId}` : '새 대화'}
              </span>
            </div>

            {chatError ? <div className="feedback error">{chatError}</div> : null}

            <div className="chat-list">
              {chatMessages.length > 0 ? (
                chatMessages.map((message, index) => (
                  <article className={`chat-bubble ${message.role}`} key={`${message.role}-${index}`}>
                    <p className="chat-role">{message.role === 'user' ? '나' : 'Assistant'}</p>
                    <p>{message.content}</p>
                  </article>
                ))
              ) : (
                <div className="empty-state">
                  방금 생성한 시스템 프롬프트를 기반으로 바로 대화를 시작할 수 있습니다.
                </div>
              )}
            </div>

            <form className="chat-form" onSubmit={onChatSubmit}>
              <textarea
                className="chat-input"
                disabled={chatSubmitting}
                onChange={(event) => onChatInputChange(event.target.value)}
                placeholder="예: 오늘 해야 할 일을 우선순위로 정리해줘."
                rows={4}
                value={chatInput}
              />
              <button className="primary-button" disabled={chatSubmitting} type="submit">
                {chatSubmitting ? '응답 생성 중...' : '메시지 보내기'}
              </button>
            </form>
          </section>
        </div>
      ) : (
        <div className="empty-state">
          모든 질문에 답변한 뒤 생성 버튼을 누르면 여기에 결과가 표시됩니다.
        </div>
      )}
    </aside>
  )
}

export type { ChatMessage }
export default ResultPanel
