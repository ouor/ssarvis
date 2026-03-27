import type { CloneOption, CurrentUser } from '../types'
import { formatCloneName, formatVisibilityLabel } from '../utils'

type CloneGridSectionProps = {
  mineClones: CloneOption[]
  friendClones: CloneOption[]
  publicClones: CloneOption[]
  currentUser: CurrentUser
  loadError: string
  onCreateClone: () => void
  onCloneSelect: (clone: CloneOption) => void
}

function CloneGridSection({ mineClones, friendClones, publicClones, currentUser, loadError, onCreateClone, onCloneSelect }: CloneGridSectionProps) {
  return (
    <section className="studio-grid">
      <button className="create-clone-card" onClick={onCreateClone} type="button">
        <span className="create-clone-icon">+</span>
        <strong>새 클론 만들기</strong>
        <p>문답을 시작해서 새로운 성격과 말투를 가진 클론을 추가합니다.</p>
      </button>

      <div className="clone-section-stack">
        {loadError ? (
          <article className="empty-card">
            <strong>클론 목록을 불러오지 못했습니다.</strong>
            <p className="inline-error">{loadError}</p>
          </article>
        ) : null}

        <section className="clone-list-section">
          <div className="section-heading">
            <div>
              <p className="panel-kicker">My Clones</p>
              <h2>내 클론</h2>
            </div>
          </div>
          <div className="clone-grid">
            {mineClones.map((clone) => (
              <button key={clone.cloneId} className="clone-card" onClick={() => onCloneSelect(clone)} type="button">
                <div className="clone-avatar">{String(clone.cloneId).slice(-2)}</div>
                <div className="clone-card-copy">
                  <div className="clone-card-topline">
                    <span>{formatCloneName(clone)}</span>
                    <time>{new Date(clone.createdAt).toLocaleDateString('ko-KR')}</time>
                  </div>
                  <div className="asset-meta-row">
                    <span className={`asset-badge${clone.isPublic ? ' asset-badge-public' : ''}`}>{formatVisibilityLabel(clone.isPublic)}</span>
                  </div>
                  <h2>{clone.alias}</h2>
                  <p>{clone.shortDescription}</p>
                </div>
              </button>
            ))}
            {mineClones.length === 0 ? (
              <article className="empty-card">
                <strong>{currentUser.displayName}님 계정에는 아직 클론이 없습니다.</strong>
                <p>첫 번째 문답을 시작해서 내 계정 전용 클론을 만들거나, 아래 공개 클론을 바로 사용해보세요.</p>
              </article>
            ) : null}
          </div>
        </section>

        <section className="clone-list-section">
          <div className="section-heading">
            <div>
              <p className="panel-kicker">Friend Clones</p>
              <h2>친구 클론</h2>
            </div>
          </div>
          <div className="clone-grid">
            {friendClones.map((clone) => (
              <button key={clone.cloneId} className="clone-card" onClick={() => onCloneSelect(clone)} type="button">
                <div className="clone-avatar">{String(clone.cloneId).slice(-2)}</div>
                <div className="clone-card-copy">
                  <div className="clone-card-topline">
                    <span>{formatCloneName(clone)}</span>
                    <time>{new Date(clone.createdAt).toLocaleDateString('ko-KR')}</time>
                  </div>
                  <div className="asset-meta-row">
                    <span className="asset-badge">친구 전용</span>
                    <span className="asset-owner">작성자 {clone.ownerDisplayName ?? '알 수 없음'}</span>
                  </div>
                  <h2>{clone.alias}</h2>
                  <p>{clone.shortDescription}</p>
                </div>
              </button>
            ))}
            {friendClones.length === 0 ? (
              <article className="empty-card">
                <strong>아직 친구가 공개하지 않은 비공개 클론이 없습니다.</strong>
                <p>친구가 되면 서로의 비공개 클론도 여기에서 바로 선택해 대화와 논쟁에 사용할 수 있습니다.</p>
              </article>
            ) : null}
          </div>
        </section>

        <section className="clone-list-section">
          <div className="section-heading">
            <div>
              <p className="panel-kicker">Public Clones</p>
              <h2>공개 클론</h2>
            </div>
          </div>
          <div className="clone-grid">
            {publicClones.map((clone) => (
              <button key={clone.cloneId} className="clone-card" onClick={() => onCloneSelect(clone)} type="button">
                <div className="clone-avatar">{String(clone.cloneId).slice(-2)}</div>
                <div className="clone-card-copy">
                  <div className="clone-card-topline">
                    <span>{formatCloneName(clone)}</span>
                    <time>{new Date(clone.createdAt).toLocaleDateString('ko-KR')}</time>
                  </div>
                  <div className="asset-meta-row">
                    <span className="asset-badge asset-badge-public">{formatVisibilityLabel(clone.isPublic)}</span>
                    <span className="asset-owner">작성자 {clone.ownerDisplayName ?? '알 수 없음'}</span>
                  </div>
                  <h2>{clone.alias}</h2>
                  <p>{clone.shortDescription}</p>
                </div>
              </button>
            ))}
            {publicClones.length === 0 ? (
              <article className="empty-card">
                <strong>아직 공개된 클론이 없습니다.</strong>
                <p>다른 사용자가 공개한 클론이 생기면 여기에서 바로 사용할 수 있습니다.</p>
              </article>
            ) : null}
          </div>
        </section>
      </div>
    </section>
  )
}

export default CloneGridSection
