import { useState, useRef, useEffect} from "react";
import type {FormEvent} from 'react'
import type {RoomSummary, RoomStateMessage, LeaveRoomMessage, RoomPlayer, ErrorMessage} from '../types/game'
import {GameSocket} from '../websocket/GameSocket'
import {fetchRooms} from '../api/roomApi'
import {clearAuth, getUsername} from '../auth/authStorage'

type Props = {onLogout: () => void}

// type guards
function isRoomState(msg: unknown): msg is RoomStateMessage {
    return typeof msg === 'object' && msg !== null && (msg as RoomStateMessage).type === 'ROOM_STATE'
}

function isLeftRoom(msg: unknown): msg is LeaveRoomMessage {
    return typeof msg === 'object' && msg !== null && (msg as LeaveRoomMessage).type === 'LEAVE_ROOM'
}

function isError(msg: unknown): msg is ErrorMessage {
    return typeof msg === 'object' && msg !== null && (msg as ErrorMessage).type === 'ERROR'
}

function LobbyPage({onLogout} :Props) {
    const [webSocketConnected, setWebSocketConnected] = useState<boolean>(false)
    const [rooms, setRooms] = useState<RoomSummary[]>([])
    const [roomState, setRoomState] = useState<RoomStateMessage | null>(null) // create a room, someone join a room, someone leave a room
    const [joinInput, setJoinInput] = useState<string>('')
    const [error, setError] = useState<string | null>(null)
    const [statusText, setStatusText] = useState<string>('')

    const socketRef = useRef<GameSocket | null>(null)

    async function fetchRoomsData() {
        try {
            const listOfRooms = await fetchRooms();
            setRooms(listOfRooms)
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to fetch rooms')
        }
    }

    const handleJoinRoom = (e: FormEvent)=> {
        e.preventDefault()
        const roomId = joinInput.trim()
        if (!roomId) {
            setError('Enter a table number')
            return
        }

        if (!webSocketConnected) {
            setError('Not connected')
            return
        }

        setStatusText(`Joining table ${roomId}...`)
        socketRef.current?.joinRoom(roomId)
    }

    useEffect(() => {
        const socket = new GameSocket()
        socketRef.current = socket

        socket.connect(
            (message) => {
                if (isError(message)) {
                    setError(message.message)
                    return
                }

                if (isRoomState(message)) {
                    setRoomState(message)
                    setError(null)
                    setStatusText('')
                    return
                }
                if (isLeftRoom(message)) {
                    setRoomState(null)
                    setError(null)
                    setStatusText('')
                    fetchRoomsData()
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
    }, []);

    useEffect(()=>{
        if (roomState !== null) return;

        const timer = setInterval(() => {
            fetchRoomsData();
        }, 2000)

        return () => clearInterval(timer)
    },[roomState])








    return (
        <>
        <div>
            <p>{webSocketConnected? "Connected" : "Connecting..."}</p>
            <button
                type="button"
                onClick={() => {
                    socketRef.current?.disconnect()
                    clearAuth()
                    onLogout()
                }}
            >
                Sign out
            </button>

            {/*error first*/}
            {error && <p style={{color: 'red'}}>{error}</p>}
            {statusText && <p>{statusText}</p>}

            {/* in the hall*/}
            { !roomState &&
                <div>
                    <ul>
                        {
                            rooms.map((room : RoomSummary) => (
                                <li key={room.roomId}>
                                    Table #{room.roomId} - Players: {room.playerCount} / {room.maxPlayers} - Status: {room.status}
                                    {
                                        room.joinable && (

                                            <button
                                                type="button"
                                                disabled={!webSocketConnected}
                                                onClick={() => {
                                                    setStatusText(`Joining table ${room.roomId}...`)
                                                    socketRef.current?.joinRoom(room.roomId)
                                                }}
                                            >
                                                Join
                                            </button>
                                        )
                                    }
                                </li>
                            ))
                        }
                    </ul>


                    <p>Number of tables: {rooms.length}</p>
                    <button
                        type="button"
                        onClick={() => {
                            setStatusText('Creating new table...')
                            socketRef.current?.createRoom()}}
                        disabled={!webSocketConnected}>
                        Create new Table
                    </button>
                    <button onClick={() => fetchRoomsData()}>Refresh</button>

                    <form onSubmit={handleJoinRoom}>
                        <input type="text" value={joinInput}
                               onChange={(e) => setJoinInput(e.target.value)}
                               placeholder="Enter room id"/>
                        <button type="submit" disabled={!webSocketConnected}>Join</button>
                    </form>
                </div>

            }

            {/* in a room*/}
            { roomState &&
                <div>
                    <h1>{"Room " + roomState.roomId}</h1>
                    <ul>
                        {roomState.players.map((player: RoomPlayer) => {
                            const isMe = player.username === getUsername()

                            return (
                                <li key={player.playerId}>
                                    player: {player.playerId} - {player.username}
                                    {
                                        !player.ready && isMe && (
                                            <button onClick={() => socketRef.current?.ready()}>Ready</button>
                                        )
                                    }
                                    {
                                        player.ready && isMe && (
                                            <button onClick={() => socketRef.current?.notReady()}>Not Ready</button>
                                        )
                                    }
                                    {
                                        !isMe && (
                                            <span> - {player.ready ? "ready" :"not ready"}</span>
                                        )
                                    }
                                </li>
                            )
                        })}
                    </ul>

                    <button type="button" onClick={() => socketRef.current?.leaveRoom()}>
                        Leave table
                    </button>
                </div>

            }
        </div>
        </>
    )
}

export default LobbyPage;