# Task 2 review package
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

## register.html
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
