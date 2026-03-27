import type { StudioTab } from '../types'

type StudioTabsProps = {
  activeTab: StudioTab
  onTabChange: (tab: StudioTab) => void
}

function StudioTabs({ activeTab, onTabChange }: StudioTabsProps) {
  return (
    <section className="studio-tabs">
      <button
        className={activeTab === 'clones' ? 'tab-chip tab-chip-active' : 'tab-chip'}
        onClick={() => onTabChange('clones')}
        type="button"
      >
        클론 스튜디오
      </button>
      <button
        className={activeTab === 'friends' ? 'tab-chip tab-chip-active' : 'tab-chip'}
        onClick={() => onTabChange('friends')}
        type="button"
      >
        친구
      </button>
      <button
        className={activeTab === 'live' ? 'tab-chip tab-chip-active' : 'tab-chip'}
        onClick={() => onTabChange('live')}
        type="button"
      >
        라이브 세션
      </button>
    </section>
  )
}

export default StudioTabs
