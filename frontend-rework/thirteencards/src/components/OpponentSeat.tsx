import type { RoomPlayer } from '../types/game'
import BackSideOfCard from './BackSideOfCard'

type Side = 'left' | 'right' | 'top'

type Props = {
    player: RoomPlayer | null
    side?: Side
}

const FULL_HAND = 13

function fanLengthCss(count: number) {
    const extra = Math.max(count - 1, 0)
    return `calc(var(--opp-card-w) + ${extra} * var(--opp-card-w) * 0.32)`
}

function OpponentSeat({ player, side = 'top' }: Props) {
    const isSide = side === 'left' || side === 'right'
    const count = player?.numberOfCards ?? FULL_HAND
    const fanLength = fanLengthCss(player ? count : FULL_HAND)

    if (!player) {
        if (isSide) {
            return (
                <div
                    className="w-[var(--side-col)] shrink-0"
                    style={{ height: fanLength }}
                    aria-hidden
                />
            )
        }

        return (
            <div
                className="flex shrink-0 flex-col items-center gap-1.5"
                style={{ height: 'var(--top-seat-h)' }}
                aria-hidden
            />
        )
    }

    const hand = (
        <div className="flex" style={{ width: fanLength }}>
            {Array.from({ length: count }).map((_, i) => (
                <div
                    key={i}
                    className={i === 0 ? undefined : 'ml-[calc(var(--opp-card-w)*-0.68)]'}
                >
                    <BackSideOfCard compact />
                </div>
            ))}
        </div>
    )

    return (
        <div
            className={[
                'flex flex-col items-center gap-1.5',
                isSide ? 'w-[var(--side-col)] max-w-[var(--side-col)]' : '',
            ].join(' ')}
        >
            <p
                className={[
                    'truncate text-center text-[length:clamp(0.6rem,1.4vw,0.75rem)] text-muted',
                    isSide ? 'w-full' : 'max-w-36',
                ].join(' ')}
            >
                {player.username}{' '}
                <span className="opacity-70">({count})</span>
            </p>

            {isSide ? (
                <div
                    className="relative"
                    style={{
                        width: 'var(--opp-card-h)',
                        height: fanLength,
                    }}
                >
                    <div
                        className="absolute left-1/2 top-1/2"
                        style={{
                            width: fanLength,
                            transform: `translate(-50%, -50%) rotate(${side === 'left' ? -90 : 90}deg)`,
                        }}
                    >
                        {hand}
                    </div>
                </div>
            ) : (
                hand
            )}
        </div>
    )
}

export default OpponentSeat
