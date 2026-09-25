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
    // Full-hand slot so chip row stays put while cards shrink
    const fullFanLength = fanLengthCss(FULL_HAND)

    if (!player) {
        if (isSide) {
            return (
                <div
                    className="flex w-[var(--side-col)] shrink-0 flex-col items-center gap-1"
                    style={{ height: `calc(${fullFanLength} + 2.75rem)` }}
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

    const chips = (
        <p className="h-5 truncate text-center text-[clamp(0.75rem,1.8vw,0.95rem)] font-semibold leading-5 text-success">
            {player.balance != null ? `${player.balance} chips` : '\u00a0'}
        </p>
    )

    return (
        <div
            className={[
                'flex flex-col items-center gap-1',
                isSide ? 'w-[var(--side-col)] max-w-[var(--side-col)]' : '',
            ].join(' ')}
        >
            <p
                className={[
                    'player-name truncate text-center text-[clamp(0.8rem,2vw,1.05rem)] text-muted',
                    isSide ? 'w-full' : 'max-w-44',
                ].join(' ')}
            >
                {player.username}{' '}
                <span className="opacity-70">({count})</span>
            </p>

            {isSide ? (
                <div
                    className="relative shrink-0"
                    style={{
                        width: 'var(--opp-card-h)',
                        height: fullFanLength,
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
                <div
                    className="flex shrink-0 justify-center"
                    style={{ width: fullFanLength, minHeight: 'var(--opp-card-h)' }}
                >
                    {hand}
                </div>
            )}

            {chips}
        </div>
    )
}

export default OpponentSeat
