import { useState } from 'react'
import Header from './components/Header.tsx'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import LobbyPage from './pages/LobbyPage'
import Footer from './components/Footer'
import { getAccessToken, getUsername } from './auth/authStorage'


type Screen = 'login' | 'register' | 'lobby'


function App() {
    const [screen, setScreen] = useState<Screen>(() =>
        getAccessToken() ? 'lobby' : 'login'
    )


  return (
    <div className="flex min-h-svh flex-col">

        <Header username={screen === 'lobby' ? getUsername() : null} />
        <main className="flex-1">
            {screen === 'login' && (
                <LoginPage
                    onGoRegister={() => setScreen('register')}
                    onSuccess={() => setScreen('lobby')}
                />
            )}
            {screen === 'register' && (
                <RegisterPage
                    onGoLogin={() => setScreen('login')}
                    onRegistered={() => setScreen('login')}
                />
            )}
            {screen === 'lobby' &&
                <LobbyPage onLogout={() => setScreen('login')} />}
        </main>
        <Footer />
    </div>
  )
}

export default App
