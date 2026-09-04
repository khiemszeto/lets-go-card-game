const USERNAME_KEY = 'username'
const BALANCE_KEY = 'balance'


export function saveSession(username: string, balance:number) {
    localStorage.setItem(USERNAME_KEY, username)
    localStorage.setItem(BALANCE_KEY, balance.toString())
}

export function clearSession() {
    localStorage.removeItem(USERNAME_KEY)
    localStorage.removeItem(BALANCE_KEY)
}

export function getUsername() {
    return localStorage.getItem(USERNAME_KEY)
}

export function getBalance() {
    return localStorage.getItem(BALANCE_KEY)
}