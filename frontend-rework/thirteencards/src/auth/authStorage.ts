const TOKEN_KEY = 'accessToken'
const USERNAME_KEY = 'username'


export function saveAuth(accessToken: string, username: string) {
    localStorage.setItem(TOKEN_KEY, accessToken)
    localStorage.setItem(USERNAME_KEY, username)
}
export function getAccessToken() {
    return localStorage.getItem(TOKEN_KEY)
}
export function clearAuth() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USERNAME_KEY)
}
export function getUsername() {
    return localStorage.getItem(USERNAME_KEY)
}