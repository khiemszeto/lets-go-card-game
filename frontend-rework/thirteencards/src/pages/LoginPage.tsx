import {  useState, type FormEvent } from 'react'
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
        <div className="grid min-h-[70svh] place-items-center p-8">
            <div className="card auth-card bg-base-200 shadow-xl">
                <div className="card-body gap-[0.55em]">
                    <p className="text-[0.72em] uppercase tracking-widest text-gold">Tiến Lên</p>
                    <h2 className="card-title">Enter the Lobby</h2>
                    <p className="text-[0.9em] text-muted">Sign in to play Thirteen Cards</p>


                <form onSubmit={handleSubmit} className="mt-[1.2em] flex flex-col gap-[0.8em]">

                        <input
                            className="input input-bordered w-full"
                            type="text"
                            placeholder="Username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                        />


                        <input
                            className="input input-bordered w-full"
                            type="password"
                            placeholder="Password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    {error && <div className="alert alert-error">{error}</div>}
                    <button className="btn btn-primary w-full" type="submit" disabled={loading}>
                        {loading ? 'Logging in…' : 'Login'}
                    </button>
                </form>
            <p className="mt-[1.2em] text-center text-[0.9em]">
                Don&apos;t have an account?{' '}
                <button className="btn btn-link btn-sm" type="button" onClick={onGoRegister}>Register</button>
            </p>
                </div>
            </div>
        </div>
    )

}

export default LoginPage;