export const ROUTES = {
  auth: '/auth',
  home: '/',
  messages: '/messages',
  postDetail: '/posts/:postId',
  people: '/people',
  userProfile: '/users/:username',
  studio: '/studio',
} as const

export function buildPostDetailRoute(postId: number | string) {
  return ROUTES.postDetail.replace(':postId', String(postId))
}

export function buildUserProfileRoute(username: string) {
  return ROUTES.userProfile.replace(':username', username)
}
