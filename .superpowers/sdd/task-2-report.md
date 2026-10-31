# Task 2 Report: Login/register HTML split structure

## Status

**Complete** — no git commit per instructions.

## Summary

Restructured `login.html` and `register.html` from single `.card` layout to `.gate > .gate-brand + .gate-panel` with `.gate-panel-inner` on the form side. Head (theme cookie inline script, favicon, `app.css`) unchanged.

## Files changed

| File | Change |
|------|--------|
| `src/main/resources/static/login.html` | Body: brand column (favicon, 飞鸽, 局域网里的信使) + panel (h1 登录, form `POST /login`) |
| `src/main/resources/static/register.html` | Body: brand column (tagline 给自己取个好认的名字) + panel (h1 注册, `#form`, fetch register) |

## Static verification

### login.html

- [x] `body.gate` with `section.gate-brand` + `main.gate-panel` + `.gate-panel-inner`
- [x] `img.gate-hero-mark` → `/favicon.png`
- [x] Form: `method="post"` `action="/login"`
- [x] Fields: `name="username"`, `name="password"`, hidden `name="_csrf"`
- [x] `#error` + query `error` script
- [x] `/js/csrf.js`, `/js/theme.js`
- [x] Head theme cookie script (`feige-theme`)

### register.html

- [x] Same gate DOM structure
- [x] Brand tagline: 给自己取个好认的名字
- [x] `h1`: 注册; lede unchanged
- [x] `form#form` with `username` / `password` fields (unchanged attributes)
- [x] Inline script: `fetch("/api/register", …)` with `readCsrf()` / `X-XSRF-TOKEN`
- [x] `/js/csrf.js`, `/js/theme.js`
- [x] Head theme cookie script

## Not changed

- `src/main/resources/static/css/app.css` (Task 3)
- No Thymeleaf templates (static HTML only per brief paths)

## Concerns

- **Visual layout** until Task 3 adds `.gate-brand` / `.gate-panel` CSS — pages may look unstyled for the split until then.
- **Login h1** changed from「回到局域网」to「登录」per brief; confirm product copy is intentional.

## Commits

None.
