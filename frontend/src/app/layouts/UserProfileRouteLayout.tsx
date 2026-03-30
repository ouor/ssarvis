import { Outlet } from 'react-router-dom'
import { LoadingState } from '../../components/shared/LoadingState'
import { useAuth } from '../../hooks/useAuth'
import { AppShell } from './AppShell'

export function UserProfileRouteLayout() {
  const { isAuthenticated, isDemo, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="page-shell">
        <LoadingState
          title="프로필을 준비하는 중입니다"
          copy="로그인 상태를 확인한 뒤 맞는 화면 구성을 보여드릴게요."
        />
      </div>
    )
  }

  if (isAuthenticated || isDemo) {
    return <AppShell />
  }

  return <Outlet />
}
