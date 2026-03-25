import AppModal from '../../../../components/AppModal'
import type { CloneOption } from '../../types'
import { formatCloneName, previewTitle } from '../../utils'

type CloneActionsModalProps = {
  clone: CloneOption
  onClose: () => void
  onChatSelect: () => void
  onDebateSelect: () => void
}

function CloneActionsModal({ clone, onClose, onChatSelect, onDebateSelect }: CloneActionsModalProps) {
  return (
    <AppModal onClose={onClose} subtitle={previewTitle(clone.preview)} title={formatCloneName(clone)}>
      <div className="modal-stack">
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
