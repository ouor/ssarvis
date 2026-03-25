type StudioHeroProps = {
  cloneCount: number
  voiceCount: number
  activeTab: 'clones' | 'live'
}

function StudioHero({ cloneCount, voiceCount, activeTab }: StudioHeroProps) {
  return (
    <section className="studio-hero">
      <div>
        <p className="studio-kicker">Kindred Flow</p>
        <h1>클론을 만들고, 말투를 입히고, 대화와 논쟁을 바로 시작해보세요.</h1>
        <p className="studio-hero-copy">
          설문으로 성격을 빚은 클론을 수집하고, 원하는 목소리를 입혀 나와 대화시키거나 서로 토론하게 만들 수 있습니다.
        </p>
      </div>
      <div className="studio-highlight">
        <div>
          <strong>{cloneCount}</strong>
          <span>등록된 클론</span>
        </div>
        <div>
          <strong>{voiceCount}</strong>
          <span>사용 가능한 목소리</span>
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
