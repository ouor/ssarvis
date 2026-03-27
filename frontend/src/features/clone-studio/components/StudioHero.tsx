import type { CurrentUser, StudioTab } from '../types'

type StudioHeroProps = {
  cloneCount: number
  voiceCount: number
  friendCount: number
  activeTab: StudioTab
  currentUser: CurrentUser
}

function StudioHero({ cloneCount, voiceCount, friendCount, activeTab, currentUser }: StudioHeroProps) {
  return (
    <section className="studio-hero">
      <div>
        <p className="studio-kicker">Kindred Flow</p>
        <h1>클론을 만들고, 말투를 입히고, 대화와 논쟁을 바로 시작해보세요.</h1>
        <p className="studio-hero-copy">
          {currentUser.displayName}님의 자산과 친구 관계, 공개 자산을 한 화면에서 관리하며 대화와 논쟁 흐름을 이어갈 수 있습니다.
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
          <strong>{friendCount}</strong>
          <span>현재 친구 수</span>
        </div>
        <div>
          <strong>{activeTab === 'live' ? 'LIVE' : activeTab === 'friends' ? 'LINK' : 'READY'}</strong>
          <span>현재 워크스페이스 상태</span>
        </div>
      </div>
    </section>
  )
}

export default StudioHero
