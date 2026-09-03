import type { RefObject } from 'react'
import TableCard from '../components/TableCard'
import { getUsername } from '../auth/authStorage'
import type { RoomPlayer, RoomStateMessage } from '../types/game'
import type { GameSocket } from '../websocket/GameSocket'

type Props = {
    roomState: RoomStateMessage
    socketRef: RefObject<GameSocket | null>
    countdown: number | null
}

function RoomPage({ roomState, socketRef, countdown }: Props) {
    const me = roomState.players.find((p: RoomPlayer) => p.username === getUsername())
    const readyCount = roomState.players.filter((p) => p.ready).length

    return (
        <div className="room-shell mx-auto flex h-full min-h-0 w-full max-w-5xl flex-col gap-2 overflow-hidden">
            <div className="room-shell__title shrink-0 text-center">
                <p className="text-[10px] uppercase tracking-widest text-gold sm:text-xs">At the table</p>
                <h2 className="text-base font-bold leading-tight sm:text-2xl">
                    Table {roomState.roomId}
                </h2>
                <p className="text-xs text-muted sm:text-sm">
                    {readyCount}/{roomState.players.length} players ready
                </p>
            </div>

            <div className="room-shell__body flex min-h-0 flex-1 flex-col gap-2 overflow-hidden">
                <div className="room-shell__table mx-auto min-h-0 w-full max-w-md shrink-0 sm:max-w-xl">
                    <TableCard
                        roomId={roomState.roomId}
                        status={countdown != null ? 'COUNTING_DOWN' : 'WAITING'}
                        playerCount={roomState.players.length}
                        maxPlayers={4}
                        joinable={false}
                        players={roomState.players}
                        size="lg"
                        countdown={countdown}
                    />
                </div>

                <div className="room-shell__side flex min-h-0 flex-1 flex-col gap-2">
                    <ul className="room-shell__players min-h-0 flex-1 space-y-1 overflow-y-auto sm:space-y-2">
                        {roomState.players.map((player: RoomPlayer) => {
                            const isMe = player.username === getUsername()
                            return (
                                <li
                                    key={player.playerId}
                                    className="flex items-center justify-between gap-2 rounded-lg bg-base-200 px-2.5 py-1.5 sm:px-4 sm:py-2.5"
                                >
                                    <span className="min-w-0 truncate text-sm font-medium sm:text-base">
                                        {player.username}
                                        {isMe && (
                                            <span className="ml-1.5 text-[10px] text-muted sm:ml-2 sm:text-xs">
                                                (you)
                                            </span>
                                        )}
                                    </span>
                                    <span
                                        className={[
                                            'badge badge-sm shrink-0',
                                            player.ready ? 'badge-success' : 'badge-ghost',
                                        ].join(' ')}
                                    >
                                        {player.ready ? 'Ready' : 'Not ready'}
                                    </span>
                                </li>
                            )
                        })}
                    </ul>

                    <div className="room-shell__actions flex shrink-0 flex-row gap-2">
                        {me && !me.ready && (
                            <button
                                type="button"
                                className="btn btn-primary btn-sm flex-1 sm:btn-md"
                                onClick={() => socketRef.current?.ready()}
                            >
                                Ready
                            </button>
                        )}
                        {me && me.ready && (
                            <button
                                type="button"
                                className="btn btn-outline btn-sm flex-1 sm:btn-md"
                                onClick={() => socketRef.current?.notReady()}
                            >
                                Not ready
                            </button>
                        )}
                        <button
                            type="button"
                            className="btn btn-ghost btn-sm flex-1 sm:btn-md"
                            onClick={() => socketRef.current?.leaveRoom()}
                        >
                            Leave
                        </button>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default RoomPage
