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
