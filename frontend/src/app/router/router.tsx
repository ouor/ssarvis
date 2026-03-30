import { createBrowserRouter } from 'react-router-dom'
import { AuthPage } from '../../pages/auth/AuthPage'
import { HomePage } from '../../pages/home/HomePage'
import { MessagesPage } from '../../pages/messages/MessagesPage'
import { PeoplePage } from '../../pages/people/PeoplePage'
import { PostDetailPage } from '../../pages/post/PostDetailPage'
import { UserProfilePage } from '../../pages/profile/UserProfilePage'
import { StudioPage } from '../../pages/studio/StudioPage'
import { ROUTES } from '../../lib/constants/routes'
import { AppShell } from '../layouts/AppShell'
import { AuthLayout } from '../layouts/AuthLayout'
import { UserProfileRouteLayout } from '../layouts/UserProfileRouteLayout'
import { ProtectedRoute } from './ProtectedRoute'

export const router = createBrowserRouter([
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppShell />,
        children: [
          { path: ROUTES.home, element: <HomePage /> },
          { path: ROUTES.messages, element: <MessagesPage /> },
          { path: ROUTES.postDetail, element: <PostDetailPage /> },
          { path: ROUTES.people, element: <PeoplePage /> },
          { path: ROUTES.studio, element: <StudioPage /> },
        ],
      },
    ],
  },
  {
    element: <UserProfileRouteLayout />,
    children: [{ path: ROUTES.userProfile, element: <UserProfilePage /> }],
  },
  {
    element: <AuthLayout />,
    children: [{ path: ROUTES.auth, element: <AuthPage /> }],
  },
])
