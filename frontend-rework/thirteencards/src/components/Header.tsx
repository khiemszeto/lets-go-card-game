
type Props = {
    username?: string | null
    balance?: string | null
    onLogout?: () => void
}

function Header({ username, balance, onLogout }: Props) {
    return (
        <header className="site-header shrink-0 border-b border-white/10 bg-base-100/80 backdrop-blur">
            <div className="mx-auto flex w-full items-center justify-between px-4 py-3 sm:px-6 sm:py-6">
                <div>
                    <p className="site-header__eyebrow text-xs uppercase tracking-widest text-primary">
                        Competitive Card Game
                    </p>
                    <h1 className="site-header__title text-lg font-bold sm:text-xl">Thirteen</h1>
                </div>
                {username && (
                    <div className="flex items-center gap-1.5 sm:gap-2">
                        <span className="badge badge-neutral badge-sm sm:badge-md">{username}</span>
                        {balance != null && (
                            <span className="badge badge-success badge-soft badge-md sm:badge-lg">
                                {balance} chips
                            </span>
                        )}
                        {onLogout && (
                            <button
                                type="button"
                                className="btn btn-ghost btn-xs sm:btn-sm"
                                onClick={onLogout}
                            >
                                Sign out
                            </button>
                        )}
                    </div>
                )}
            </div>
        </header>
    )
}

export default Header
