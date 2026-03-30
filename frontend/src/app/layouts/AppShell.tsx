import { NavLink, Outlet } from 'react-router-dom'
import { SidebarNav } from '../../components/shared/SidebarNav'
import { useAuth } from '../../hooks/useAuth'
import { getDemoUser } from '../../lib/demo/adapters'
import { buildUserProfileRoute, ROUTES } from '../../lib/constants/routes'

export function AppShell() {
  const { currentUser } = useAuth()
  const user = currentUser ?? getDemoUser()
  const mobileTabs = [
    { label: 'Home', to: ROUTES.home },
    { label: 'Messages', to: ROUTES.messages },
    { label: 'Studio', to: ROUTES.studio },
    { label: 'Profile', to: buildUserProfileRoute(user.username) },
  ]

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
