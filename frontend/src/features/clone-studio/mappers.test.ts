import { formatCloneTimestamp } from './mappers'

describe('clone mappers', () => {
  it('formats clone timestamps into readable labels', () => {
    expect(formatCloneTimestamp('2026-03-25T11:20:00Z')).toMatch(/3/)
  })
})
