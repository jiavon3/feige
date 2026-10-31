### Task 2: 登录/注册 HTML 分栏结构

**Files:**
- Modify: `src/main/resources/static/login.html`
- Modify: `src/main/resources/static/register.html`

**Interfaces:**
- Consumes: `/favicon.png`
- Produces: DOM 结构 `.gate > .gate-brand + .gate-panel`；表单 `action`/`id` 与现脚本一致。

- [ ] **Step 1: 改写 login.html body**

```html
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
<!-- 保留现有 csrf.js / theme.js / error query 脚本 -->
</body>
```

- [ ] **Step 2: 改写 register.html body**

同样分栏；左侧 tagline「给自己取个好认的名字」；右侧 `h1` 为「注册」，说明文案与 `#form` 提交脚本保持不变。

- [ ] **Step 3: 静态检查**

确认 `name="username"` / `password`、`/api/register` 脚本、CSRF hidden、主题 cookie 脚本均在。

---
