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
