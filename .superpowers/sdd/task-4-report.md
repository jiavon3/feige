# Task 4 Report: Package and browser acceptance (login split)

## Status

**PASS** — packaged, restarted, acceptance checks satisfied. No git commit (per constraints).

## Step 1: Package and restart

| Check | Result |
|-------|--------|
| `pkill -f target/feige.jar` | Done |
| `mvn -gs /tmp/feige-maven-settings.xml -s /tmp/feige-maven-settings.xml -q -DskipTests package` | Exit 0 (~7.6s) |
| `java -jar target/feige.jar` (JDK 1.8.0_311) | Running (PID 86837) |
| Log: `Tomcat started on port(s): 8080` | Yes |
| Log: `Started FeigeApplication` | Yes (~11.9s) |

Artifact: `target/feige.jar` (~43 MB).

## Step 2: Acceptance checklist

### Automated (curl / jar)

| # | Check | Result |
|---|--------|--------|
| JAR contains `login.html`, `register.html` | Yes |
| JAR contains `favicon.png`, `favicon.svg` | Yes |
| `GET /login` HTML includes `gate-brand`, `gate-panel`, `gate-hero-mark`, `/favicon.png` | Yes |
| `GET /register` includes `gate-brand`, `gate-panel`; `form#form` + `fetch("/api/register"` | Yes |
| `GET /favicon.png`, `/favicon.svg` | 200 |
| `GET /login`, `/register` | 200 |
| `GET /`, `/chat` (no session) | 302 |
| Packaged `chat.html` has `brand-mark` → `/favicon.png` | Yes |
| Packaged `app.css` includes `html[data-theme="signal"] .gate-brand` rules | Yes (2 selectors) |

### Browser (127.0.0.1:8080)

| # | Brief item | Result |
|---|------------|--------|
| 1 | `/login` hard load: left brand, right form; new celadon pigeon icon | **Pass** — two-column split; hero mark matches Task 1 icon |
| 2 | `/register` same structure; registration still wired | **Pass** — split layout; form + fetch script present (not exercised end-to-end register) |
| 3 | Narrow viewport (&lt;900px): stacked layout | **Pass** — 480×800: brand block above form (classic and signal) |
| 4 | Admin → 信标 → gate (login) left dark atmosphere | **Pass** — admin switched theme; after logout, login shows `data-theme=signal`, `.gate-brand` bg `rgb(5, 9, 15)` with grid/glow |
| 5 | Chat sidebar brand uses new icon | **Pass** — logged-in chat shows teal pigeon mark in sidebar lockup (signal theme active) |

## HTTP summary (unauthenticated)

| Path | Status |
|------|--------|
| `/login` | 200 |
| `/register` | 200 |
| `/favicon.png` | 200 |
| `/` | 302 |
| `/chat` | 302 |

## Concerns

- **Register E2E:** Only static/HTML + script presence verified; no new user registration POST in this task.
- **Signal theme on gate:** Requires `/api/theme` (via `theme.js`) or admin theme switch; inline head script alone reads cookie only on first paint — `theme.js` corrects after fetch (observed signal after admin switch + logout).
- **Shell PATH:** Some agent shells lacked `curl` on default PATH; checks used `/usr/bin/curl` explicitly.

## Verification commands (reference)

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_311.jdk/Contents/Home
/usr/bin/jar tf target/feige.jar | grep favicon
/usr/bin/curl -s http://127.0.0.1:8080/login | grep gate-brand
```

## Commits

None.
