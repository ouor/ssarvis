import type { AppUser } from '../../lib/types/common'

export type AuthResponse = AppUser & {
  accessToken: string
}

export type LoginFormValues = {
  username: string
  password: string
}

export type SignupFormValues = LoginFormValues & {
  displayName: string
}
