export type Visibility = 'PUBLIC' | 'PRIVATE'

export type AppUser = {
  userId: number
  username: string
  displayName: string
  visibility: Visibility
}
