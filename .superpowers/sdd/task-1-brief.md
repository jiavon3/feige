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
