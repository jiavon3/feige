# 飞鸽登录分栏海报 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把登录/注册门页改成左右分栏海报布局，并升级青绿鸽衔信品牌图标。

**Architecture:** 门页 HTML 拆成 `.gate-brand` + `.gate-panel` 两栏；CSS 在 `app.css` 用 grid + 媒体查询适配窄屏；图标以新 `favicon.svg` 为准，用 `rsvg-convert`/`qlmanage`/Python PIL 或内联 SVG 导出圆角 `favicon.png`；信笺/信标靠现有 `data-theme` 覆盖左侧氛围与右侧面板。

**Tech Stack:** 静态 HTML/CSS、现有 Spring Boot 静态资源、Java 8 打包验证。

## Global Constraints

- Java 8 / Spring Boot 2.7 fat jar；静态资源路径不变（`/login`、`/register`、`/favicon.png`）。
- 不改登录/注册 API、CSRF、字段名。
- 不主动 git commit（除非用户明确要求）。
- 避免紫渐变、奶油陶土风、报纸密排；延续 celadon `#2f7a6d` 体系。
- Spec：`docs/superpowers/specs/2026-10-30-feige-login-split-design.md`

## File map

| File | Role |
|------|------|
| `src/main/resources/static/favicon.svg` | 新图标源 |
| `src/main/resources/static/favicon.png` | favicon + brand-mark 位图 |
| `src/main/resources/static/login.html` | 分栏登录结构 |
| `src/main/resources/static/register.html` | 分栏注册结构 |
| `src/main/resources/static/css/app.css` | `.gate*` 样式与 signal 覆盖 |

---

### Task 1: 品牌图标

**Files:**
- Modify: `src/main/resources/static/favicon.svg`
- Modify: `src/main/resources/static/favicon.png`

**Interfaces:**
- Produces: `/favicon.png`、`/favicon.svg` 青绿圆角鸽衔信；聊天侧栏与门页共用 PNG。

- [ ] **Step 1: 重写 favicon.svg**

用青绿底（约 `#2f7a6d`→`#1f5c52` 渐变）、白鸽、信封、轻高光，圆角矩形 `rx≈14`，viewBox `0 0 64 64`。去掉旧蓝色 `#4b86f5` 稿。

- [ ] **Step 2: 导出 favicon.png**

从 SVG 渲染 64×64（或 128×128）PNG，覆盖 `src/main/resources/static/favicon.png`。可用：

```bash
# 任选可用工具；若无 rsvg，用 Python/cairo 或 macOS qlmanage 后裁切
```

若环境无矢量栅格工具：用 Cursor `GenerateImage` 按 SVG 描述生成方形图标后写入同路径（圆角青绿底白鸽衔信）。

- [ ] **Step 3: 目视确认**

打开 PNG/SVG，确认无蓝底、有信封、圆角清晰。

---

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

### Task 3: 门页 CSS（含信标与窄屏）

**Files:**
- Modify: `src/main/resources/static/css/app.css`

**Interfaces:**
- Consumes: `.gate`、`.gate-brand`、`.gate-panel`、`.gate-panel-inner`、`.gate-hero-mark`、`.gate-brand-name`、`.gate-tagline`
- Produces: ≥900px 左右分栏；&lt;900px 上下；signal 主题覆盖。

- [ ] **Step 1: 替换/扩展 `.gate` / `.card` 门页样式**

保留 `.card` 供其他页若仍引用；门页改用：

```css
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
```

- [ ] **Step 2: signal 主题覆盖**

```css
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
```

- [ ] **Step 3: 确认 `.brand-mark` 聊天侧栏仍正常**（尺寸样式不因门页改坏）。

---

### Task 4: 打包与浏览器验收

**Files:** 无新文件；运行产物 `target/feige.jar`

- [ ] **Step 1: 打包并重启**

```bash
pkill -f 'target/feige.jar' 2>/dev/null || true
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_311.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:/Users/wangjf/Other/maven/apache-maven-3.6.3/bin:$PATH"
cd /Users/wangjf/Project/Workspace/GitHub/feige
mvn -gs /tmp/feige-maven-settings.xml -s /tmp/feige-maven-settings.xml -q -DskipTests package
java -jar target/feige.jar
```

Expected: 日志含 `Started FeigeApplication`、`Tomcat started on port(s): 8080`

- [ ] **Step 2: 验收清单**

1. 硬刷新 `http://127.0.0.1:8080/login`：左品牌右表单；新图标。
2. `/register` 同结构；注册脚本仍可用。
3. 窄视口（或 DevTools &lt;900px）：上下布局。
4. 管理端切信标后门页左侧深色氛围正确。
5. 聊天侧栏品牌标为新图标。

---

## Spec coverage

| Spec 项 | Task |
|---------|------|
| 桌面左右分栏 | 2, 3 |
| 窄屏上下 | 3 |
| 新图标全站 | 1, 4 |
| 信笺/信标 | 3 |
| 功能不变 | 2, 4 |

## Self-review

- 无 TBD；提交步骤按用户规则省略。
- HTML 字段/脚本与现网一致。
