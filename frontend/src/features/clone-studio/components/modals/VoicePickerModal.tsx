import type { ChangeEvent, FormEvent } from 'react'
import AppModal from '../../../../components/AppModal'
import type { CloneOption, VoiceOption } from '../../types'
import { formatCloneName, formatVoiceLabel } from '../../utils'

type VoicePickerModalProps = {
  clone: CloneOption
  voices: VoiceOption[]
  selectedVoiceId: string
  voiceLoadError: string
  voiceRegistering: boolean
  voiceRegisterError: string
  onClose: () => void
  onBack: () => void
  onVoiceSelect: (voiceId: string) => void
  onVoiceFileChange: (event: ChangeEvent<HTMLInputElement>) => void
  onVoiceRegister: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onStartChat: () => Promise<void>
}

function VoicePickerModal({
  clone,
  voices,
  selectedVoiceId,
  voiceLoadError,
  voiceRegistering,
  voiceRegisterError,
  onClose,
  onBack,
  onVoiceSelect,
  onVoiceFileChange,
  onVoiceRegister,
  onStartChat,
}: VoicePickerModalProps) {
  return (
    <AppModal
      onBack={onBack}
      onClose={onClose}
      subtitle="목소리는 선택 사항입니다. 고르지 않으면 텍스트만으로 대화를 시작합니다."
      title={`${formatCloneName(clone)}와 대화하기`}
    >
      <div className="modal-stack">
        <div className="voice-list">
          <button
            className={selectedVoiceId === '' ? 'voice-card voice-card-active voice-card-button' : 'voice-card voice-card-button'}
            onClick={() => onVoiceSelect('')}
            type="button"
          >
            <strong>목소리 없이 시작</strong>
            <span>TTS 없이 텍스트 응답만 받습니다.</span>
          </button>
          {voices.map((voice) => (
            <label
              key={voice.registeredVoiceId}
              className={selectedVoiceId === String(voice.registeredVoiceId) ? 'voice-card voice-card-active' : 'voice-card'}
            >
              <input
                checked={selectedVoiceId === String(voice.registeredVoiceId)}
                name="voice-selection"
                onChange={() => onVoiceSelect(String(voice.registeredVoiceId))}
                type="radio"
                value={voice.registeredVoiceId}
              />
              <strong>{formatVoiceLabel(voice)}</strong>
              <span>{voice.voiceId}</span>
            </label>
          ))}
          {voiceLoadError ? <p className="inline-error">{voiceLoadError}</p> : null}
          {voices.length === 0 ? <p className="muted-copy">등록된 목소리가 아직 없습니다. 아래에서 새로 등록해 주세요.</p> : null}
        </div>

        <form className="voice-upload-card" onSubmit={onVoiceRegister}>
          <div>
            <strong>새 목소리 등록</strong>
            <p>짧은 음성 파일을 업로드하면 선택 목록에 바로 추가됩니다.</p>
          </div>
          <input accept="audio/*" onChange={onVoiceFileChange} type="file" />
          {voiceRegisterError ? <p className="inline-error">{voiceRegisterError}</p> : null}
          <button className="secondary-button" disabled={voiceRegistering} type="submit">
            {voiceRegistering ? '등록 중...' : '목소리 등록'}
          </button>
        </form>

        <div className="modal-footer">
          <span>{selectedVoiceId ? '목소리 선택 완료' : '지금 상태로는 텍스트만 대화합니다.'}</span>
          <button className="primary-button" onClick={() => void onStartChat()} type="button">
            대화 시작
          </button>
        </div>
      </div>
    </AppModal>
  )
}

export default VoicePickerModal
