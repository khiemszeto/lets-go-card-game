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
            <div className="card w-full max-w-md bg-base-200 shadow-xl">
                <div className="card-body">
                    <p className="text-xs uppercase tracking-widest text-gold">Tiến Lên</p>
                    <h2 className="card-title text-2xl">Enter the Lobby</h2>
                    <p className="text-sm text-muted">Sign in to play Thirteen Cards</p>


                <form onSubmit={handleSubmit} className="mt-4 flex flex-col gap-3">

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
            <p className="mt-4 text-center text-sm">
                Don&apos;t have an account?{' '}
                <button className="btn btn-link btn-sm" type="button" onClick={onGoRegister}>Register</button>
            </p>
                </div>
            </div>
        </div>
    )

}

export default LoginPage;