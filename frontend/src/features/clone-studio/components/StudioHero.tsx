import type { CurrentUser } from '../types'

type StudioHeroProps = {
  cloneCount: number
  voiceCount: number
  activeTab: 'clones' | 'live'
  currentUser: CurrentUser
}

function StudioHero({ cloneCount, voiceCount, activeTab, currentUser }: StudioHeroProps) {
  return (
    <section className="studio-hero">
      <div>
        <p className="studio-kicker">Kindred Flow</p>
        <h1>클론을 만들고, 말투를 입히고, 대화와 논쟁을 바로 시작해보세요.</h1>
        <p className="studio-hero-copy">
          {currentUser.displayName}님의 자산은 물론, 다른 사용자가 공개한 클론과 목소리까지 불러와 대화와 논쟁 흐름을 이어갈 수 있습니다.
        </p>
      </div>
      <div className="studio-highlight">
        <div>
          <strong>{cloneCount}</strong>
          <span>내 클론</span>
        </div>
        <div>
          <strong>{voiceCount}</strong>
          <span>내 목소리</span>
        </div>
        <div>
          <strong>{activeTab === 'live' ? 'LIVE' : 'READY'}</strong>
          <span>실시간 세션 상태</span>
        </div>
      </div>
    </section>
  )
}

export default StudioHero
