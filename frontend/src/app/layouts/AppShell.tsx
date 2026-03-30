import { NavLink, Outlet } from 'react-router-dom'
import { SidebarNav } from '../../components/shared/SidebarNav'
import { ROUTES } from '../../lib/constants/routes'

const mobileTabs = [
  { label: 'Home', to: ROUTES.home },
  { label: 'Messages', to: ROUTES.messages },
  { label: 'Studio', to: ROUTES.studio },
  { label: 'Profile', to: ROUTES.profile },
]

export function AppShell() {
  return (
    <div className="app-shell">
      <SidebarNav />
      <main className="main-region">
        <div className="mobile-topbar">
          <span className="brand-kicker">SSARVIS</span>
          <strong>Soft Futurism Social</strong>
        </div>
        <Outlet />
      </main>
      <div />
      <nav className="bottom-tabs" aria-label="Mobile navigation">
        {mobileTabs.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            className={({ isActive }) => `bottom-tab${isActive ? ' active' : ''}`}
          >
            {tab.label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
