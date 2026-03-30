import { render, screen } from '@testing-library/react'
import { Routes, Route } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'
import { createAuthWrapper } from '../../test/auth-test-utils'

describe('ProtectedRoute', () => {
  it('shows the loading state while auth is being resolved', () => {
    render(
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<div>Protected Content</div>} />
        </Route>
      </Routes>,
      {
        wrapper: createAuthWrapper({
          isLoading: true,
          isAuthenticated: false,
        }),
      },
    )

    expect(screen.getByRole('status')).toBeInTheDocument()
    expect(screen.getByText(/세션을 불러오는 중입니다/i)).toBeInTheDocument()
  })

  it('renders protected content when authenticated', () => {
    render(
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<div>Protected Content</div>} />
        </Route>
      </Routes>,
      {
        wrapper: createAuthWrapper(),
      },
    )

    expect(screen.getByText('Protected Content')).toBeInTheDocument()
  })

  it('redirects unauthenticated users away from protected content', () => {
    render(
      <Routes>
        <Route path="/auth" element={<div>Auth Screen</div>} />
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<div>Protected Content</div>} />
        </Route>
      </Routes>,
      {
        wrapper: createAuthWrapper({
          isAuthenticated: false,
        }),
      },
    )

    expect(screen.getByText('Auth Screen')).toBeInTheDocument()
  })
})
