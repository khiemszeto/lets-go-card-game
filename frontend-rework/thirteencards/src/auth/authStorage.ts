const TOKEN_KEY = 'accessToken'
const USERNAME_KEY = 'username'
const BALANCE_KEY = 'balance'


export function saveAuth(accessToken: string, username: string, balance:number) {
    localStorage.setItem(TOKEN_KEY, accessToken)
    localStorage.setItem(USERNAME_KEY, username)
    localStorage.setItem(BALANCE_KEY, balance.toString())
}
export function getAccessToken() {
    return localStorage.getItem(TOKEN_KEY)
}
export function clearAuth() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USERNAME_KEY)
    localStorage.removeItem(BALANCE_KEY)
}
export function getUsername() {
    return localStorage.getItem(USERNAME_KEY)
}

export function getBalance() {
    return localStorage.getItem(BALANCE_KEY)
}