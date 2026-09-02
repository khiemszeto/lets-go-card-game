import { useState } from 'react'
import Header from './components/Header.tsx'
import CardOrbit from './components/CardOrbit'
import LoginPage from './components/LoginPage'
import RegisterPage from './components/RegisterPage'

type Screen = 'login' | 'register' | 'lobby'


function App() {
    const [screen, setScreen] = useState<Screen>('login')


  return (
    <>
        <Header></Header>
        <CardOrbit />

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
        {screen === 'lobby' && <div>Lobby coming soon…</div>}
    </>
  )
}

export default App
