import { formatVoiceTimestamp } from './mappers'

describe('voice mappers', () => {
  it('formats voice timestamps into readable labels', () => {
    expect(formatVoiceTimestamp('2026-03-25T11:21:00Z')).toMatch(/3/)
  })
})
