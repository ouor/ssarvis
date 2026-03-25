type ChatMessage = {
  role: 'user' | 'assistant'
  content: string
  ttsAudioDataUrl?: string
  ttsVoiceId?: string
}

type ResultPanelProps = {
  chatError: string
  chatInput: string
  chatMessages: ChatMessage[]
  chatSubmitting: boolean
  conversationId: number | null
  error: string
  onVoiceFileChange: (file: File | null) => void
  onChatInputChange: (value: string) => void
  onChatSubmit: React.FormEventHandler<HTMLFormElement>
  onVoiceRegister: React.FormEventHandler<HTMLFormElement>
  registeredVoiceLabel: string
  voiceRegisterError: string
  voiceRegistering: boolean
  systemPrompt: string
}

function ResultPanel({
  chatError,
  chatInput,
  chatMessages,
  chatSubmitting,
  conversationId,
  error,
  onVoiceFileChange,
  onChatInputChange,
  onChatSubmit,
  onVoiceRegister,
  registeredVoiceLabel,
  systemPrompt,
  voiceRegisterError,
  voiceRegistering,
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

            <form className="voice-panel" onSubmit={onVoiceRegister}>
              <div>
                <p className="section-label">Voice</p>
                <h2>내 음성 등록하기</h2>
              </div>
              <input
                accept="audio/*"
                className="voice-file-input"
                onChange={(event) => onVoiceFileChange(event.target.files?.[0] ?? null)}
                type="file"
              />
              <button className="secondary-button" disabled={voiceRegistering} type="submit">
                {voiceRegistering ? '등록 중...' : '음성 등록'}
              </button>
              {registeredVoiceLabel ? <p className="voice-status">{registeredVoiceLabel}</p> : null}
              {voiceRegisterError ? <div className="feedback error">{voiceRegisterError}</div> : null}
            </form>

            {chatError ? <div className="feedback error">{chatError}</div> : null}

            <div className="chat-list">
              {chatMessages.length > 0 ? (
                chatMessages.map((message, index) => (
                  <article className={`chat-bubble ${message.role}`} key={`${message.role}-${index}`}>
                    <p className="chat-role">{message.role === 'user' ? '나' : 'Assistant'}</p>
                    <p>{message.content}</p>
                    {message.ttsAudioDataUrl ? (
                      <div className="tts-block">
                        <audio controls preload="none" src={message.ttsAudioDataUrl} />
                        {message.ttsVoiceId ? <p className="tts-caption">Voice: {message.ttsVoiceId}</p> : null}
                      </div>
                    ) : null}
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
