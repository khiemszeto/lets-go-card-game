import { FormEvent, useState } from 'react'
import { loginPlayer } from '../api/authApi'
import { saveAuth } from '../auth/authStorage'

type Props = {
    onGoRegister: () => void
    onSuccess: () => void
}


function LoginPage({onGoRegister, onSuccess} : Props) {
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(false)


    async function handleSubmit(e: FormEvent) {
        e.preventDefault()
        setError(null)
        setLoading(true)
        try {
            const { accessToken } = await loginPlayer({ username, password })
            saveAuth(accessToken, username)
            onSuccess()
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Login failed')
        } finally {
            setLoading(false)
        }
    }
    return (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <h1>Login</h1>
            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', width: '260px', gap: '8px' }}>
                <input
                    type="text"
                    placeholder="Username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    required
                />
                <input
                    type="password"
                    placeholder="Password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                />
                {error && <p style={{ color: 'red' }}>{error}</p>}
                <button type="submit" disabled={loading}>
                    {loading ? 'Logging in…' : 'Login'}
                </button>
            </form>
            <p>
                Don&apos;t have an account?{' '}
                <button type="button" onClick={onGoRegister}>Register</button>
            </p>
        </div>
    )

}

export default LoginPage;