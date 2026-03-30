import { render, screen } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { AppShell } from './layouts/AppShell'
import { HomePage } from '../pages/home/HomePage'
import { MessagesPage } from '../pages/messages/MessagesPage'
import { StudioPage } from '../pages/studio/StudioPage'
import { createAuthWrapper } from '../test/auth-test-utils'

describe('app scenarios', () => {
  it('renders the main app shell and home experience for an authenticated demo user', () => {
    render(
      <Routes>
        <Route element={<AppShell />}>
          <Route path="/" element={<HomePage />} />
        </Route>
      </Routes>,
      {
        wrapper: createAuthWrapper(),
      },
    )

    expect(screen.getAllByText('SSARVIS').length).toBeGreaterThan(0)
    expect(
      screen.getByRole('heading', {
        name: /사람의 흐름 위에 ai 스튜디오가 겹쳐지는 피드/i,
      }),
    ).toBeInTheDocument()
  })

  it('renders the messaging workflow inside the shared shell', () => {
    render(
      <Routes>
        <Route element={<AppShell />}>
          <Route path="/" element={<MessagesPage />} />
        </Route>
      </Routes>,
      {
        wrapper: createAuthWrapper(),
      },
    )

    expect(
      screen.getByRole('heading', {
        name: /텍스트와 음성이 자연스럽게 섞이는 대화 공간/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '메시지 보내기' })).toBeInTheDocument()
  })

  it('renders the studio workflow inside the shared shell', () => {
    render(
      <Routes>
        <Route element={<AppShell />}>
          <Route path="/" element={<StudioPage />} />
        </Route>
      </Routes>,
      {
        wrapper: createAuthWrapper(),
      },
    )

    expect(
      screen.getByRole('heading', {
        name: /사람의 말투와 음성을 다듬는 페르소나 작업실/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('대화 히스토리')).toBeInTheDocument()
  })
})
