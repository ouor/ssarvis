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
  cloneAId: string
  cloneBId: string
  cloneAVoiceId: string
  cloneBVoiceId: string
  clones: CloneOption[]
  debateError: string
  debateSubmitting: boolean
  debateTopic: string
  debateTurns: string
  debateTurnsList: DebateTurn[]
  onCloneAChange: (value: string) => void
  onCloneAVoiceChange: (value: string) => void
  onCloneBChange: (value: string) => void
  onCloneBVoiceChange: (value: string) => void
  onDebateSubmit: React.FormEventHandler<HTMLFormElement>
  onTopicChange: (value: string) => void
  onTurnsChange: (value: string) => void
  voices: VoiceOption[]
}

function DebatePanel({
  cloneAId,
  cloneBId,
  cloneAVoiceId,
  cloneBVoiceId,
  clones,
  debateError,
  debateSubmitting,
  debateTopic,
  debateTurns,
  debateTurnsList,
  onCloneAChange,
  onCloneAVoiceChange,
  onCloneBChange,
  onCloneBVoiceChange,
  onDebateSubmit,
  onTopicChange,
  onTurnsChange,
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
            <select onChange={(event) => onCloneAChange(event.target.value)} value={cloneAId}>
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
            <select onChange={(event) => onCloneAVoiceChange(event.target.value)} value={cloneAVoiceId}>
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
            <select onChange={(event) => onCloneBChange(event.target.value)} value={cloneBId}>
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
            <select onChange={(event) => onCloneBVoiceChange(event.target.value)} value={cloneBVoiceId}>
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
            onChange={(event) => onTopicChange(event.target.value)}
            placeholder="예: 원격근무가 오프라인 근무보다 더 효율적인가?"
            rows={3}
            value={debateTopic}
          />
        </label>

        <label className="field-block compact">
          <span>클론당 발언 횟수</span>
          <select onChange={(event) => onTurnsChange(event.target.value)} value={debateTurns}>
            <option value="1">1회</option>
            <option value="2">2회</option>
            <option value="3">3회</option>
          </select>
        </label>

        <button className="primary-button" disabled={debateSubmitting} type="submit">
          {debateSubmitting ? '논쟁 생성 중...' : '논쟁 시작'}
        </button>
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
          <div className="empty-state">두 클론과 음성을 고르고 주제를 입력하면 여기에 논쟁이 생성됩니다.</div>
        )}
      </div>
    </section>
  )
}

export type { CloneOption, DebateTurn, VoiceOption }
export default DebatePanel
