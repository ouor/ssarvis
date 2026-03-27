import type { ChangeEvent, FormEvent } from 'react'
import AppModal from '../../../../components/AppModal'
import type { CloneOption, VoiceOption } from '../../types'
import { formatCloneName, formatVoiceLabel, formatVisibilityLabel } from '../../utils'

type VoicePickerModalProps = {
  clone: CloneOption
  mineVoices: VoiceOption[]
  friendVoices: VoiceOption[]
  publicVoices: VoiceOption[]
  currentUserDisplayName: string
  selectedVoiceId: string
  voiceAlias: string
  voiceLoadError: string
  voiceRegistering: boolean
  visibilityUpdatingId: number | null
  voiceRegisterError: string
  onClose: () => void
  onBack: () => void
  onVoiceSelect: (voiceId: string) => void
  onVoiceAliasChange: (value: string) => void
  onVoiceFileChange: (event: ChangeEvent<HTMLInputElement>) => void
  onVoiceRegister: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onToggleVoiceVisibility: (voice: VoiceOption) => Promise<void>
  onStartChat: () => Promise<void>
}

function VoicePickerModal({
  clone,
  mineVoices,
  friendVoices,
  publicVoices,
  currentUserDisplayName,
  selectedVoiceId,
  voiceAlias,
  voiceLoadError,
  voiceRegistering,
  visibilityUpdatingId,
  voiceRegisterError,
  onClose,
  onBack,
  onVoiceSelect,
  onVoiceAliasChange,
  onVoiceFileChange,
  onVoiceRegister,
  onToggleVoiceVisibility,
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
        <p className="modal-note">
          내 음성은 여기서 바로 공개 전환할 수 있고, 친구 음성과 공개 음성은 작성자 표기를 확인한 뒤 현재 계정에서도 바로 사용할 수 있습니다.
        </p>
        <div className="voice-list">
          <button
            className={selectedVoiceId === '' ? 'voice-card voice-card-active voice-card-button' : 'voice-card voice-card-button'}
            onClick={() => onVoiceSelect('')}
            type="button"
          >
            <strong>목소리 없이 시작</strong>
            <span>TTS 없이 텍스트 응답만 받습니다.</span>
          </button>
          <div className="voice-group">
            <strong className="voice-group-title">내 음성</strong>
            {mineVoices.map((voice) => (
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
                <div className="asset-meta-row">
                  <span className={`asset-badge${voice.isPublic ? ' asset-badge-public' : ''}`}>{formatVisibilityLabel(voice.isPublic)}</span>
                  <span className="asset-owner">작성자 {voice.ownerDisplayName ?? currentUserDisplayName}</span>
                  <button
                    className="secondary-button"
                    disabled={visibilityUpdatingId === voice.registeredVoiceId}
                    onClick={(event) => {
                      event.preventDefault()
                      void onToggleVoiceVisibility(voice)
                    }}
                    type="button"
                  >
                    {visibilityUpdatingId === voice.registeredVoiceId ? '변경 중...' : voice.isPublic ? '비공개로 전환' : '공개로 전환'}
                  </button>
                </div>
              </label>
            ))}
            {mineVoices.length === 0 ? <p className="muted-copy">내 계정에 등록된 목소리가 아직 없습니다.</p> : null}
          </div>

          <div className="voice-group">
            <strong className="voice-group-title">친구 음성</strong>
            {friendVoices.map((voice) => (
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
                <div className="asset-meta-row">
                  <span className="asset-badge">친구 전용</span>
                  <span className="asset-owner">작성자 {voice.ownerDisplayName ?? currentUserDisplayName}</span>
                </div>
              </label>
            ))}
            {friendVoices.length === 0 ? <p className="muted-copy">사용 가능한 친구 음성이 아직 없습니다.</p> : null}
          </div>

          <div className="voice-group">
            <strong className="voice-group-title">공개 음성</strong>
            {publicVoices.map((voice) => (
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
                <div className="asset-meta-row">
                  <span className="asset-badge asset-badge-public">{formatVisibilityLabel(voice.isPublic)}</span>
                  <span className="asset-owner">작성자 {voice.ownerDisplayName ?? currentUserDisplayName}</span>
                </div>
              </label>
            ))}
            {publicVoices.length === 0 ? <p className="muted-copy">사용 가능한 공개 음성이 아직 없습니다.</p> : null}
          </div>
          {voiceLoadError ? <p className="inline-error">{voiceLoadError}</p> : null}
        </div>

        <form className="voice-upload-card" onSubmit={onVoiceRegister}>
          <div>
            <strong>새 목소리 등록</strong>
            <p>짧은 음성 파일을 업로드하면 선택 목록에 바로 추가됩니다.</p>
          </div>
          <input
            onChange={(event) => onVoiceAliasChange(event.target.value)}
            placeholder="사용할 별칭 입력 (예: 차분한 민지)"
            type="text"
            value={voiceAlias}
          />
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
