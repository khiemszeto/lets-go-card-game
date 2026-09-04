import { useState, useEffect, useRef } from 'react'
import Header from './components/Header.tsx'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import LobbyPage from './pages/LobbyPage'
import Footer from './components/Footer'
import LandscapeGate from './components/LandscapeGate'
import { saveSession, getUsername, getBalance, clearSession } from './auth/authStorage'
import { fetchMe, logoutPlayer } from './api/authApi'

// Shared lock name — every tab uses the same string so the browser knows they compete
const GAME_TAB_LOCK = 'thirteencards-game'

type AuthScreen = 'login' | 'register'

type AuthStatus = 'loading' | 'anonymous' | 'authenticated'

// Tab lock lifecycle for logged-in users:
//   pending  → waiting to acquire the game lock (show "Connecting…")
//   owner    → this tab owns the lock (LobbyPage + WebSocket allowed)
//   blocked  → another tab holds the lock (show "Game already open")
type TabLockState = 'pending' | 'owner' | 'blocked'

function App() {
    const [tabLock, setTabLock] = useState<TabLockState>('pending')
    const [authScreen, setAuthScreen] = useState<AuthScreen>('login')

    // Bumped on login/logout and on storage events so effects re-run and re-read localStorage
    const [sessionTick, setSessionTick] = useState(0)

    const [authStatus, setAuthStatus] = useState<AuthStatus>('loading')
    const isLoggedIn = (authStatus === 'authenticated')

    // Increments on each lock attempt; stale async callbacks from old effects bail out
    const claimIdRef = useRef(0)

    // check if browser supports Web Locks API
    const hasTabLocks = typeof navigator !== 'undefined' && Boolean(navigator.locks)

    // Lobby / room / game — not login/register
    const inPlaySurface =
        isLoggedIn && (!hasTabLocks || tabLock === 'owner' || tabLock === 'pending' || tabLock === 'blocked')

    function refreshSession() {
        setSessionTick((n) => n + 1)
    }

    async function handleLogout() {
        try {
            await logoutPlayer()
        } catch {}

        clearSession()
        setAuthStatus('anonymous')
        setAuthScreen('login')
    }

    // Use Effect for restore session
    useEffect(() => {
        let cancelled = false;

        async function restoreSession() {
            try {
                const me = await fetchMe()
                if (cancelled) return

                saveSession(me.username, me.balance)
                setAuthStatus('authenticated')
            } catch {
                if (cancelled) return
                clearSession()
                setAuthStatus('anonymous')
            }
        }

        void restoreSession()
        return () => {
            cancelled = true
        }
    }, [sessionTick]);


    /*
     * Cross-tab auth sync
     * localStorage is shared by all tabs on the same site.
     * The `storage` event fires in OTHER tabs when one tab changes localStorage
     * (not in the tab that made the change).
     * Example: Tab A signs out → Tab B hears it → refreshSession() → UI updates.
     */
    useEffect(() => {
        function onStorage(e: StorageEvent) {
            if (e.key === 'username') {
                refreshSession()// Force re-render. When lock effect deps change, React runs the 1st effect's
                // cleanup first (releaseLock → lock released), then runs the 2nd effect setup.
                // On logout, the 2nd setup returns early (!isLoggedIn) — no new lock claimed.
            }
        }
        window.addEventListener('storage', onStorage)
        return () => window.removeEventListener('storage', onStorage)
    }, [])

    /*
     * One game tab per logged-in player (Web Locks API)
     *
     * Flow:
     *   1. Tab tries to acquire exclusive lock "thirteencards-game"
     *   2. First tab  → owner  → LobbyPage mounts → WebSocket connects
     *   3. Second tab → blocked → waits in queue (no LobbyPage, no WebSocket)
     *   4. First tab closes → lock released → second tab becomes owner automatically
     *
     * claimIdRef prevents React Strict Mode double-mount from leaving the tab stuck:
     *   cleanup bumps the id so the previous effect's async code stops updating state.
     */
    useEffect(() => {
        if (!isLoggedIn || !hasTabLocks) return

        const claimId = ++claimIdRef.current
        let releaseLock: (() => void) | undefined

        async function claimTab() {
            if (claimIdRef.current !== claimId) return
            setTabLock('pending')

            // If another tab already holds our lock, show the blocked screen while we wait
            try {
                const state = await navigator.locks.query()
                if (claimIdRef.current !== claimId) return
                if (state.held.some((lock) => lock.name === GAME_TAB_LOCK)) {
                    setTabLock('blocked')
                }
            } catch {
                // Older browsers without locks.query — skip straight to request
            }

            // Blocks until this tab owns the lock; resolves when releaseLock() is called
            await navigator.locks.request(GAME_TAB_LOCK, async () => {
                if (claimIdRef.current !== claimId) return
                setTabLock('owner')
                await new Promise<void>((resolve) => {
                    releaseLock = resolve
                })
            })
        }

        void claimTab()

        return () => {
            // Invalidate in-flight claimTab from this effect run (Strict Mode / re-login)
            claimIdRef.current += 1
            // Release the lock so another tab can take over
            releaseLock?.()
        }
    }, [isLoggedIn, sessionTick, hasTabLocks])

    if (authStatus === 'loading') {
        return (
            <div className="app-shell">
                <main className="grid min-h-[50svh] place-items-center">
                    <p className="text-muted">Loading…</p>
                </main>
            </div>
        )
    }

    // Full-page blocked UI — LobbyPage is NOT mounted, so no WebSocket is opened
    if (isLoggedIn && hasTabLocks && tabLock === 'blocked') {
        return (
            <div className="app-shell landscape-play">
                <LandscapeGate />
                <Header username={getUsername()} balance={getBalance()} />
                <main className="grid flex-1 place-items-center p-8">
                    <div className="card w-full max-w-md bg-base-200 shadow-xl">
                        <div className="card-body text-center">
                            <h2 className="card-title justify-center text-xl">Game already open</h2>
                            <p className="text-sm text-muted">
                                This account is active in another tab. Close that tab and this
                                page will unlock automatically.
                            </p>
                        </div>
                    </div>
                </main>
                <Footer />
            </div>
        )
    }

    return (
        <div className={['app-shell', inPlaySurface ? 'landscape-play' : ''].join(' ')}>
            {inPlaySurface && <LandscapeGate />}
            <Header
                username={isLoggedIn ? getUsername() : null}
                balance={isLoggedIn ? getBalance() : null}
                onLogout={() => handleLogout()}
            />
            <main className="app-main">
                {/* Not logged in — portrait or landscape OK */}
                {!isLoggedIn && authScreen === 'login' && (
                    <LoginPage
                        onGoRegister={() => setAuthScreen('register')}
                        onSuccess={() => {
                            setAuthStatus('authenticated')
                            refreshSession() // token saved → re-render → tab lock effect runs
                        }}
                    />
                )}
                {!isLoggedIn && authScreen === 'register' && (
                    <RegisterPage
                        onGoLogin={() => setAuthScreen('login')}
                        onRegistered={() => setAuthScreen('login')}
                    />
                )}

                {/* Logged in but still acquiring the tab lock */}
                {isLoggedIn && hasTabLocks && tabLock === 'pending' && (
                    <div className="grid min-h-[50svh] place-items-center">
                        <p className="text-muted">Connecting…</p>
                    </div>
                )}

                {/* Logged in and owns the lock (or browser has no Web Locks API) */}
                {isLoggedIn && (!hasTabLocks || tabLock === 'owner') && (
                    <LobbyPage />
                )}
            </main>
            <Footer />
        </div>
    )
}

export default App
