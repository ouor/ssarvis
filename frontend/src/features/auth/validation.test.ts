import { validateLogin, validateSignup } from './validation'

describe('auth validation', () => {
  it('requires username and password for login', () => {
    expect(validateLogin({ username: '', password: '' })).toBe(
      '아이디를 입력해주세요.',
    )
    expect(validateLogin({ username: 'haru', password: '' })).toBe(
      '비밀번호를 입력해주세요.',
    )
  })

  it('requires display name and minimum password length for signup', () => {
    expect(
      validateSignup({
        username: 'haru',
        password: '12345678',
        displayName: '',
      }),
    ).toBe('표시 이름을 입력해주세요.')

    expect(
      validateSignup({
        username: 'haru',
        password: '1234',
        displayName: '하루',
      }),
    ).toBe('비밀번호는 8자 이상으로 입력해주세요.')
  })

  it('returns null for valid signup values', () => {
    expect(
      validateSignup({
        username: 'haru',
        password: 'secret123',
        displayName: '하루',
      }),
    ).toBeNull()
  })
})
