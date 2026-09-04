import type { RefObject } from 'react'
import type { Card, RoomPlayer } from '../types/game'
import type { GameSocket } from '../websocket/GameSocket'
import PlayingCard from '../components/PlayingCard'
import { sortHand, sameCard } from '../utils/cardUtils'
import { getUsername } from '../auth/authStorage'
import OpponentSeat from '../components/OpponentSeat'

export type GameState = {
    roomId: string
    players: RoomPlayer[]
    currentPlayerId: number
    myHand: Card[]
    selected: Card[]
    lastPlay: { playerId: number; cards: Card[] } | null
    winnerId: number | null
    endSeconds: number | null
}

type Props = {
    gamestate: GameState
    socketRef: RefObject<GameSocket | null>
    onChangeGame: (gamestate: GameState) => void
}

function LastPlayCards({ lastPlay }: { lastPlay: GameState['lastPlay'] }) {
    if (!lastPlay) {
        return <p className="text-sm text-muted">No cards played yet</p>
    }

    return sortHand(lastPlay.cards).map((c) => (
        <PlayingCard key={`${c.suit}-${c.rank}`} card={c} disabled />
    ))
}

function GamePage({ gamestate, socketRef, onChangeGame }: Props) {
    const myHand = sortHand(gamestate.myHand)
    const myUsername = getUsername()
    const me = gamestate.players.find((p) => p.username === myUsername)
    const isMyTurn = me != null && me.playerId === gamestate.currentPlayerId
    const gameEnded = gamestate.winnerId != null

    function relativeSeat(player: RoomPlayer) {
        if (!me) return 0
        return (player.seat - me.seat + 4) % 4
    }

    const leftOpponent = gamestate.players.find((p) => relativeSeat(p) === 1) ?? null
    const rightOpponent = gamestate.players.find((p) => relativeSeat(p) === 3) ?? null
    const oppositeOpponent = gamestate.players.find((p) => relativeSeat(p) === 2) ?? null

    function toggleCard(card: Card) {
        const isCardSelected = gamestate.selected.some((c) => sameCard(c, card))

        const selected = isCardSelected
            ? gamestate.selected.filter((c) => !sameCard(c, card))
            : [...gamestate.selected, card]

        onChangeGame({ ...gamestate, selected })
    }

    function handlePlay() {
        if (!isMyTurn || gamestate.selected.length === 0) return
        socketRef.current?.play(gamestate.selected)
    }

    function handlePass() {
        if (!isMyTurn) return
        socketRef.current?.pass()
    }

    function handleLeaveDuringGame() {
        socketRef.current?.leaveDuringGame()
    }

    return (
        <div className="game-table relative mx-auto w-full max-w-[1600px] items-center gap-2 px-2 py-1 lg:gap-4">
            {gamestate.winnerId != null && (
                <div className="absolute inset-0 z-30 grid place-items-center bg-black/60">
                    <div className="rounded-2xl bg-base-200 px-8 py-6 text-center shadow-xl">
                        <p className="text-xs uppercase tracking-widest text-gold">Game over</p>
                        <h2 className="mt-2 text-2xl font-bold">
                            {gamestate.players.find((p) => p.playerId === gamestate.winnerId)?.username
                                ?? `Player ${gamestate.winnerId}`}{' '}
                            wins!
                        </h2>
                        <p className="mt-3 text-sm text-muted">
                            Returning to table in {gamestate.endSeconds ?? 0}s…
                        </p>
                    </div>
                </div>
            )}


            <button
                type="button"
                className="btn btn-error btn-sm absolute right-2 top-1 z-20"
                onClick={handleLeaveDuringGame}
            >
                Leave
            </button>

            <p className="shrink-0 text-xs uppercase tracking-widest text-gold">
                Table {gamestate.roomId}
            </p>

            <div className="grid min-h-0 w-full flex-1 grid-cols-[var(--side-col)_minmax(0,1fr)_var(--side-col)] grid-rows-[auto_minmax(0,1fr)] items-center gap-x-1 gap-y-2 lg:gap-x-4 lg:gap-y-4">
                <div className="col-start-2 row-start-1 justify-self-center">
                    <OpponentSeat player={oppositeOpponent} side="top" />
                </div>

                <div className="col-start-1 row-start-2 flex items-center justify-center self-stretch overflow-visible">
                    <OpponentSeat player={leftOpponent} side="left" />
                </div>

                <div className="play-slot col-start-2 row-start-2 min-h-0 min-w-0 self-stretch">
                    <div className="play-area flex items-center justify-center gap-[clamp(0.25rem,0.8vw,0.5rem)] rounded-2xl bg-base-200 shadow-inner">
                        <LastPlayCards lastPlay={gamestate.lastPlay} />
                    </div>
                </div>

                <div className="col-start-3 row-start-2 flex items-center justify-center self-stretch overflow-visible">
                    <OpponentSeat player={rightOpponent} side="right" />
                </div>
            </div>

            <div className="flex w-full shrink-0 flex-col items-center gap-1">
                <div className="flex items-center justify-center gap-3">

                    <div className="flex flex-wrap justify-center gap-[clamp(0.25rem,0.7vw,0.7rem)]"
                         style={{
                             width:
                                 'calc(13 * var(--card-w) + 12 * clamp(0.25rem, 0.7vw, 0.7rem))',
                             minWidth:
                                 'calc(13 * var(--card-w) + 12 * clamp(0.25rem, 0.7vw, 0.7rem))',
                         }}>
                        {myHand.map((card) => (
                            <PlayingCard
                                key={`${card.suit}-${card.rank}`}
                                card={card}
                                isSelected={gamestate.selected.some((c) => sameCard(c, card))}
                                onClick={() => toggleCard(card)}
                            />
                        ))}
                    </div>

                    <div className="flex shrink-0 flex-col gap-2">
                        <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            disabled={!isMyTurn || gamestate.selected.length === 0 || gameEnded}
                            onClick={handlePlay}
                        >
                            Play
                        </button>
                        <button
                            type="button"
                            className="btn btn-outline btn-sm"
                            disabled={!isMyTurn || gameEnded}
                            onClick={handlePass}
                        >
                            Pass
                        </button>
                    </div>
                </div>

                <p className="text-center text-[length:clamp(0.8rem,2vw,1.125rem)] font-bold">
                    {myUsername}
                    {isMyTurn && (
                        <span className="ml-2 text-xs font-normal text-gold">Your turn</span>
                    )}
                </p>
                {gamestate.selected.length > 0 && (
                    <p className="text-center text-sm text-muted">
                        Selecting: {gamestate.selected.length} cards
                    </p>
                )}
            </div>
        </div>
    )
}

export default GamePage
