# Whole feature review package
## Changed sources
### favicon.svg
-rw-r--r--@ 1 wangjf  staff  2583 Oct 30 00:31 /Users/wangjf/Project/Workspace/GitHub/feige/src/main/resources/static/favicon.svg
### favicon.png
-rw-r--r--@ 1 wangjf  staff  17192 Oct 30 00:31 /Users/wangjf/Project/Workspace/GitHub/feige/src/main/resources/static/favicon.png
### login.html
-rw-r--r--@ 1 wangjf  staff  1830 Oct 30 00:32 /Users/wangjf/Project/Workspace/GitHub/feige/src/main/resources/static/login.html
### register.html
-rw-r--r--@ 1 wangjf  staff  2770 Oct 30 00:32 /Users/wangjf/Project/Workspace/GitHub/feige/src/main/resources/static/register.html
### css/app.css
-rw-r--r--@ 1 wangjf  staff  23228 Oct 30 00:33 /Users/wangjf/Project/Workspace/GitHub/feige/src/main/resources/static/css/app.css

## login.html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>登录 · 飞鸽</title>
    <link rel="icon" href="/favicon.png" type="image/png">
    <script>
        (function () {
            var match = document.cookie.match(/(?:^|; )feige-theme=([^;]*)/);
            var theme = match ? decodeURIComponent(match[1]) : "classic";
            if (theme !== "classic" && theme !== "signal") theme = "classic";
            document.documentElement.setAttribute("data-theme", theme);
        })();
    </script>
    <link rel="stylesheet" href="/css/app.css">
</head>
<body class="gate">
<section class="gate-brand" aria-hidden="false">
    <img class="gate-hero-mark" src="/favicon.png" alt="">
    <p class="gate-brand-name">飞鸽</p>
    <p class="gate-tagline">局域网里的信使</p>
</section>
<main class="gate-panel">
    <div class="gate-panel-inner">
        <h1>登录</h1>
        <p class="lede">用已经注册的账号进入大厅，或点开某个人私聊。</p>
        <p id="error" class="error" hidden>用户名或密码错误</p>
        <form method="post" action="/login">
            <label>用户名<input name="username" autocomplete="username" required></label>
            <label>密码<input name="password" type="password" autocomplete="current-password" required></label>
            <input type="hidden" name="_csrf">
            <button type="submit">进入</button>
        </form>
        <p class="switch">还没有账号？<a href="/register">注册</a></p>
    </div>
</main>
<script src="/js/csrf.js"></script>
<script src="/js/theme.js"></script>
<script>
    if (location.search.indexOf("error") >= 0) {
        document.getElementById("error").hidden = false;
    }
</script>
</body>
</html>

## register.html head+body structure (first 80 lines)
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>注册 · 飞鸽</title>
    <link rel="icon" href="/favicon.png" type="image/png">
    <script>
        (function () {
            var match = document.cookie.match(/(?:^|; )feige-theme=([^;]*)/);
            var theme = match ? decodeURIComponent(match[1]) : "classic";
            if (theme !== "classic" && theme !== "signal") theme = "classic";
            document.documentElement.setAttribute("data-theme", theme);
        })();
    </script>
    <link rel="stylesheet" href="/css/app.css">
</head>
<body class="gate">
<section class="gate-brand" aria-hidden="false">
    <img class="gate-hero-mark" src="/favicon.png" alt="">
    <p class="gate-brand-name">飞鸽</p>
    <p class="gate-tagline">给自己取个好认的名字</p>
</section>
<main class="gate-panel">
    <div class="gate-panel-inner">
        <h1>注册</h1>
        <p class="lede">用户名 2 到 20 位，可以是汉字、字母或数字。密码至少 6 位。</p>
        <p id="error" class="error" hidden></p>
        <form id="form">
            <label>用户名<input name="username" autocomplete="username" required maxlength="20"></label>
            <label>密码<input name="password" type="password" autocomplete="new-password" required minlength="6" maxlength="64"></label>
            <button type="submit">注册</button>
        </form>
        <p class="switch">已有账号？<a href="/login">登录</a></p>
    </div>
</main>
<script src="/js/csrf.js"></script>
<script src="/js/theme.js"></script>
<script>
    var form = document.getElementById("form");
    var error = document.getElementById("error");
    form.addEventListener("submit", function (event) {
        event.preventDefault();
        error.hidden = true;
        var body = {
            username: form.username.value,
            password: form.password.value
        };
        fetch("/api/register", {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/json",
                "X-XSRF-TOKEN": readCsrf()
            },
            body: JSON.stringify(body)
        }).then(function (response) {
            if (response.status === 201) {
                location.href = "/login";
                return;
            }
            return response.json().then(function (payload) {
                error.textContent = payload.message || "注册失败";
                error.hidden = false;
            });
        }).catch(function () {
            error.textContent = "注册失败";
            error.hidden = false;
        });
    });
</script>
</body>
</html>

## gate CSS excerpt
.gate {
    min-height: 100vh;
    display: grid;
    grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
    background: var(--paper);
}
.gate-brand {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: flex-start;
    gap: 16px;
    padding: 48px 56px;
    background:
        radial-gradient(circle at 18% 20%, rgba(47, 122, 109, 0.28), transparent 42%),
        linear-gradient(145deg, #e7f1ee 0%, #d5e6e1 48%, #c5dcd5 100%);
    color: var(--ink);
}
.gate-hero-mark {
    width: 96px;
    height: 96px;
    border-radius: 22px;
    box-shadow: 0 18px 40px rgba(20, 32, 28, 0.16);
}
.gate-brand-name {
    margin: 0;
    font-family: "Iowan Old Style", Palatino, "Songti SC", serif;
    font-size: clamp(40px, 5vw, 56px);
    letter-spacing: 0.14em;
    color: var(--seal);
}
.gate-tagline {
    margin: 0;
    max-width: 16em;
    color: var(--muted);
    font-size: 17px;
}
.gate-panel {
    display: grid;
    place-items: center;
    padding: 32px 24px;
    background: var(--card);
    border-left: 1px solid var(--line);
}
.gate-panel-inner {
    width: min(380px, 100%);
}
.gate-panel h1 { margin-top: 0; }
.gate-panel button[type="submit"] {
    width: 100%;
    margin-top: 8px;
    min-height: 44px;
    border-radius: 10px;
}
.gate-panel input[type="text"],
.gate-panel input[type="password"],
.gate-panel input:not([type]) {
    border-radius: 10px;
}
@media (max-width: 899px) {
    .gate {
        grid-template-columns: 1fr;
        grid-template-rows: auto 1fr;
    }
    .gate-brand {
        padding: 28px 24px 24px;
        align-items: center;
        text-align: center;
    }
    .gate-hero-mark { width: 72px; height: 72px; border-radius: 18px; }
    .gate-brand-name { font-size: 32px; }
    .gate-panel { border-left: 0; border-top: 1px solid var(--line); }
}

.card {
    width: min(420px, calc(100% - 32px));
    background: var(--card);
    border: 1px solid var(--line);
    padding: 32px 28px;
    box-shadow: 0 18px 50px rgba(38, 34, 28, 0.08);
}

## signal gate excerpt
html[data-theme="signal"] .admin-bar,
html[data-theme="signal"] table {
    background: var(--card);
    border-radius: 2px;
}

html[data-theme="signal"] .gate-brand {
    background:
        linear-gradient(rgba(94, 234, 212, 0.05) 1px, transparent 1px),
        linear-gradient(90deg, rgba(94, 234, 212, 0.05) 1px, transparent 1px),
        radial-gradient(circle at 20% 15%, rgba(94, 234, 212, 0.18), transparent 40%),
        #05090f;
    background-size: 28px 28px, 28px 28px, auto, auto;
    color: var(--ink);
}
html[data-theme="signal"] .gate-panel {
    background: var(--card);
    border-left-color: var(--line);
}
html[data-theme="signal"] .gate-brand-name { color: var(--seal); }

html[data-theme="signal"] .log {
    background:
        radial-gradient(circle at 12% 0%, rgba(94, 234, 212, 0.08), transparent 28%),
        linear-gradient(180deg, rgba(18, 28, 25, 0.55), transparent 140px),
        var(--paper);

## progress ledger
Task 1: complete (no git, review clean — minor: stale target/ until package)
Task 2: complete (review clean)
Task 3: complete (review clean)
Task 4: complete (acceptance PASS)
