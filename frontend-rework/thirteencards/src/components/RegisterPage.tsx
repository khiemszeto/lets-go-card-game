import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { registerPlayer } from '../api/authApi'

const formSchema = z.object({
    username: z
        .string()
        .trim()
        .min(6, 'Username must be at least 6 characters'),
    email: z
        .string()
        .trim()
        .email('Invalid email format'),
    password: z
        .string()
        .min(6, 'Password must be at least 6 characters'),
});

type Props = {
    onGoLogin: () => void
    onRegistered: () => void
}

function RegisterPage({onGoLogin, onRegistered}: Props) {
    const {
        register,
        handleSubmit,
        formState: { errors, isSubmitting },
        setError,
    } = useForm({ resolver: zodResolver(formSchema) });

    async function submitForm(data: z.infer<typeof formSchema>) {
        try {
            await registerPlayer(data)
            onRegistered()

        } catch (err) {
            setError('root', {
                message: err instanceof Error ? err.message : 'Registration failed',
            })

        }
    }

    return(
        <>
            <div>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    <h1>Register</h1>
                    <form
                        onSubmit={handleSubmit(submitForm)}
                        style={{ display: 'flex', flexDirection: 'column', width: '260px', gap: 8 }}
                    >
                        <input {...register('username')} placeholder="Username" />
                        {errors.username && <p style={{ color: 'red' }}>{errors.username.message}</p>}
                        <input {...register('email')} type="email" placeholder="Email" />
                        {errors.email && <p style={{ color: 'red' }}>{errors.email.message}</p>}
                        <input {...register('password')} type="password" placeholder="Password" />
                        {errors.password && <p style={{ color: 'red' }}>{errors.password.message}</p>}
                        {errors.root && <p style={{ color: 'red' }}>{errors.root.message}</p>}
                        <button type="submit" disabled={isSubmitting}>
                            {isSubmitting ? 'Creating account…' : 'Register'}
                        </button>
                    </form>
                    <p>
                        Already have an account?{' '}
                        <a onClick={(e) => { e.preventDefault(); onGoLogin(); }}>
                            Login
                        </a>
                    </p>
                </div>

            </div>
        </>

        )


}

export default RegisterPage;