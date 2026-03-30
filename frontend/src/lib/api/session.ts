import { STORAGE_KEYS } from '../constants/storage'
import { readStorage } from '../utils/storage'

export class SessionRequiredError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'SessionRequiredError'
  }
}

export function getRequiredAccessToken(message: string) {
  const token = readStorage(STORAGE_KEYS.accessToken)

  if (!token) {
    throw new SessionRequiredError(message)
  }

  return token
}
