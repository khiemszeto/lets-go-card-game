import { useState } from 'react'
import type { FormEvent } from 'react'
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
        <main className="auth-page">
            <div className="auth-card">
                <h1 className="auth-title">Welcome back</h1>
                <p className="auth-subtitle">Sign in to play Thirteen.</p>

                <form className="auth-form" onSubmit={handleSubmit}>
                    <label className="field">
                        <span className="field-label">Username</span>
                        <input
                            className="field-input"
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            autoComplete="username"
                            autoFocus
                            required
                        />
                    </label>

                    <label className="field">
                        <span className="field-label">Password</span>
                        <input
                            className="field-input"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            autoComplete="current-password"
                            required
                        />
                    </label>

                    {error && <p className="form-error" role="alert">{error}</p>}

                    <button className="btn" type="submit" disabled={loading}>
                        {loading ? 'Logging in…' : 'Login'}
                    </button>
                </form>

                <p className="auth-alt">
                    Don&apos;t have an account?{' '}
                    <button type="button" className="link-btn" onClick={onGoRegister}>Register</button>
                </p>
            </div>
        </main>
    )

}

export default LoginPage;
