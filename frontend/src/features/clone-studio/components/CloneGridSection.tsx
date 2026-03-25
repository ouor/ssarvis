import type { CloneOption } from '../types'
import { formatCloneName } from '../utils'

type CloneGridSectionProps = {
  clones: CloneOption[]
  loadError: string
  onCreateClone: () => void
  onCloneSelect: (clone: CloneOption) => void
}

function CloneGridSection({ clones, loadError, onCreateClone, onCloneSelect }: CloneGridSectionProps) {
  return (
    <section className="studio-grid">
      <button className="create-clone-card" onClick={onCreateClone} type="button">
        <span className="create-clone-icon">+</span>
        <strong>새 클론 만들기</strong>
        <p>문답을 시작해서 새로운 성격과 말투를 가진 클론을 추가합니다.</p>
      </button>

      <div className="clone-grid">
        {loadError ? (
          <article className="empty-card">
            <strong>클론 목록을 불러오지 못했습니다.</strong>
            <p className="inline-error">{loadError}</p>
          </article>
        ) : null}
        {clones.map((clone) => (
          <button key={clone.cloneId} className="clone-card" onClick={() => onCloneSelect(clone)} type="button">
            <div className="clone-avatar">{String(clone.cloneId).slice(-2)}</div>
            <div className="clone-card-copy">
              <div className="clone-card-topline">
                <span>{formatCloneName(clone)}</span>
                <time>{new Date(clone.createdAt).toLocaleDateString('ko-KR')}</time>
              </div>
              <h2>{clone.alias}</h2>
              <p>{clone.shortDescription}</p>
            </div>
          </button>
        ))}
        {clones.length === 0 ? (
          <article className="empty-card">
            <strong>아직 만들어진 클론이 없습니다.</strong>
            <p>첫 번째 문답을 시작해서 스튜디오를 채워보세요.</p>
          </article>
        ) : null}
      </div>
    </section>
  )
}

export default CloneGridSection
