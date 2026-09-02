import { useEffect, useRef } from 'react'
import { orderedDeck } from '../game/thirteen'
import cardsCss from '@magmacrunch/adenosine-cards/cards.css?raw'
import './CardOrbit.css'

/** Travel speed along the border, in px per second. */
const SPEED = 16
/** Corner radius. A sharp corner pulls neighbouring cards together; see below. */
const RADIUS = 120
/** Smallest gap from the viewport edge; grows with the card if needed. */
const BASE_INSET = 30
/** The library renders every card at a fixed 100x140. */
const CARD_W = 100
const CARD_H = 140
const MIN_SCALE = 0.15
const MAX_SCALE = 0.95
/** Shaves the exact non-overlap bound so cards never touch outright. */
const SAFETY = 0.95
/** Points sampled around the loop when solving for that bound. */
const SAMPLES = 720

/**
 * Load the deck's stylesheet, confined to the orbit.
 *
 * It styles a bare `.card` where the class daisyUI uses for its own card component -
 * and ships unlayered, so importing it normally would size every daisyUI card on
 * the site to a 100x140 playing card. `@scope` keeps it off the rest of the page
 * without touching its selectors; the layer puts it under the unlayered rules in
 * CardOrbit.css, which have to win to place a card on the path.
 */
function loadCardStyles() {
    const id = 'orbit-card-styles'
    if (document.getElementById(id)) return
    const style = document.createElement('style')
    style.id = id
    style.textContent = `@layer deck { @scope (.orbit) { ${cardsCss} } }`
    document.head.appendChild(style)
}

type Segment =
    | { kind: 'line'; len: number; x0: number; y0: number; dx: number; dy: number }
    | { kind: 'arc'; len: number; cx: number; cy: number; r: number; a0: number }

type Path = { segments: Segment[]; starts: number[]; perimeter: number }

function buildPath(width: number, height: number, inset: number): Path {
    const across = Math.max(width - inset * 2, 0)
    const down = Math.max(height - inset * 2, 0)
    const r = Math.max(Math.min(RADIUS, across / 2, down / 2), 0)

    const left = inset
    const top = inset
    const right = inset + across
    const bottom = inset + down
    const runX = Math.max(across - r * 2, 0)
    const runY = Math.max(down - r * 2, 0)
    const quarter = (Math.PI / 2) * r
    const HALF_PI = Math.PI / 2

    const segments: Segment[] = [
        { kind: 'line', len: runX, x0: left + r, y0: top, dx: runX, dy: 0 },
        { kind: 'arc', len: quarter, cx: right - r, cy: top + r, r, a0: -HALF_PI },
        { kind: 'line', len: runY, x0: right, y0: top + r, dx: 0, dy: runY },
        { kind: 'arc', len: quarter, cx: right - r, cy: bottom - r, r, a0: 0 },
        { kind: 'line', len: runX, x0: right - r, y0: bottom, dx: -runX, dy: 0 },
        { kind: 'arc', len: quarter, cx: left + r, cy: bottom - r, r, a0: HALF_PI },
        { kind: 'line', len: runY, x0: left, y0: bottom - r, dx: 0, dy: -runY },
        { kind: 'arc', len: quarter, cx: left + r, cy: top + r, r, a0: Math.PI },
    ]

    const starts: number[] = []
    let perimeter = 0
    for (const seg of segments) {
        starts.push(perimeter)
        perimeter += seg.len
    }
    return { segments, starts, perimeter }
}

function pointOn(path: Path, distance: number): [number, number] {
    const { segments, starts, perimeter } = path
    if (perimeter <= 0) return [0, 0]
    const d = ((distance % perimeter) + perimeter) % perimeter

    let i = segments.length - 1
    while (i > 0 && d < starts[i]) i--

    const seg = segments[i]
    const local = d - starts[i]

    if (seg.kind === 'line') {
        const t = seg.len > 0 ? local / seg.len : 0
        return [seg.x0 + seg.dx * t, seg.y0 + seg.dy * t]
    }
    const angle = seg.a0 + (seg.r > 0 ? local / seg.r : 0)
    return [seg.cx + seg.r * Math.cos(angle), seg.cy + seg.r * Math.sin(angle)]
}

/**
 * Largest scale at which no two neighbours overlap anywhere on the loop.
 *
 * Upright rectangles miss each other when they are a full card apart on either
 * axis, so a pair separated by (dx, dy) tolerates a scale up to
 * max(dx/100, dy/140). The orbit is limited by its tightest pair, which always
 * falls on a corner arc.
 */
function solveScale(path: Path, count: number): number {
    if (path.perimeter <= 0 || count <= 0) return MIN_SCALE
    const step = path.perimeter / count
    let limit = Infinity
    for (let i = 0; i < SAMPLES; i++) {
        const d = (i / SAMPLES) * path.perimeter
        const [ax, ay] = pointOn(path, d)
        const [bx, by] = pointOn(path, d + step)
        const pair = Math.max(Math.abs(ax - bx) / CARD_W, Math.abs(ay - by) / CARD_H)
        if (pair < limit) limit = pair
    }
    return Math.min(MAX_SCALE, Math.max(MIN_SCALE, limit * SAFETY))
}

/**
 * All 52 cards, in Thirteen order, orbiting the page clockwise: across the top,
 * down the right edge, back along the bottom, up the left. Cards stay upright.
 *
 * The path is a rounded rectangle rather than a sharp one. On a square corner
 * two cards an equal path-distance apart sit only 1/sqrt(2) of that distance
 * apart as the crow flies, so they collide however wide the spacing. Rounding
 * the turn spreads it over an arc.
 *
 * Positions are computed per frame; a keyframe track cannot turn a corner.
 */
function CardOrbit() {
    const layerRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        const layer = layerRef.current
        if (!layer) return

        loadCardStyles()

        const cards = orderedDeck().map((card) => {
            const el = card.getHTML()
            el.classList.add('orbit-card')
            layer.appendChild(el)
            return el
        })

        let path = buildPath(0, 0, BASE_INSET)
        let scale = MIN_SCALE

        function measure() {
            // Orbit between the header and the footer, so the bottom run does not
            // pass behind the footer text.
            const header = document.querySelector('header')
            const footer = document.querySelector('footer')
            const headerHeight = header ? header.getBoundingClientRect().height : 0
            const footerHeight = footer ? footer.getBoundingClientRect().height : 0
            layer!.style.top = `${headerHeight}px`
            layer!.style.bottom = `${footerHeight}px`

            // Read the viewport, not the layer. A hidden layer measures 0x0, and
            // deriving the path from that would leave the orbit stuck hidden even
            // after the window grew back.
            const width = window.innerWidth
            const height = Math.max(window.innerHeight - headerHeight - footerHeight, 0)

            // The inset has to clear half a card, but the card's size depends on
            // the path the inset defines. A few passes settle it.
            let inset = BASE_INSET
            for (let pass = 0; pass < 3; pass++) {
                path = buildPath(width, height, inset)
                scale = solveScale(path, cards.length)
                inset = Math.max(BASE_INSET, (CARD_H * scale) / 2 + 6)
            }
            path = buildPath(width, height, inset)
            scale = solveScale(path, cards.length)
        }

        function layout(offset: number) {
            const step = path.perimeter / cards.length
            // Subtracting the index puts 3 of spades at the front of the queue, so
            // the deck streams past in ascending Thirteen order and 2 of hearts
            // brings up the rear.
            cards.forEach((el, i) => {
                const [x, y] = pointOn(path, offset - i * step)
                el.style.transform = `translate(${x}px, ${y}px) scale(${scale})`
            })
        }

        measure()

        const still = window.matchMedia('(prefers-reduced-motion: reduce)')
        let frame = 0

        function start() {
            if (still.matches) {
                layout(0)
                return
            }
            const t0 = performance.now()
            const tick = (now: number) => {
                layout(((now - t0) / 1000) * SPEED)
                frame = requestAnimationFrame(tick)
            }
            frame = requestAnimationFrame(tick)
        }

        function restart() {
            cancelAnimationFrame(frame)
            measure()
            start()
        }

        start()

        function remeasure() {
            measure()
            if (still.matches) layout(0)
        }

        // Viewport changes drive the perimeter; the header and footer drive the
        // top and bottom edges. body's height is content-driven and does not
        // track the viewport, so a ResizeObserver on it alone would miss a
        // vertical resize.
        window.addEventListener('resize', remeasure)
        const observer = new ResizeObserver(remeasure)
        for (const el of [document.querySelector('header'), document.querySelector('footer')]) {
            if (el) observer.observe(el)
        }

        still.addEventListener('change', restart)

        return () => {
            cancelAnimationFrame(frame)
            window.removeEventListener('resize', remeasure)
            observer.disconnect()
            still.removeEventListener('change', restart)
            layer.replaceChildren()
        }
    }, [])

    return <div className="orbit" aria-hidden="true" ref={layerRef} />
}

export default CardOrbit
