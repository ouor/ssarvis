import { render, screen } from '@testing-library/react'
import { Button } from './Button'

describe('Button', () => {
  it('renders the button label', () => {
    render(<Button>게시하기</Button>)

    expect(
      screen.getByRole('button', { name: '게시하기' }),
    ).toBeInTheDocument()
  })
})
