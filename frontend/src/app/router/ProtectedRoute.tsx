import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { LoadingState } from '../../components/shared/LoadingState'
import { ROUTES } from '../../lib/constants/routes'
import { useAuth } from '../../hooks/useAuth'

export function ProtectedRoute() {
  const location = useLocation()
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="page-shell">
        <LoadingState
          title="세션을 불러오는 중입니다"
          copy="보호된 화면에 접근하기 전에 로그인 상태를 확인하고 있어요."
        />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.auth} replace state={{ from: location }} />
  }

  return <Outlet />
}
