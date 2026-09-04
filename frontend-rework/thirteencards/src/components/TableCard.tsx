import type { RoomSummary, RoomPlayer } from '../types/game'
import pokerTable from '../assets/poker-table.svg'

type TableCardProps = {
    roomId: string
    status: RoomSummary['status']
    playerCount: number
    joinable: boolean
    maxPlayers: number
    onJoin?: () => void
    players?: RoomPlayer[]
    joinDisabled?: boolean
    size?: 'sm' | 'lg'
    countdown?: number | null
}

function statusLabel(status: RoomSummary['status']) {
    switch (status) {
        case 'WAITING':
            return 'Waiting'
        case 'COUNTING_DOWN':
            return 'Starting…'
        case 'STARTED':
            return 'In game'
    }
}

function Seat({ filled, label }: { filled: boolean; label?: string }) {
    return (
        <div className="flex flex-col items-center gap-0.5">
            <div
                className={[
                    'aspect-square w-full rounded-full border-2',
                    filled ? 'bg-error border-error' : 'border-white/40 bg-transparent',
                ].join(' ')}
            />
            {label && (
                <span className="max-w-[4.5rem] truncate text-center text-[9px] leading-tight text-white/80 sm:max-w-16 sm:text-[10px]">
                    {label}
                </span>
            )}
        </div>
    )
}

function buildSeats(maxPlayers: number, playerCount: number, players?: RoomPlayer[]) {
    const seats = []
    for (let i = 1; i <= maxPlayers; i++) {
        seats.push({ seat: i, filled: false, label: undefined as string | undefined })
    }

    if (players && players.length > 0) {
        for (const player of players) {
            const slot = seats[player.seat - 1]
            if (slot) {
                slot.filled = true
                slot.label = player.username
            }
        }
    } else {
        for (let i = 0; i < playerCount; i++) {
            seats[i].filled = true
        }
    }

    return seats
}

const SEAT_POSITIONS: Record<number, { left: string; top: string }> = {
    1: { left: '50%', top: '18%' },
    2: { left: '89%', top: '50%' },
    3: { left: '50%', top: '82%' },
    4: { left: '11%', top: '50%' },
}

function TableCard({
    roomId,
    status,
    playerCount,
    joinable,
    maxPlayers,
    onJoin,
    players,
    joinDisabled,
    size = 'sm',
    countdown,
}: TableCardProps) {
    const seats = buildSeats(maxPlayers, playerCount, players)
    const isLarge = size === 'lg'


    return (
        <div
            className={[
                'card bg-base-200 shadow-md',
                isLarge ? 'w-full max-w-xl p-3 sm:p-5' : 'w-full max-w-[280px] p-3',
            ].join(' ')}
        >
            <div className="relative aspect-5/3 w-full">
                <img
                    src={pokerTable}
                    alt=""
                    className="pointer-events-none absolute inset-0 h-full w-full object-contain"
                />

                {[1, 2, 3, 4].map((seatNum) => {
                    const pos = SEAT_POSITIONS[seatNum]
                    const seat = seats[seatNum - 1]
                    return (
                        <div
                            key={seatNum}
                            className="absolute w-[12%] -translate-x-1/2 -translate-y-1/2 sm:w-[10%]"
                            style={{ left: pos.left, top: pos.top }}
                        >
                            <Seat filled={seat.filled} label={seat.label} />
                        </div>
                    )
                })}

                <div className="pointer-events-none absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 px-2 text-center">
                    <p className="text-[10px] uppercase tracking-wide text-white/90">
                        Table {roomId}
                    </p>

                    {countdown != null ? (
                        <p className={`font-bold text-white ${isLarge ? 'text-2xl sm:text-3xl' : 'text-xl'}`}>
                            {countdown}
                        </p>
                    ) : (
                        <p className={`font-semibold text-white ${isLarge ? 'text-sm sm:text-base' : 'text-sm'}`}>
                            {statusLabel(status)}
                        </p>
                    )}

                    <p className="text-[10px] text-white/80">
                        {playerCount}/{maxPlayers} players
                    </p>
                </div>
            </div>

            {joinable && onJoin && (
                <button
                    className="btn btn-primary btn-sm mt-3 w-full"
                    onClick={onJoin}
                    disabled={joinDisabled}
                >
                    Join table
                </button>
            )}
        </div>
    )
}

export default TableCard
