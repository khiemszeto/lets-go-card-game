
// Every protected REST call needs the JWT. Create one helper instead of repeating headers.
export async function apiFetch(path: string, options: RequestInit = {}) {
    const headers = new Headers(options.headers)

    if (!headers.has('Content-Type') && options.body) {
        headers.set('Content-Type', 'application/json')
    }

    const res = await fetch(path, {
        ...options,
        headers,
        credentials: 'include',
    })

    if (!res.ok) {
        const msg = (await res.json().catch(() => null))?.message ?? res.statusText
        throw new Error(msg)
    }
    return res

}