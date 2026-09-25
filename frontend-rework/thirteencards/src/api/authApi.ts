import type {LoginResponse, LoginRequest, RegisterRequest,RegisterResponse, MeResponse} from "../types/auth";

async function readError(res: Response): Promise<string> {
    const raw = await res.text()
    try {
        const data = JSON.parse(raw) as { message?: string }
        return data.message ?? 'Request failed'
    } catch {
        return raw || 'Request failed'
    }
}

export async function loginPlayer(body: LoginRequest) : Promise<LoginResponse> {

    const response = await fetch('/login', {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(body)
        })

    if (!response.ok) {
         throw new Error(await readError(response));
    }

    return response.json();
}

export async function registerPlayer(body: RegisterRequest) : Promise<RegisterResponse> {

    const response = await fetch('/api/players', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
    })

    if (!response.ok) {
        throw new Error(await readError(response));
    }

    return response.json();
}

export async function fetchMe() : Promise<MeResponse> {
    const response = await fetch('/api/me',
        {
            credentials: 'include'
        })

    if (!response.ok) {
        throw new Error(await readError(response));
    }

    return response.json();

}

export async function logoutPlayer() : Promise<void> {
   const res = await fetch('/logout', {
        method: 'POST',
        credentials: 'include'})
    if (!res.ok) throw new Error('Logout failed')
}