import type {LoginResponse, LoginRequest, RegisterRequest,RegisterResponse} from "../types/auth";

async function readError(res: Response): Promise<string> {
    try {
        const data = await res.json();
        return data.message ?? 'Request failed';
    }catch {
        return (await res.text()) || 'Request failed'
    }
}

export async function loginPlayer(body: LoginRequest) : Promise<LoginResponse> {

    const response = await fetch('/login', {
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

export async function registerPlayer(body: RegisterRequest) : Promise<RegisterResponse> {

    const response = await fetch('/api/players/register', {
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