import { useEffect } from 'react'

/**
 * Soft-locks play surfaces (lobby / room / game) to landscape on phones.
 * Login/register stay unlocked. Browsers often ignore orientation.lock without
 * fullscreen — the CSS gate still blocks portrait and asks the user to rotate.
 */
function LandscapeGate() {
    useEffect(() => {
        const orientation = screen.orientation as ScreenOrientation & {
            lock?: (orientation: OrientationLockType) => Promise<void>
        }

        void orientation.lock?.('landscape').catch(() => {
            // Not allowed outside fullscreen / unsupported (e.g. iOS Safari)
        })

        return () => {
            try {
                orientation.unlock?.()
            } catch {
                // ignore
            }
        }
    }, [])

    return (
        <div className="rotate-device-gate" role="dialog" aria-modal="true" aria-label="Rotate device">
            <div className="rotate-device-gate__card">
                <div className="rotate-device-gate__icon" aria-hidden>
                    <svg viewBox="0 0 64 64" className="h-14 w-14 text-gold">
                        <rect
                            x="18"
                            y="8"
                            width="28"
                            height="48"
                            rx="4"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="3"
                        />
                        <path
                            d="M8 28c0-10 8-18 18-18"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="3"
                            strokeLinecap="round"
                        />
                        <path d="M22 14l4-6 4 6" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                </div>
                <p className="text-xs uppercase tracking-widest text-gold">Thirteen</p>
                <h2 className="mt-2 text-xl font-bold">Rotate your phone</h2>
                <p className="mt-2 max-w-xs text-sm text-muted">
                    Lobby, tables, and play require landscape. Turn your device sideways to continue.
                </p>
            </div>
        </div>
    )
}

export default LandscapeGate
