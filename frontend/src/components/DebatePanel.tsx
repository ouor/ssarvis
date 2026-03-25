type CloneOption = {
  cloneId: number
  createdAt: string
  preview: string
}

type VoiceOption = {
  registeredVoiceId: number
  voiceId: string
  preferredName: string
  originalFilename: string
}

type DebateTurn = {
  turnIndex: number
  speaker: string
  cloneId: number
  content: string
  ttsAudioDataUrl?: string
  ttsVoiceId?: string
}

type DebatePanelProps = {
  debateSessionId: number | null
  debateRunning: boolean
  debateStopping: boolean
  cloneAId: string
  cloneBId: string
  cloneAVoiceId: string
  cloneBVoiceId: string
  clones: CloneOption[]
  debateError: string
  debateSubmitting: boolean
  debateTopic: string
  debateTurnsList: DebateTurn[]
  onCloneAChange: (value: string) => void
  onCloneAVoiceChange: (value: string) => void
  onCloneBChange: (value: string) => void
  onCloneBVoiceChange: (value: string) => void
  onDebateSubmit: React.FormEventHandler<HTMLFormElement>
  onDebateStop: () => void
  onTopicChange: (value: string) => void
  voices: VoiceOption[]
}

function DebatePanel({
  debateSessionId,
  debateRunning,
  debateStopping,
  cloneAId,
  cloneBId,
  cloneAVoiceId,
  cloneBVoiceId,
  clones,
  debateError,
  debateSubmitting,
  debateTopic,
  debateTurnsList,
  onCloneAChange,
  onCloneAVoiceChange,
  onCloneBChange,
  onCloneBVoiceChange,
  onDebateSubmit,
  onDebateStop,
  onTopicChange,
  voices,
}: DebatePanelProps) {
  return (
    <section className="debate-panel">
      <div className="panel-header">
        <div>
          <p className="section-label">Debate</p>
          <h2>두 클론 논쟁시키기</h2>
        </div>
      </div>

      <form className="debate-form" onSubmit={onDebateSubmit}>
        <div className="debate-grid">
          <label className="field-block">
            <span>클론 A</span>
            <select disabled={debateRunning} onChange={(event) => onCloneAChange(event.target.value)} value={cloneAId}>
              <option value="">선택하세요</option>
              {clones.map((clone) => (
                <option key={clone.cloneId} value={clone.cloneId}>
                  #{clone.cloneId} - {clone.preview}
                </option>
              ))}
            </select>
          </label>

          <label className="field-block">
            <span>클론 A 음성</span>
            <select disabled={debateRunning} onChange={(event) => onCloneAVoiceChange(event.target.value)} value={cloneAVoiceId}>
              <option value="">선택하세요</option>
              {voices.map((voice) => (
                <option key={voice.registeredVoiceId} value={voice.registeredVoiceId}>
                  {voice.originalFilename} - {voice.preferredName}
                </option>
              ))}
            </select>
          </label>

          <label className="field-block">
            <span>클론 B</span>
            <select disabled={debateRunning} onChange={(event) => onCloneBChange(event.target.value)} value={cloneBId}>
              <option value="">선택하세요</option>
              {clones.map((clone) => (
                <option key={clone.cloneId} value={clone.cloneId}>
                  #{clone.cloneId} - {clone.preview}
                </option>
              ))}
            </select>
          </label>

          <label className="field-block">
            <span>클론 B 음성</span>
            <select disabled={debateRunning} onChange={(event) => onCloneBVoiceChange(event.target.value)} value={cloneBVoiceId}>
              <option value="">선택하세요</option>
              {voices.map((voice) => (
                <option key={voice.registeredVoiceId} value={voice.registeredVoiceId}>
                  {voice.originalFilename} - {voice.preferredName}
                </option>
              ))}
            </select>
          </label>
        </div>

        <label className="field-block">
          <span>논쟁 주제</span>
          <textarea
            className="chat-input"
            disabled={debateRunning}
            onChange={(event) => onTopicChange(event.target.value)}
            placeholder="예: 원격근무가 오프라인 근무보다 더 효율적인가?"
            rows={3}
            value={debateTopic}
          />
        </label>

        <div className="debate-actions">
          <button className="primary-button" disabled={debateSubmitting || debateRunning} type="submit">
            {debateSubmitting ? '첫 턴 생성 중...' : '논쟁 시작'}
          </button>
          <button className="secondary-button" disabled={!debateRunning || debateStopping} onClick={onDebateStop} type="button">
            {debateStopping ? '중단 중...' : '중단'}
          </button>
          <span className="chat-status">{debateSessionId ? `세션 ID ${debateSessionId}` : '세션 없음'}</span>
        </div>
      </form>

      {debateError ? <div className="feedback error">{debateError}</div> : null}

      <div className="debate-turns">
        {debateTurnsList.length > 0 ? (
          debateTurnsList.map((turn) => (
            <article className={`chat-bubble ${turn.speaker === 'CLONE_A' ? 'user' : 'assistant'}`} key={`${turn.speaker}-${turn.turnIndex}`}>
              <p className="chat-role">
                {turn.speaker === 'CLONE_A' ? `클론 A (#${turn.cloneId})` : `클론 B (#${turn.cloneId})`}
              </p>
              <p>{turn.content}</p>
              {turn.ttsAudioDataUrl ? (
                <div className="tts-block">
                  <audio controls preload="none" src={turn.ttsAudioDataUrl} />
                  {turn.ttsVoiceId ? <p className="tts-caption">Voice: {turn.ttsVoiceId}</p> : null}
                </div>
              ) : null}
            </article>
          ))
        ) : (
          <div className="empty-state">두 클론과 음성을 고르고 주제를 입력하면 A의 첫 발언부터 실시간으로 재생됩니다.</div>
        )}
      </div>
    </section>
  )
}

export type { CloneOption, DebateTurn, VoiceOption }
export default DebatePanel
