import { NavLink } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { getDemoUser } from '../../lib/demo/adapters'
import { buildUserProfileRoute, ROUTES } from '../../lib/constants/routes'
import { ProfileLink } from './ProfileLink'
import { Avatar } from '../ui/Avatar'
import { Button } from '../ui/Button'

export function SidebarNav() {
  const { currentUser, isDemo, logout } = useAuth()
  const user = currentUser ?? getDemoUser()
  const navItems = [
    { label: 'Home', to: ROUTES.home },
    { label: 'Messages', to: ROUTES.messages },
    { label: 'Studio', to: ROUTES.studio },
    { label: 'People', to: ROUTES.people },
    { label: 'Profile', to: buildUserProfileRoute(user.username) },
  ]

  return (
    <aside className="sidebar">
      <div className="brand-lockup">
        <span className="brand-kicker">Soft Futurism</span>
        <div className="brand-title">SSARVIS</div>
        <p className="brand-copy">
          사람의 관계와 AI 페르소나를 하나의 공간 안에서 부드럽게 이어주는
          소셜 캔버스.
        </p>
      </div>

      <nav className="nav-list" aria-label="Primary">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
          >
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <Button variant="primary">새 글 쓰기</Button>
        <ProfileLink
          username={user.username}
          className="glass-card ui-card sidebar-user"
        >
          <Avatar name={user.displayName} />
          <div>
            <strong>{user.displayName}</strong>
            <div className="meta-line">@{user.username}</div>
            {isDemo ? <div className="meta-line">Demo Session</div> : null}
          </div>
        </ProfileLink>
        <Button variant="ghost" onClick={logout}>
          로그아웃
        </Button>
      </div>
    </aside>
  )
}
