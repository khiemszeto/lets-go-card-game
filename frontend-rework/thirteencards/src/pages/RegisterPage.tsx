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

            <div className="grid min-h-[70svh] place-items-center p-8">
                <div className="card auth-card bg-base-200 shadow-xl">
                    <div className="card-body gap-[0.55em]">
                        <p className="text-[0.72em] uppercase tracking-widest text-primary">Tiến Lên</p>
                        <h2 className="card-title">Create account</h2>
                        <p className="text-[0.9em] text-muted">Register to play Thirteen Cards</p>

                        <form
                            className="mt-[1.2em] flex flex-col gap-[0.8em]"
                            onSubmit={handleSubmit(submitForm)}
                        >
                            <input
                                className="input input-bordered w-full"
                                {...register('username')} placeholder="Username" />
                            {errors.username && <p className="text-[0.9em] text-error">{errors.username.message}</p>}
                            <input
                                className="input input-bordered w-full"
                                {...register('email')} type="email" placeholder="Email" />
                            {errors.email && <p className="text-[0.9em] text-error">{errors.email.message}</p>}
                            <input
                                className="input input-bordered w-full"
                                {...register('password')} type="password" placeholder="Password" />

                            {errors.password && <p className="text-[0.9em] text-error">{errors.password.message}</p>}


                            {errors.root && <p className="text-[0.9em] text-error">{errors.root.message}</p>}
                            <button className="btn btn-primary w-full" type="submit" disabled={isSubmitting}>
                                {isSubmitting ? 'Creating account…' : 'Register'}
                            </button>

                        </form>
                        <p className="mt-[1.2em] text-center text-[0.9em]">
                            Already have an account?{' '}
                            <button type="button" className="btn btn-link btn-sm" onClick={onGoLogin}>
                                Login
                            </button>
                        </p>

                    </div>
                </div>
            </div>


        )


}

export default RegisterPage;