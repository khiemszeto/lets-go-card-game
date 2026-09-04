type Props = {
    compact?: boolean
}

function BackSideOfCard({ compact = false }: Props) {
    return (
        <div
            className={[
                'relative shrink-0 overflow-hidden rounded-lg border-2 border-amber-900/40 bg-[#1a3d2a] shadow',
                compact
                    ? 'h-[var(--opp-card-h,68px)] w-[var(--opp-card-w,2.75rem)]'
                    : 'h-[var(--card-h,92px)] w-[var(--card-w,4rem)]',
            ].join(' ')}
            aria-hidden
        >
            <span className="absolute inset-[8%] rounded-md border border-gold/50" />
            <span
                className="absolute inset-[12%] rounded-sm opacity-40"
                style={{
                    backgroundImage:
                        'repeating-linear-gradient(45deg, #d4a853 0 1px, transparent 1px 6px), repeating-linear-gradient(-45deg, #d4a853 0 1px, transparent 1px 6px)',
                }}
            />
        </div>
    )
}

export default BackSideOfCard
