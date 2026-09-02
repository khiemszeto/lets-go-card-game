
type Props = { username?: string | null }

function Header({ username }: Props) {
    return (
        <header className="border-b border-white/10 bg-base-100/80 backdrop-blur">
            <div className="mx-auto flex max-w-6xl items-center justify-between px-5 py-4">
                <div>
                    <p className="text-xs uppercase tracking-widest text-primary">Thirteen Cards</p>
                    <h1 className="text-xl font-bold">Thirteen</h1>
                </div>
                {username && (
                    <span className="badge badge-neutral">{username}</span>
                )}
            </div>
        </header>
    )
}

export default Header