import { useEffect, type CSSProperties, type ReactNode } from 'react'
import type { Card } from '../types/game'
import PlayingCard from './PlayingCard'

type Props = {
    open: boolean
    onClose: () => void
}

function c(rank: Card['rank'], suit: Card['suit']): Card {
    return { rank, suit }
}

function MiniHand({
    cards,
    nowrap = false,
    separator,
}: {
    cards: Card[]
    nowrap?: boolean
    separator?: string
}) {
    return (
        <div
            className={['flex items-end gap-1', nowrap ? 'flex-nowrap' : 'flex-wrap'].join(' ')}
            style={
                {
                    '--card-w': '3.29rem',
                    '--card-h': 'calc(3.29rem * 1.44)',
                } as CSSProperties
            }
        >
            {cards.map((card, i) => (
                <div key={`${card.rank}-${card.suit}`} className="flex items-center gap-1">
                    {separator && i > 0 && (
                        <span className="shrink-0 px-0.5 text-[0.65rem] font-bold uppercase text-base-content/50">
                            {separator}
                        </span>
                    )}
                    <PlayingCard card={card} disabled />
                </div>
            ))}
        </div>
    )
}

function BeatsRow({
    caption,
    weaker,
    stronger,
    weakerSeparator,
    weakerGroups,
}: {
    caption: ReactNode
    weaker?: Card[]
    stronger: Card[]
    weakerSeparator?: string
    /** Alternative hands shown with OR between them (e.g. single 2 OR 3 consecutive pairs). */
    weakerGroups?: Card[][]
}) {
    return (
        <div className="rounded-lg border border-white/10 bg-base-200/40 p-3 text-center">
            <p className="mb-2 text-xs font-medium text-base-content/70">{caption}</p>
            <div className="flex flex-wrap items-center justify-center gap-2 sm:gap-3">
                <MiniHand cards={stronger} />
                <span
                    className="shrink-0 px-1 text-base font-black uppercase tracking-[0.2em] text-amber-400 drop-shadow-[0_1px_1px_rgba(0,0,0,0.45)]"
                    aria-hidden
                >
                    beat
                </span>
                {weakerGroups ? (
                    <div className="flex flex-wrap items-center justify-center gap-1">
                        {weakerGroups.map((group, i) => (
                            <div key={i} className="flex items-center gap-1">
                                {i > 0 && (
                                    <span className="shrink-0 px-0.5 text-[0.65rem] font-bold uppercase text-base-content/50">
                                        OR
                                    </span>
                                )}
                                <MiniHand cards={group} />
                            </div>
                        ))}
                    </div>
                ) : (
                    <MiniHand cards={weaker ?? []} separator={weakerSeparator} />
                )}
            </div>
        </div>
    )
}

function RulesModal({ open, onClose }: Props) {
    useEffect(() => {
        if (!open) return
        function onKey(e: KeyboardEvent) {
            if (e.key === 'Escape') onClose()
        }
        window.addEventListener('keydown', onKey)
        return () => window.removeEventListener('keydown', onKey)
    }, [open, onClose])

    if (!open) return null

    return (
        <div className="modal modal-open" role="dialog" aria-modal="true" aria-labelledby="rules-title">
            <div className="modal-box w-11/12 max-h-[85dvh] max-w-5xl overflow-y-auto">
                <div className="mb-3 flex items-start justify-between gap-3">
                    <h2 id="rules-title" className="text-lg font-bold">
                        Thirteen Cards rules
                    </h2>
                    <button type="button" className="btn btn-ghost btn-sm btn-circle" onClick={onClose} aria-label="Close">
                        ✕
                    </button>
                </div>

                <div className="space-y-5 text-sm leading-relaxed">
                    <section>
                        <h3 className="mb-1 font-semibold text-primary">1. Card strength</h3>
                        <p className="mb-2 text-base-content/80">Rank (weak → strong). <strong>2</strong> is strongest:</p>
                        <MiniHand
                            nowrap
                            cards={[
                                c('THREE', 'SPADES'),
                                c('FOUR', 'SPADES'),
                                c('FIVE', 'SPADES'),
                                c('SIX', 'SPADES'),
                                c('SEVEN', 'SPADES'),
                                c('EIGHT', 'SPADES'),
                                c('NINE', 'SPADES'),
                                c('TEN', 'SPADES'),
                                c('JACK', 'SPADES'),
                                c('QUEEN', 'SPADES'),
                                c('KING', 'SPADES'),
                                c('ACE', 'SPADES'),
                                c('TWO', 'SPADES'),
                            ]}
                        />
                        <p className="mt-3 mb-2 text-base-content/80">
                            Suit (weak → strong), used when ranks tie:
                        </p>
                        <MiniHand
                            cards={[
                                c('ACE', 'SPADES'),
                                c('ACE', 'CLUBS'),
                                c('ACE', 'DIAMONDS'),
                                c('ACE', 'HEARTS'),
                            ]}
                        />
                    </section>

                    <section className="space-y-3">
                        <h3 className="font-semibold text-primary">2. Same-type plays</h3>
                        <p className="text-base-content/80">
                            Same combo type, same number of cards, strictly stronger high card.
                        </p>

                        <div className="flex flex-col gap-3 sm:flex-row sm:items-stretch">
                            <div className="min-w-0 flex-1">
                                <BeatsRow
                                    caption="Single — higher rank wins"
                                    weaker={[c('THREE', 'HEARTS')]}
                                    stronger={[c('FOUR', 'SPADES')]}
                                />
                            </div>
                            <div className="min-w-0 flex-1">
                                <BeatsRow
                                    caption="Single — same rank, higher suit wins"
                                    weaker={[c('ACE', 'DIAMONDS')]}
                                    stronger={[c('ACE', 'HEARTS')]}
                                />
                            </div>
                        </div>
                        <div className="flex flex-col gap-3 sm:flex-row sm:items-stretch">
                            <div className="min-w-0 flex-1">
                                <BeatsRow
                                    caption="Pair — higher pair wins"
                                    weaker={[c('EIGHT', 'DIAMONDS'), c('EIGHT', 'CLUBS')]}
                                    stronger={[c('NINE', 'SPADES'), c('NINE', 'HEARTS')]}
                                />
                            </div>
                            <div className="min-w-0 flex-1">
                                <BeatsRow
                                    caption="Triple — higher triple wins"
                                    weaker={[
                                        c('SEVEN', 'SPADES'),
                                        c('SEVEN', 'CLUBS'),
                                        c('SEVEN', 'DIAMONDS'),
                                    ]}
                                    stronger={[
                                        c('JACK', 'SPADES'),
                                        c('JACK', 'CLUBS'),
                                        c('JACK', 'HEARTS'),
                                    ]}
                                />
                            </div>
                        </div>
                        <BeatsRow
                            caption={
                                <>
                                    Straight — <strong>SAME LENGTH ONLY</strong>; higher top card wins
                                </>
                            }
                            weaker={[
                                c('THREE', 'SPADES'),
                                c('FOUR', 'CLUBS'),
                                c('FIVE', 'DIAMONDS'),
                                c('SIX', 'HEARTS'),
                                c('SEVEN', 'SPADES'),
                            ]}
                            stronger={[
                                c('FOUR', 'HEARTS'),
                                c('FIVE', 'SPADES'),
                                c('SIX', 'CLUBS'),
                                c('SEVEN', 'DIAMONDS'),
                                c('EIGHT', 'HEARTS'),
                            ]}
                        />
                        <p className="text-xs text-base-content/60">
                            A longer straight does not beat a shorter one. Straights and consecutive pairs cannot include 2s.
                        </p>
                    </section>

                    <section className="space-y-3">
                        <h3 className="font-semibold text-primary">3. Bombs</h3>
                        <p className="text-base-content/80">
                            These can beat across types — they chop 2s and some other bombs:
                        </p>

                        <BeatsRow
                            caption="3 consecutive pairs chop a single 2 (any suit)"
                            weakerSeparator="OR"
                            weaker={[
                                c('TWO', 'SPADES'),
                                c('TWO', 'CLUBS'),
                                c('TWO', 'DIAMONDS'),
                                c('TWO', 'HEARTS'),
                            ]}
                            stronger={[
                                c('FOUR', 'SPADES'),
                                c('FOUR', 'HEARTS'),
                                c('FIVE', 'CLUBS'),
                                c('FIVE', 'DIAMONDS'),
                                c('SIX', 'SPADES'),
                                c('SIX', 'HEARTS'),
                            ]}
                        />
                        <BeatsRow
                            caption="Quad chops a single 2 OR 3 consecutive pairs"
                            stronger={[
                                c('NINE', 'SPADES'),
                                c('NINE', 'CLUBS'),
                                c('NINE', 'DIAMONDS'),
                                c('NINE', 'HEARTS'),
                            ]}
                            weakerGroups={[
                                [c('TWO', 'HEARTS')],
                                [
                                    c('FOUR', 'SPADES'),
                                    c('FOUR', 'HEARTS'),
                                    c('FIVE', 'CLUBS'),
                                    c('FIVE', 'DIAMONDS'),
                                    c('SIX', 'SPADES'),
                                    c('SIX', 'HEARTS'),
                                ],
                            ]}
                        />
                        <BeatsRow
                            caption="4 consecutive pairs chop 3 consecutive pairs, any quad, or a triple of 2s"
                            stronger={[
                                c('THREE', 'SPADES'),
                                c('THREE', 'HEARTS'),
                                c('FOUR', 'CLUBS'),
                                c('FOUR', 'DIAMONDS'),
                                c('FIVE', 'SPADES'),
                                c('FIVE', 'HEARTS'),
                                c('SIX', 'CLUBS'),
                                c('SIX', 'DIAMONDS'),
                            ]}
                            weakerGroups={[
                                [
                                    c('FOUR', 'SPADES'),
                                    c('FOUR', 'HEARTS'),
                                    c('FIVE', 'CLUBS'),
                                    c('FIVE', 'DIAMONDS'),
                                    c('SIX', 'SPADES'),
                                    c('SIX', 'HEARTS'),
                                ],
                                [
                                    c('NINE', 'SPADES'),
                                    c('NINE', 'CLUBS'),
                                    c('NINE', 'DIAMONDS'),
                                    c('NINE', 'HEARTS'),
                                ],
                                [
                                    c('TWO', 'SPADES'),
                                    c('TWO', 'CLUBS'),
                                    c('TWO', 'HEARTS'),
                                ],
                            ]}
                        />
                    </section>
                </div>

                <div className="modal-action">
                    <button type="button" className="btn btn-primary" onClick={onClose}>
                        Got it
                    </button>
                </div>
            </div>
            <button type="button" className="modal-backdrop bg-black/50" aria-label="Close rules" onClick={onClose} />
        </div>
    )
}

export default RulesModal
