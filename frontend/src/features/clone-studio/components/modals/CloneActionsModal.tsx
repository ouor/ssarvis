import AppModal from '../../../../components/AppModal'
import type { CloneOption } from '../../types'
import { formatCloneName, formatVisibilityLabel } from '../../utils'

type CloneActionsModalProps = {
  clone: CloneOption
  currentUserDisplayName: string
  canManageVisibility: boolean
  visibilityUpdating: boolean
  onClose: () => void
  onChatSelect: () => void
  onDebateSelect: () => void
  onToggleVisibility: () => Promise<void>
}

function CloneActionsModal({
  clone,
  currentUserDisplayName,
  canManageVisibility,
  visibilityUpdating,
  onClose,
  onChatSelect,
  onDebateSelect,
  onToggleVisibility,
}: CloneActionsModalProps) {
  const ownerName = clone.ownerDisplayName ?? currentUserDisplayName
  const visibilityHint = canManageVisibility
    ? '공개 상태를 바꾸면 다른 로그인 사용자도 이 클론을 대화와 논쟁에 사용할 수 있습니다.'
    : `${ownerName}님이 공개한 클론입니다. 지금 계정에서도 바로 대화와 논쟁에 사용할 수 있습니다.`

  return (
    <AppModal onClose={onClose} subtitle={clone.shortDescription} title={formatCloneName(clone)}>
      <div className="modal-stack">
        <div className="asset-meta-row">
          <span className={`asset-badge${clone.isPublic ? ' asset-badge-public' : ''}`}>{formatVisibilityLabel(clone.isPublic)}</span>
          <span className="asset-owner">작성자 {ownerName}</span>
          {canManageVisibility ? (
            <button className="secondary-button" disabled={visibilityUpdating} onClick={() => void onToggleVisibility()} type="button">
              {visibilityUpdating ? '변경 중...' : clone.isPublic ? '비공개로 전환' : '공개로 전환'}
            </button>
          ) : null}
        </div>
        <p className="modal-note">{visibilityHint}</p>
        <button className="action-card" onClick={onChatSelect} type="button">
          <strong>나와 대화하기</strong>
          <p>목소리를 선택한 뒤 이 클론과 1:1 실시간 대화를 시작합니다.</p>
        </button>
        <button className="action-card" onClick={onDebateSelect} type="button">
          <strong>논쟁시키기</strong>
          <p>다른 클론과 짝을 지어 주제에 대해 계속 말하게 합니다.</p>
        </button>
      </div>
    </AppModal>
  )
}

export default CloneActionsModal
