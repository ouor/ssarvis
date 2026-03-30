import type { LoginFormValues, SignupFormValues } from './types'

function isBlank(value: string) {
  return value.trim().length === 0
}

export function validateLogin(values: LoginFormValues) {
  if (isBlank(values.username)) {
    return '아이디를 입력해주세요.'
  }

  if (isBlank(values.password)) {
    return '비밀번호를 입력해주세요.'
  }

  return null
}

export function validateSignup(values: SignupFormValues) {
  if (isBlank(values.displayName)) {
    return '표시 이름을 입력해주세요.'
  }

  if (values.displayName.trim().length > 100) {
    return '표시 이름은 100자 이하로 입력해주세요.'
  }

  const loginError = validateLogin(values)

  if (loginError) {
    return loginError
  }

  if (values.password.trim().length < 8) {
    return '비밀번호는 8자 이상으로 입력해주세요.'
  }

  return null
}
