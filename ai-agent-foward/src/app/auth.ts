const ACCESS_TOKEN_KEY = 'accessToken'

export function isAuthenticated(): boolean {
  // Debug mode: skip auth in development
  if (import.meta.env.DEV) {
    return true
  }
  return Boolean(localStorage.getItem(ACCESS_TOKEN_KEY) || sessionStorage.getItem(ACCESS_TOKEN_KEY))
}

export function clearAccessToken(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
}

export function saveAccessToken(token: string, rememberMe: boolean): void {
  if (rememberMe) {
    localStorage.setItem(ACCESS_TOKEN_KEY, token)
    sessionStorage.removeItem(ACCESS_TOKEN_KEY)
    return
  }

  sessionStorage.setItem(ACCESS_TOKEN_KEY, token)
  localStorage.removeItem(ACCESS_TOKEN_KEY)
}
