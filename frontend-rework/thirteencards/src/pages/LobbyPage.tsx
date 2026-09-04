import { useState, useRef, useEffect } from 'react'
import type { FormEvent } from 'react'
import type {
    RoomSummary,
    RoomStateMessage,
    LeaveRoomMessage,
    ErrorMessage,
    CountDownMessage,
    GameStartMessage,
    HandMessage,
    PlayResultMessage,
    PassResultMessage,
    GameOverMessage,
    LeftDuringGameMessage,
} from '../types/game'
import { GameSocket } from '../websocket/GameSocket'
import { fetchRooms } from '../api/roomApi'
import TableCard from '../components/TableCard'
import RoomPage from './RoomPage'
import GamePage from './GamePage'
import type {GameState} from "./GamePage";

function isRoomState(msg: unknown): msg is RoomStateMessage {
    return typeof msg === 'object' && msg !== null && (msg as RoomStateMessage).type === 'ROOM_STATE'
}

function isLeftRoom(msg: unknown): msg is LeaveRoomMessage {
    return typeof msg === 'object' && msg !== null && (msg as LeaveRoomMessage).type === 'LEAVE_ROOM'
}

function isError(msg: unknown): msg is ErrorMessage {
    return typeof msg === 'object' && msg !== null && (msg as ErrorMessage).type === 'ERROR'
}

function isCountDown(msg: unknown): msg is CountDownMessage {
    return typeof msg === 'object' && msg !== null && (msg as CountDownMessage).type === 'COUNTDOWN'
}

function isGameStart(msg: unknown): msg is GameStartMessage {
    return typeof msg === 'object' && msg !== null && (msg as GameStartMessage).type === 'GAME_START'
}

function isHand(msg: unknown): msg is HandMessage {
    return typeof msg === 'object' && msg !== null && (msg as HandMessage).type === 'HAND'
}

function isPlayResult(msg: unknown): msg is PlayResultMessage {
    return typeof msg === 'object' && msg !== null && (msg as PlayResultMessage).type === 'PLAY_RESULT'
}

function isPassResult(msg: unknown): msg is PassResultMessage {
    return typeof msg === 'object' && msg !== null && (msg as PassResultMessage).type === 'PASS_RESULT'
}

function isGameOver(msg: unknown): msg is GameOverMessage {
    return typeof msg === 'object' && msg !== null && (msg as GameOverMessage).type === 'GAME_OVER'
}

function isLeftDuringGame(msg: unknown): msg is LeftDuringGameMessage {
    return typeof msg === 'object' && msg !== null && (msg as LeftDuringGameMessage).type === 'LEFT_DURING_GAME'
}

function LobbyPage() {
    const [webSocketConnected, setWebSocketConnected] = useState(false)
    const [rooms, setRooms] = useState<RoomSummary[]>([])
    const [roomState, setRoomState] = useState<RoomStateMessage | null>(null)
    const [joinInput, setJoinInput] = useState('')
    const [error, setError] = useState<string | null>(null)
    const [statusText, setStatusText] = useState<string | null>(null)
    const [countdown, setCountdown] = useState<number | null>(null)
    const [gameState, setGameState] = useState<GameState | null> (null)

    const socketRef = useRef<GameSocket | null>(null)

    async function fetchRoomsData() {
        try {
            const listOfRooms = await fetchRooms()
            setRooms(listOfRooms)
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to fetch rooms')
        }
    }

    function handleCreateRoom() {
        if (!webSocketConnected) {
            setError('Not connected to server')
            return
        }
        setError(null)
        setStatusText('Creating new table…')
        socketRef.current?.createRoom()
    }

    function handleJoinRoom(roomId: string) {
        if (!webSocketConnected) {
            setError('Not connected to server')
            return
        }
        setError(null)
        setStatusText(`Joining table ${roomId}…`)
        socketRef.current?.joinRoom(roomId)
    }

    function handleJoinById(e: FormEvent) {
        e.preventDefault()
        const roomId = joinInput.trim()
        if (!roomId) {
            setError('Enter a table number')
            return
        }
        handleJoinRoom(roomId)
    }

    useEffect(() => {
        const socket = new GameSocket()
        socketRef.current = socket

        socket.connect(
            (message) => {
                if (isError(message)) {
                    setError(message.message)
                    setStatusText(null)
                    return
                }

                if (isGameStart(message)) {
                    setCountdown(null)
                    setGameState({
                        roomId: message.roomId,
                        players: message.players,
                        currentPlayerId: message.currentPlayerId,
                        myHand: [],
                        selected: [],
                        lastPlay: null,
                        winnerId: null,
                        endSeconds:  null
                    })
                    return
                }

                if (isHand(message)) {
                    setGameState((prevState) => {
                        if (!prevState || prevState.roomId !== message.roomId) return prevState
                        return { ...prevState, myHand: message.cards, selected: [] }
                    })
                    return
                }

                if (isPlayResult(message)) {
                    setGameState((prev) => {
                        if (!prev || prev.roomId !== message.roomId) return prev
                        return {
                            ...prev,
                            players: message.players,
                            currentPlayerId: message.currentPlayerId,
                            lastPlay: message.trickReset
                                ? null
                                : { playerId: message.playerId, cards: message.cardsPlayed },
                            selected: [],
                        }
                    })
                    return
                }

                if (isPassResult(message)) {
                    setGameState((prev) => {
                        if (!prev || prev.roomId !== message.roomId) return prev
                        return {
                            ...prev,
                            currentPlayerId: message.currentPlayerId,
                            lastPlay: message.trickReset ? null : prev.lastPlay,
                            selected: [],
                        }
                    })
                    return
                }

                if (isGameOver(message)) {
                    setGameState((prev) => {
                        if (!prev || prev.roomId !== message.roomId) return prev
                        return { ...prev, winnerId: message.winnerId,  endSeconds: 5}
                    })

                    return
                }

                if (isLeftDuringGame(message)) {
                    setGameState((prev) => {
                        if (!prev || prev.roomId !== message.roomId) return prev
                        return {
                            ...prev,
                            players: message.players,
                            currentPlayerId: message.currentPlayerId,
                            lastPlay: message.trickReset ? null : prev.lastPlay,
                        }
                    })

                    return
                }

                if (isCountDown(message)) {
                    setCountdown(message.seconds)
                    return
                }

                if (isRoomState(message)) {
                    setRoomState(message)
                    if (message.readyPlayers < 2) setCountdown(null)

                    setGameState(null)


                    setError(null)
                    setStatusText(null)
                    return
                }

                if (isLeftRoom(message)) {
                    setGameState(null)
                    setRoomState(null)
                    setCountdown(null)
                    setError(null)
                    setStatusText(null)
                    void fetchRoomsData()
                    return
                }
            },
            () => setWebSocketConnected(true),
            () => setWebSocketConnected(false),
        )

        void fetchRoomsData()

        return () => {
            socket.disconnect()
            socketRef.current = null
        }
    }, [])

    useEffect(() => {
        if (roomState !== null) return

        const timer = setInterval(() => {
            void fetchRoomsData()
        }, 2000)

        return () => clearInterval(timer)
    }, [roomState])

    useEffect(() => {
        if (gameState?.endSeconds == null || gameState.endSeconds <= 0) return

        const timer = setTimeout(() => {
            setGameState((prev) => {
                if (prev && prev.endSeconds != null && prev.endSeconds > 0) {
                    return { ...prev, endSeconds: prev.endSeconds - 1 }
                }
                return prev
            })
        }, 1000)

        return () => clearTimeout(timer)
    }, [gameState?.endSeconds])




    return (
        <div
            className={
                gameState || roomState
                    ? 'flex min-h-0 flex-1 flex-col overflow-hidden'
                    : 'hall-root flex min-h-0 w-full flex-1 flex-col overflow-hidden px-2 py-1 sm:mx-auto sm:overflow-auto sm:px-6 sm:py-6'
            }
        >
            {error && (
                <div className={['alert alert-error mb-4', gameState ? 'mx-4 mt-2 shrink-0' : ''].join(' ')}>
                    <span>{error}</span>
                </div>
            )}

            {statusText && (
                <div className={['alert alert-info mb-4', gameState ? 'mx-4 mt-2 shrink-0' : ''].join(' ')}>
                    <span>{statusText}</span>
                </div>
            )}

            {gameState ? (
                <div className="game-shell">
                    <GamePage
                        gamestate={gameState}
                        socketRef={socketRef}
                        onChangeGame={setGameState}
                    />
                </div>
            ) : roomState ? (
                <div className="room-view flex min-h-0 flex-1 flex-col overflow-hidden px-2 py-1 sm:px-4 sm:py-2">
                    <RoomPage
                        roomState={roomState}
                        socketRef={socketRef}
                        countdown={countdown}
                    />
                </div>
            ) : (
                <div className="hall-shell flex min-h-0 flex-1 flex-col gap-2 overflow-hidden sm:gap-4">
                    <div className="hall-shell__toolbar flex shrink-0 flex-col gap-2 sm:mb-2 sm:flex-row sm:flex-wrap sm:items-end sm:justify-between sm:gap-4">
                        <div className="min-w-0">
                            <p className="text-[10px] uppercase tracking-widest text-gold sm:text-xs">Tiến Lên</p>
                            <h2 className="text-base font-bold leading-tight sm:text-2xl">Game Hall</h2>
                            <p className="hidden text-sm text-muted sm:block">
                                Join a table or create your own
                            </p>
                        </div>
                        <div className="hall-shell__actions flex flex-nowrap items-center gap-1.5 overflow-x-auto sm:gap-2">
                            <form
                                onSubmit={handleJoinById}
                                className="flex flex-nowrap items-center gap-1.5 sm:gap-2"
                            >
                                <input
                                    type="text"
                                    value={joinInput}
                                    onChange={(e) => setJoinInput(e.target.value)}
                                    placeholder="Table #"
                                    className="input input-bordered input-sm w-24 sm:w-56"
                                />
                                <button
                                    type="submit"
                                    className="btn btn-outline btn-sm"
                                    disabled={!webSocketConnected}
                                >
                                    Join
                                </button>
                            </form>
                            <span
                                className={[
                                    'badge badge-sm shrink-0',
                                    webSocketConnected ? 'badge-success' : 'badge-warning',
                                ].join(' ')}
                            >
                                {webSocketConnected ? 'On' : '…'}
                            </span>
                            <button
                                type="button"
                                className="btn btn-ghost btn-sm shrink-0"
                                onClick={() => void fetchRoomsData()}
                            >
                                Refresh
                            </button>
                            <button
                                type="button"
                                className="btn btn-primary btn-sm shrink-0"
                                onClick={handleCreateRoom}
                                disabled={!webSocketConnected}
                            >
                                Create
                            </button>
                        </div>
                    </div>

                    <div className="hall-shell__rooms min-h-0 flex-1 overflow-hidden">
                        {rooms.length === 0 ? (
                            <div className="card h-full bg-base-200 shadow-md">
                                <div className="card-body items-center justify-center py-6 text-center sm:py-16">
                                    <p className="text-base font-semibold sm:text-lg">No tables yet</p>
                                    <p className="text-xs text-muted sm:text-sm">
                                        Be the first to create a table and invite friends.
                                    </p>
                                    <button
                                        type="button"
                                        className="btn btn-primary btn-sm mt-3 sm:btn-md sm:mt-4"
                                        onClick={handleCreateRoom}
                                        disabled={!webSocketConnected}
                                    >
                                        Create table
                                    </button>
                                </div>
                            </div>
                        ) : (
                            <div className="hall-shell__track grid h-full grid-cols-1 gap-4 sm:grid-cols-2 sm:gap-6 lg:grid-cols-3 xl:grid-cols-6">
                                {rooms.map((room) => (
                                    <TableCard
                                        key={room.roomId}
                                        roomId={room.roomId}
                                        status={room.status}
                                        playerCount={room.playerCount}
                                        maxPlayers={room.maxPlayers}
                                        joinable={room.joinable}
                                        joinDisabled={!webSocketConnected}
                                        onJoin={
                                            room.joinable
                                                ? () => handleJoinRoom(room.roomId)
                                                : undefined
                                        }
                                    />
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    )
}

export default LobbyPage;
