# 飞鸽（Feige）

局域网即时通信应用：一个 Spring Boot 可执行 jar，内置 H2 数据库和前端页面，启动后即可在办公室、教室或家庭局域网里群聊和私聊，不需要额外安装数据库、Redis 或消息队列。

## 功能特性

- **大厅群聊 + 一对一私聊**：所有登录用户都在大厅；点击联系人发起私聊，同一对用户只有一条私聊会话。
- **消息**：文本（最多 2000 字）、图片（jpeg/png/gif/webp）、文件（单个最大 100MB）；一次可发送最多 9 张图片或 20 个文件。
- **引用与撤回**：可引用同会话内的消息；自己的消息在 2 分钟内可撤回，撤回后文件同步删除。
- **实时体验**：WebSocket 推送新消息、撤回、已读、在线状态和资料变更；多标签页保持一致。
- **个人资料**：修改昵称、选择 16 款内置头像、上传自定义头像（≤2MB）、修改密码。
- **默认头像**：没有设置头像的用户自动使用「昵称/用户名首字 + 稳定配色」的圆形头像，不占用内置头像，也不使用系统 logo。
- **主题**：信笺（浅色）和信标（深色）两套主题，管理员切换后所有在线用户实时同步，刷新后仍生效。
- **会话清空**：聊天输入框工具栏的垃圾桶按钮可清空当前大厅/私聊的全部消息和文件，实时同步给会话参与者。
- **管理设置**（仅 `admin`）：查看并删除普通用户（连同其消息和文件）、清空全部聊天记录和文件、切换全局主题。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 8、Spring Boot 2.7 |
| 认证 | Spring Security（表单登录、BCrypt、CSRF） |
| 实时通信 | Spring WebSocket |
| 持久化 | Spring Data JPA + H2 文件数据库 |
| 前端 | 原生 HTML / CSS / JavaScript，无构建步骤 |
| 测试 | JUnit 5、Spring Boot Test、MockMvc |

## 快速开始

### 环境要求

- JDK 8 或更高（建议 8 / 11 / 17）
- Maven 3.6+

### 启动

```bash
git clone <your-repo-url> feige
cd feige
./start.sh          # 打包并启动，默认 8080 端口
./start.sh 9090     # 指定端口
PORT=9090 ./start.sh
```

也可以手动打包运行：

```bash
mvn -DskipTests package
java -jar target/feige.jar --server.port=8080
```

开发时可以直接运行：

```bash
mvn spring-boot:run
```

从 IDE 启动 `feige.FeigeApplication` 也可以；此时数据目录是项目根目录下的 `data/`。

启动日志会打印本机和局域网访问地址，例如：

```text
数据目录: /path/to/feige/target/data
本机访问: http://127.0.0.1:8080
局域网访问: http://192.168.1.10:8080
```

浏览器打开对应地址即可。服务默认监听 `0.0.0.0`，同一局域网的设备可直接访问。

> 同一时间只能运行一个实例：H2 文件数据库会加文件锁。`start.sh` 会检测已有进程并提示先停止。

### 默认管理员

首次启动会自动创建管理员账号：

- 用户名：`admin`
- 密码：`admin123`

**登录后请立即修改密码**：点击左下角账号 → 修改密码。管理员不能被删除，也可以像普通用户一样参与大厅和私聊。

## 使用说明

1. **注册**：打开 `/register`，用户名 2–20 位字母、数字或汉字（`admin` 保留），密码 6–64 位。注册成功会回到登录页。
2. **大厅**：登录后默认进入大厅，所有在线用户都能看到大厅消息。
3. **私聊**：点击左侧联系人即可开始私聊；顶部标题显示对方昵称或用户名。
4. **发送附件**：输入框工具栏可发送图片或任意文件；也可以直接粘贴图片。文件消息点击后下载。
5. **引用 / 撤回**：气泡底部有「引用 / 撤回」；引用只在同一会话内有效，撤回窗口为 2 分钟。
6. **昵称与头像**：点击左下角头像更换内置或自定义头像；点击昵称打开菜单，可修改昵称和密码。
7. **主题**：管理员在「管理设置」里切换信笺/信标，所有在线用户同步切换。
8. **清空会话**：输入框工具栏最右侧的垃圾桶图标可清空当前会话；需要确认，删除后不可恢复。
9. **管理设置**（仅 `admin`）：左下角账号菜单 → 管理设置，可删除普通用户、清空全部记录、切换主题。

## 数据与备份

数据默认保存在 **jar 同级目录的 `data/`**（使用 `start.sh` 时是 `target/data/`）：

```text
data/
├── feige.mv.db     H2 数据库：用户、消息、会话、设置
├── files/          聊天上传的文件（UUID 文件名）
└── avatars/        自定义头像（UUID 文件名）
```

- **备份**：停止服务后复制整个 `data/` 目录。
- **重置**：停止服务后删除 `data/`，再次启动会重新初始化数据库并创建默认 `admin`（原有数据全部丢失）。
- 实际路径以启动日志中的「数据目录」为准。

## 配置

主要配置在 `src/main/resources/application.yml`：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | `8080` | 端口，也可用 `PORT` 环境变量或 `--server.port` 覆盖 |
| `server.servlet.session.timeout` | `24h` | 会话空闲过期时间 |
| `spring.servlet.multipart.max-file-size` | `100MB` | 单个上传文件上限 |
| `spring.servlet.multipart.max-request-size` | `100MB` | 单次请求上限 |
| `spring.jpa.hibernate.ddl-auto` | `update` | H2 表结构自动升级 |

前端还有一组交互限制（单文件 100MB、一次最多 9 张图片 / 20 个文件、文本 2000 字），修改时请同步后端校验：`ChatService.MAX_TEXT`、`FileStorage.MAX_BYTES` 以及 `chat.js` 中的常量。

## 项目结构

```text
src/main/java/feige/
├── bootstrap/    启动初始化、默认管理员、启动日志
├── config/       安全配置、WebSocket 配置、数据目录解析
├── domain/       JPA 实体（用户、会话、消息、设置）
├── repo/         Spring Data JPA 仓库
├── service/      业务逻辑（聊天、文件、资料、管理、在线状态）
└── web/          REST 控制器、WebSocket 处理器、会话跟踪

src/main/resources/
├── application.yml
└── static/       登录、注册、聊天页面及前端资源

src/test/java/feige/   自动化测试
docs/                  设计文档
start.sh               打包并启动
pom.xml
```

## 接口概览

### HTTP

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/register` | 注册，JSON：`username`、`password` |
| GET | `/api/me` | 当前用户、角色、大厅会话 id、未读数 |
| GET | `/api/users` | 联系人列表、在线状态、未读数 |
| POST | `/api/conversations/direct` | 打开或创建私聊，JSON：`userId` |
| GET | `/api/conversations/{id}/messages` | 会话历史（分页 50） |
| DELETE | `/api/conversations/{id}/messages` | 清空该会话消息和文件 |
| POST | `/api/conversations/{id}/read` | 标记已读 |
| POST | `/api/conversations/{id}/files` | 上传文件 |
| POST | `/api/conversations/{id}/images` | 上传图片 |
| POST | `/api/messages/{id}/recall` | 撤回消息（2 分钟内） |
| GET | `/api/files/{messageId}` | 下载文件，权限同会话 |
| POST | `/api/me/profile` | 修改昵称或选择内置头像 |
| POST | `/api/me/avatar` | 上传自定义头像 |
| POST | `/api/me/password` | 修改密码 |
| GET | `/api/theme` | 当前主题 |
| GET | `/api/avatars/builtin` | 内置头像列表 |
| GET | `/api/avatars/{name}` | 获取自定义头像 |
| GET | `/api/admin/users` | 普通用户列表（仅 admin） |
| DELETE | `/api/admin/users/{id}` | 删除用户及其数据（仅 admin） |
| POST | `/api/admin/clear` | 清空全部消息和文件（仅 admin） |
| POST | `/api/admin/theme` | 切换全局主题（仅 admin） |

### WebSocket

地址：`/ws`，握手需要已登录 Session 且来源同源。

客户端发送：

```json
{"type":"send","conversationId":1,"text":"你好","quoteMessageId":null}
```

服务端事件：`message`、`recall`、`read`、`presence`、`profile`、`theme`、`user.deleted`、`conversations.deleted`、`cleared`、`conversation.cleared`、`error`。完整字段见 `docs/` 下的设计文档。

## 测试

```bash
mvn test
```

测试使用内存 H2 和临时数据目录，不会读写正式 `data/`。当前包含 32 个自动化测试，覆盖注册登录、权限、私聊隔离、文件、撤回、引用、历史分页、资料/头像、管理员操作、在线计数等。

## 安全说明

- 本项目按**局域网 HTTP** 场景设计，默认没有 HTTPS；需要跨公网访问时请放到反向代理/VPN 之后。
- 默认管理员密码 `admin123` 仅用于首次启动，部署后必须修改。
- 登录使用 Session（默认空闲 24 小时过期），密码只保存 BCrypt 哈希，写操作使用 CSRF Token。
- WebSocket 只接受已登录且同源的连接。
- 文件下载受会话权限保护：私聊文件只有双方可下载，大厅文件对所有登录用户开放。
- 任何会话参与者都可以清空该会话；管理员可以清空全部记录。相关操作都有浏览器确认，但删除后不可恢复。

## 常见问题

**端口被占用**：`./start.sh 9090` 换端口，或停止占用 8080 的进程。

**局域网其他设备打不开**：确认在同一网段、系统防火墙放行端口，并检查启动日志里的「局域网访问」地址。

**忘记管理员密码**：可以停止服务后删除 `data/` 重置（数据会全丢）。

**能否同时启动多个实例**：不能，H2 文件数据库有单进程锁；需要多实例请改用独立数据库并拆分在线状态/WebSocket 推送。

## 许可证

仓库目前还没有 `LICENSE` 文件。如果准备让其他人使用或再分发，建议先补充许可证（例如 MIT），并在本 README 中更新许可证说明。
