import AppModal from '../../../../components/AppModal'
import type { CloneOption, VoiceOption } from '../../types'
import { formatCloneName, formatVoiceLabel, previewTitle } from '../../utils'

type DebateSetupModalProps = {
  clone: CloneOption
  clones: CloneOption[]
  cloneLoadError: string
  voices: VoiceOption[]
  voiceLoadError: string
  debateOpponentId: string
  debateVoiceAId: string
  debateVoiceBId: string
  debateTopic: string
  debateSetupError: string
  debateOpponent: CloneOption | null
  onClose: () => void
  onBack: () => void
  onOpponentChange: (value: string) => void
  onVoiceAChange: (value: string) => void
  onVoiceBChange: (value: string) => void
  onTopicChange: (value: string) => void
  onStartDebate: () => Promise<void>
}

function DebateSetupModal({
  clone,
  clones,
  cloneLoadError,
  voices,
  voiceLoadError,
  debateOpponentId,
  debateVoiceAId,
  debateVoiceBId,
  debateTopic,
  debateSetupError,
  debateOpponent,
  onClose,
  onBack,
  onOpponentChange,
  onVoiceAChange,
  onVoiceBChange,
  onTopicChange,
  onStartDebate,
}: DebateSetupModalProps) {
  return (
    <AppModal
      onBack={onBack}
      onClose={onClose}
      subtitle="상대 클론과 두 목소리를 정한 뒤, 실시간으로 이어지는 논쟁을 시작할 수 있습니다."
      title={`${formatCloneName(clone)} 논쟁 설정`}
    >
      <div className="modal-stack">
        {cloneLoadError ? <p className="inline-error">{cloneLoadError}</p> : null}
        <label className="field-stack">
          <span>상대 클론</span>
          <select onChange={(event) => onOpponentChange(event.target.value)} value={debateOpponentId}>
            <option value="">다른 클론 선택</option>
            {clones
              .filter((item) => item.cloneId !== clone.cloneId)
              .map((item) => (
                <option key={item.cloneId} value={item.cloneId}>
                  {formatCloneName(item)} · {previewTitle(item.preview)}
                </option>
              ))}
          </select>
        </label>

        <label className="field-stack">
          <span>{`${formatCloneName(clone)}의 목소리`}</span>
          <select onChange={(event) => onVoiceAChange(event.target.value)} value={debateVoiceAId}>
            <option value="">목소리 선택</option>
            {voices.map((voice) => (
              <option key={voice.registeredVoiceId} value={voice.registeredVoiceId}>
                {formatVoiceLabel(voice)}
              </option>
            ))}
          </select>
        </label>

        <label className="field-stack">
          <span>{debateOpponent ? `${formatCloneName(debateOpponent)}의 목소리` : '두 번째 목소리'}</span>
          <select onChange={(event) => onVoiceBChange(event.target.value)} value={debateVoiceBId}>
            <option value="">목소리 선택</option>
            {voices.map((voice) => (
              <option key={voice.registeredVoiceId} value={voice.registeredVoiceId}>
                {formatVoiceLabel(voice)}
              </option>
            ))}
          </select>
        </label>

        <label className="field-stack">
          <span>논쟁 주제</span>
          <textarea
            onChange={(event) => onTopicChange(event.target.value)}
            placeholder="예: 인간적인 설득력은 데이터보다 중요한가?"
            rows={4}
            value={debateTopic}
          />
        </label>

        {voiceLoadError ? <p className="inline-error">{voiceLoadError}</p> : null}

        {debateSetupError ? <p className="inline-error">{debateSetupError}</p> : null}

        <div className="modal-footer">
          <span>페이지를 떠나거나 중단 버튼을 누를 때까지 논쟁이 이어집니다.</span>
          <button className="primary-button" onClick={() => void onStartDebate()} type="button">
            논쟁 시작
          </button>
        </div>
      </div>
    </AppModal>
  )
}

export default DebateSetupModal
