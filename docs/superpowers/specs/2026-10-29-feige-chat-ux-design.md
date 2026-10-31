# 飞鸽聊天体验升级设计

日期：2026-10-29

## 目标

在现有局域网单 jar 聊天上增加：消息撤回、引用回复、图片发送与预览、会话未读角标、页签标题新消息提示、聊天区独立滚动与历史分页，以及飞鸽品牌图标。

## 已定决策

- 在现有消息模型与 WebSocket 上增量扩展，不引入独立事件流。
- 撤回：仅本人，发送后 2 分钟内。成功后显示「某某撤回了一条消息」，原文与磁盘文件删除。
- 引用：发送带 `quoteMessageId`。被引已撤回时摘要为「引用内容已撤回」。
- 图片：独立「选择图片」；类型 `IMAGE`；jpeg/png/gif/webp；缩略图预览，点击新标签看原图（inline）。
- 未读：`conversation_members.last_read_message_id`；打开会话清零；自己发送不加未读；角标 `99+`。
- 页签提示：仅标题在「飞鸽」与「【新消息】飞鸽」间闪动；不使用桌面通知。
- 历史：默认最近 50 条；「查看历史」每次再往前 50 条。
- 布局：仅消息列表滚动；标题与输入区（含引用条）固定。
- 大厅也有成员行，用于记录已读与未读。
- 图标：自绘 SVG，favicon 与各页品牌旁共用。

## 不做的事

系统桌面通知、已读回执细节（谁读了）、消息编辑、超过 2 分钟的撤回、管理员代撤。

## 数据模型

### messages

新增：

- `recalled` boolean，默认 false
- `quote_message_id` 可空
- `kind` 增加 `IMAGE`

撤回后：`recalled=true`，清空 `stored_name` / `size_bytes`，`body` 对外不再返回原文。

### conversation_members

新增：

- `last_read_message_id` 可空

大厅：每个账号一条大厅成员记录。注册时加入；启动时为已有账号补齐。

## API

- `GET /api/conversations/{id}/messages?beforeId=&limit=50`  
  响应：`{ messages, hasMore }`。无 `beforeId` 时取最新一页；有则取更早一页。列表按时间正序。
- `POST /api/conversations/{id}/read`  
  将当前用户该会话 `lastReadMessageId` 推到该会话最大消息 id。并向该用户其它连接推送 `{type:read, conversationId, lastReadMessageId}`。
- `POST /api/messages/{id}/recall`  
  校验本人与 2 分钟窗口；删除磁盘文件；广播 `{type:recall, messageId, conversationId, senderName}`。
- `POST /api/conversations/{id}/images`  
  multipart 字段 `file`，可选 `quoteMessageId`。校验图片类型。
- `POST /api/conversations/{id}/files`  
  可选 `quoteMessageId`。
- `GET /api/me`  
  增加 `hallUnreadCount`。
- `GET /api/users`  
  每项增加 `conversationId`（若已有私聊）、`unreadCount`。
- `GET /api/files/{id}`  
  `IMAGE` 使用 `Content-Disposition: inline`，其它仍为 `attachment`。

WebSocket `send` 可带 `quoteMessageId`。`message` 推送含 `recalled`、引用摘要字段与 `kind:IMAGE`。

## 前端

- 气泡：撤回（本人且 2 分钟内）、引用（未撤回）。
- 输入区上方引用预览，可取消。
- 「选择图片」与「选择文件」。
- 左侧大厅与用户未读角标。
- `document.hidden` 时闪标题；可见或打开会话后停止。
- `#log` 唯一滚动；顶部「查看历史」。
- classic / signal 主题样式覆盖新控件。

## 错误文案

- 超过两分钟，无法撤回
- 只能撤回自己的消息
- 无法引用该消息
- 请选择图片文件

## 测试

- 撤回成功 / 超时 / 非本人；文件删除；WS 同步
- 引用跨会话拒绝；引用已撤回展示
- 图片非法类型；inline；大厅/私聊可见性
- 历史分页 `hasMore`
- 未读加减；大厅成员补齐
- 主题、清空、删用户回归
