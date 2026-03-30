import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, vi } from 'vitest'
import {
  AuthContext,
  type AuthContextValue,
} from '../../app/providers/auth-context'
import { UserProfilePage } from './UserProfilePage'

const apiMocks = vi.hoisted(() => ({
  getProfileByUsername: vi.fn(),
  getPublicProfileByUsername: vi.fn(),
  getProfilePosts: vi.fn(),
  getPublicProfilePosts: vi.fn(),
}))

vi.mock('../../features/profile-settings/api', () => ({
  getProfileByUsername: apiMocks.getProfileByUsername,
  getPublicProfileByUsername: apiMocks.getPublicProfileByUsername,
}))

vi.mock('../../features/feed/api', () => ({
  getProfilePosts: apiMocks.getProfilePosts,
  getPublicProfilePosts: apiMocks.getPublicProfilePosts,
}))

const unauthenticatedValue: AuthContextValue = {
  currentUser: null,
  isLoading: false,
  isAuthenticated: false,
  isDemo: false,
  sessionMessage: null,
  loginWithPassword: async () => {},
  signupWithPassword: async () => {},
  continueAsDemo: () => {},
  logout: () => {},
  deleteAccount: async () => {},
  clearSessionMessage: () => {},
  syncCurrentUser: () => {},
}

const ownerDemoValue: AuthContextValue = {
  ...unauthenticatedValue,
  isAuthenticated: true,
  isDemo: true,
}

describe('UserProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows the same profile shell for visitors but without editable setting cards', async () => {
    apiMocks.getPublicProfileByUsername.mockResolvedValue({
      userId: 2,
      username: 'alice',
      displayName: 'Alice',
      visibility: 'PUBLIC',
      me: false,
      following: false,
    })
    apiMocks.getPublicProfilePosts.mockResolvedValue([
      {
        postId: 10,
        ownerUserId: 2,
        ownerUsername: 'alice',
        ownerDisplayName: 'Alice',
        ownerVisibility: 'PUBLIC',
        content: 'hello world',
        createdAt: '2026-03-30T10:00:00Z',
      },
    ])

    render(
      <AuthContext.Provider value={unauthenticatedValue}>
        <MemoryRouter initialEntries={['/users/alice']}>
          <Routes>
            <Route path="/users/:username" element={<UserProfilePage />} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    expect(
      await screen.findByRole('heading', { name: 'Alice님의 프로필' }),
    ).toBeInTheDocument()
    expect(screen.getAllByText('Alice').length).toBeGreaterThan(0)
    expect(screen.getAllByText('@alice').length).toBeGreaterThan(0)
    expect(screen.getByRole('heading', { name: '내 게시물' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '표시 이름 저장' })).not.toBeInTheDocument()
    expect(screen.queryByText('공개성')).not.toBeInTheDocument()
    expect(screen.queryByText('자동응답 상태')).not.toBeInTheDocument()
    expect(apiMocks.getPublicProfileByUsername).toHaveBeenCalledWith('alice')
    expect(apiMocks.getPublicProfilePosts).toHaveBeenCalledWith('alice')
    expect(apiMocks.getProfileByUsername).not.toHaveBeenCalled()
    expect(apiMocks.getProfilePosts).not.toHaveBeenCalled()
  })

  it('shows the full owner profile when the route username is mine', async () => {
    render(
      <AuthContext.Provider value={ownerDemoValue}>
        <MemoryRouter initialEntries={['/users/haru']}>
          <Routes>
            <Route path="/users/:username" element={<UserProfilePage />} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    expect(
      screen.getByRole('heading', {
        name: '내 프로필',
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '표시 이름 저장' })).toBeInTheDocument()
    expect(apiMocks.getPublicProfileByUsername).not.toHaveBeenCalled()
    expect(apiMocks.getPublicProfilePosts).not.toHaveBeenCalled()
  })
})
