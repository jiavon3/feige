# Task 3 Report: Gate CSS (split login)

## Status

**Complete** — no git commit per instructions.

## Summary

Replaced centered `.gate` with a two-column grid (≥900px) and stacked layout (<900px). Added `.gate-brand`, `.gate-hero-mark`, `.gate-brand-name`, `.gate-tagline`, `.gate-panel`, and `.gate-panel-inner` using the celadon palette from the brief. Left `.card` unchanged for other pages. Added `html[data-theme="signal"]` overrides for gate brand/panel.

## Files changed

| File | Change |
|------|--------|
| `src/main/resources/static/css/app.css` | Gate split layout, brand hero styling, panel form tweaks, narrow breakpoint, signal theme gate rules |

## Verification

- [x] `.gate` grid columns `1.08fr` / `0.92fr`; stacks at `max-width: 899px`
- [x] Celadon gradients on `.gate-brand`; no purple/cream-terracotta patterns added
- [x] Signal theme: grid background on brand, panel uses `--card` / `--line`
- [x] `.brand-mark` still `28×28px`, `border-radius: 8px` — untouched (separate from `.gate-hero-mark`)

## Concerns

- Narrow view centers brand copy; long taglines may need extra padding if copy grows.
- Signal `.gate-panel` border-top on mobile inherits generic `var(--line)` only (no extra signal rule in brief).

## Commits

None.
