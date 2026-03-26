import AppModal from '../../../../components/AppModal'
import type { CloneOption, VoiceOption } from '../../types'
import { formatCloneName, formatVoiceLabel, formatVisibilityLabel } from '../../utils'

type DebateSetupModalProps = {
  clone: CloneOption
  availableClones: CloneOption[]
  cloneLoadError: string
  availableVoices: VoiceOption[]
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
  availableClones,
  cloneLoadError,
  availableVoices,
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
  const cloneVoiceLabel = `${formatCloneName(clone)}의 목소리`
  const opponentVoiceLabel = debateOpponent ? `${formatCloneName(debateOpponent)}의 목소리` : '두 번째 목소리'

  return (
    <AppModal
      onBack={onBack}
      onClose={onClose}
      subtitle="상대 클론과 두 목소리를 정한 뒤, 실시간으로 이어지는 논쟁을 시작할 수 있습니다."
      title={`${formatCloneName(clone)} 논쟁 설정`}
    >
      <div className="modal-stack">
        <p className="modal-note">공개 클론과 공개 음성도 선택할 수 있습니다. 작성자 이름과 공개 여부를 확인한 뒤 조합을 정해 주세요.</p>
        {cloneLoadError ? <p className="inline-error">{cloneLoadError}</p> : null}
        <label className="field-stack">
          <span>상대 클론</span>
          <select onChange={(event) => onOpponentChange(event.target.value)} value={debateOpponentId}>
            <option value="">다른 클론 선택</option>
            {availableClones
              .filter((item) => item.cloneId !== clone.cloneId)
              .map((item) => (
                <option key={item.cloneId} value={item.cloneId}>
                  {formatCloneName(item)} · {formatVisibilityLabel(item.isPublic)} · {item.ownerDisplayName ?? '소유자 없음'}
                </option>
              ))}
          </select>
        </label>

        <label className="field-stack">
          <span>{cloneVoiceLabel}</span>
          <select onChange={(event) => onVoiceAChange(event.target.value)} value={debateVoiceAId}>
            <option value="">목소리 선택</option>
            {availableVoices.map((voice) => (
              <option key={voice.registeredVoiceId} value={voice.registeredVoiceId}>
                {formatVoiceLabel(voice)} · {formatVisibilityLabel(voice.isPublic)} · {voice.ownerDisplayName ?? '소유자 없음'}
              </option>
            ))}
          </select>
        </label>

        <label className="field-stack">
          <span>{opponentVoiceLabel}</span>
          <select onChange={(event) => onVoiceBChange(event.target.value)} value={debateVoiceBId}>
            <option value="">목소리 선택</option>
            {availableVoices.map((voice) => (
              <option key={voice.registeredVoiceId} value={voice.registeredVoiceId}>
                {formatVoiceLabel(voice)} · {formatVisibilityLabel(voice.isPublic)} · {voice.ownerDisplayName ?? '소유자 없음'}
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
