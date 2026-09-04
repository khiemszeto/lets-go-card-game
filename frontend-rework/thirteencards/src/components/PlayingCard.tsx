import type { Card } from '../types/game'
import { rankLabel, suitSymbol, isRedSuit } from '../utils/cardUtils'

type Props = {
    card: Card
    isSelected?: boolean
    onClick?: () => void
    disabled?: boolean
}

function PlayingCard({ card, isSelected, onClick, disabled }: Props) {
    const red = isRedSuit(card.suit)
    const symbol = suitSymbol(card.suit)
    const label = rankLabel(card.rank)

    return (
        <button
            type="button"
            disabled={disabled}
            onClick={onClick}
            className={[
                'relative shrink-0 rounded-lg border-2 bg-white shadow',
                'h-[var(--card-h,92px)] w-[var(--card-w,4rem)]',
                'transition-transform hover:-translate-y-[8%]',
                red ? 'text-red-600' : 'text-neutral-900',
                isSelected ? '-translate-y-[14%] border-amber-400 shadow-lg' : 'border-black/10',
                disabled ? 'cursor-default opacity-80' : 'cursor-pointer',
            ].join(' ')}
        >
            <span className="absolute left-[8%] top-[6%] text-center font-bold leading-none [font-size:max(0.45rem,calc(var(--card-w,4rem)*0.22))]">
                <span className="block">{label}</span>
                <span className="block">{symbol}</span>
            </span>
            <span className="grid h-full place-items-center [font-size:max(0.7rem,calc(var(--card-w,4rem)*0.38))]">
                {symbol}
            </span>
            <span className="absolute right-[8%] bottom-[6%] rotate-180 text-center font-bold leading-none [font-size:max(0.45rem,calc(var(--card-w,4rem)*0.22))]">
                <span className="block">{label}</span>
                <span className="block">{symbol}</span>
            </span>
        </button>
    )
}

export default PlayingCard
