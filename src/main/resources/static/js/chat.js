(function () {
    var me = null;
    var users = [];
    var hallUnread = 0;
    var activeId = null;
    var activeTitle = "大厅";
    var ws = null;
    var retry = 1000;
    var reconnectTimer = null;
    var titleTimer = null;
    var quoteTarget = null;
    var hasMore = false;
    var oldestId = null;
    var RECALL_MS = 2 * 60 * 1000;

    var hallButton = document.getElementById("hall");
    var hallBadge = document.getElementById("hallBadge");
    var userBox = document.getElementById("users");
    var title = document.getElementById("title");
    var log = document.getElementById("log");
    var status = document.getElementById("status");
    var flashBox = document.getElementById("flash");
    var form = document.getElementById("composer");
    var text = document.getElementById("text");
    var fileInput = document.getElementById("file");
    var imageInput = document.getElementById("image");
    var fileName = document.getElementById("fileName");
    var attachPreview = document.getElementById("attachPreview");
    var attachThumbs = document.getElementById("attachThumbs");
    var attachClear = document.getElementById("attachClear");
    var emojiToggle = document.getElementById("emojiToggle");
    var emojiPanel = document.getElementById("emojiPanel");
    var fontToggle = document.getElementById("fontToggle");
    var fontPanel = document.getElementById("fontPanel");
    var fontSizeChips = document.getElementById("fontSizeChips");
    var fontStyleChips = document.getElementById("fontStyleChips");
    var quoteBar = document.getElementById("quoteBar");
    var quoteWho = document.getElementById("quoteWho");
    var quoteText = document.getElementById("quoteText");
    var pendingImages = [];
    var pendingFile = null;
    var MAX_IMAGES = 9;
    var FONT_KEY = "feige.composerFont";
    var FONT_SIZES = [
        {id: "sm", label: "小", size: "15px"},
        {id: "md", label: "中", size: "17px"},
        {id: "lg", label: "大", size: "19px"},
        {id: "xl", label: "特大", size: "22px"}
    ];
    var FONT_STYLES = [
        {id: "sans", label: "默认", family: '"Avenir Next", "PingFang SC", "Noto Sans SC", sans-serif'},
        {id: "serif", label: "衬线", family: '"Iowan Old Style", Palatino, "Songti SC", "Noto Serif SC", serif'},
        {id: "rounded", label: "圆润", family: '"Avenir Next Rounded", "PingFang SC", "Hiragino Sans GB", sans-serif'},
        {id: "mono", label: "等宽", family: '"SF Mono", "ui-monospace", Menlo, Consolas, monospace'}
    ];
    var EMOJIS = [
        "😀", "😁", "😂", "🤣", "😊", "😍", "😘", "😜",
        "🤔", "🙄", "😒", "😢", "😭", "😡", "👍", "👎",
        "👏", "🙏", "🔥", "✨", "🎉", "❤️", "💔", "💯",
        "😎", "🤩", "😴", "🤗", "🤫", "🤝", "💪", "🌟",
        "🍀", "🌸", "🐶", "🐱", "☕", "🍺", "🍕", "🚀"
    ];

    function flash(message) {
        flashBox.textContent = message;
        flashBox.hidden = !message;
    }

    function escapeHtml(value) {
        return String(value).replace(/[&<>"']/g, function (char) {
            return {"&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;"}[char];
        });
    }

    function sizeLabel(bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
        return (bytes / 1024 / 1024).toFixed(1) + " MB";
    }

    function badgeLabel(count) {
        if (!count || count <= 0) return "";
        return count > 99 ? "99+" : String(count);
    }

    function api(url, options) {
        options = options || {};
        options.credentials = "same-origin";
        options.headers = options.headers || {};
        options.headers["X-XSRF-TOKEN"] = readCsrf();
        return fetch(url, options).then(function (response) {
            if (response.status === 401) {
                location.href = "/login";
                return Promise.reject(new Error("未登录"));
            }
            return response;
        });
    }

    function stopTitleFlash() {
        if (titleTimer) {
            clearInterval(titleTimer);
            titleTimer = null;
        }
        document.title = "飞鸽";
    }

    function maybeFlashTitle() {
        if (!document.hidden) return;
        if (titleTimer) return;
        var on = false;
        titleTimer = setInterval(function () {
            on = !on;
            document.title = on ? "【新消息】飞鸽" : "飞鸽";
        }, 1000);
    }

    document.addEventListener("visibilitychange", function () {
        if (!document.hidden) stopTitleFlash();
    });

    function setQuote(message) {
        quoteTarget = message;
        quoteWho.textContent = message.senderName;
        if (message.kind === "IMAGE") quoteText.textContent = "[图片]";
        else if (message.kind === "FILE") quoteText.textContent = "[文件] " + message.body;
        else quoteText.textContent = message.body;
        quoteBar.hidden = false;
    }

    function clearQuote() {
        quoteTarget = null;
        quoteBar.hidden = true;
    }

    document.getElementById("quoteCancel").addEventListener("click", clearQuote);

    function renderBadge(el, count) {
        var label = badgeLabel(count);
        if (!label) {
            el.hidden = true;
            el.textContent = "";
            return;
        }
        el.hidden = false;
        el.textContent = label;
    }

    function renderHallBadge() {
        renderBadge(hallBadge, hallUnread);
    }

    function renderUsers() {
        users.sort(function (a, b) {
            if (a.online !== b.online) return a.online ? -1 : 1;
            return a.username.localeCompare(b.username, "zh");
        });
        userBox.innerHTML = "";
        if (!users.length) {
            userBox.innerHTML = "<p class='me'>还没有其他用户</p>";
            return;
        }
        users.forEach(function (user) {
            var button = document.createElement("button");
            button.type = "button";
            button.className = "person" + (activeTitle === user.username ? " active" : "");
            var unread = user.unreadCount || 0;
            button.innerHTML = "<span class='dot" + (user.online ? " on" : "") + "'></span><span class='person-name'>"
                + escapeHtml(user.username) + "</span>"
                + (unread ? "<span class='badge'>" + escapeHtml(badgeLabel(unread)) + "</span>" : "");
            button.addEventListener("click", function () { openDirect(user); });
            userBox.appendChild(button);
        });
    }

    function quoteBlock(message) {
        if (!message.quoteMessageId && !message.quoteBody) return "";
        var who = message.quoteSenderName ? escapeHtml(message.quoteSenderName) + "：" : "";
        return "<div class='quote-ref'>" + who + escapeHtml(message.quoteBody || "") + "</div>";
    }

    function canRecall(message) {
        if (!me || message.senderId !== me.id || message.recalled) return false;
        return Date.now() - new Date(message.createdAt).getTime() <= RECALL_MS;
    }

    function messageBodyHtml(message) {
        if (message.recalled) {
            return "<div class='recalled'>" + escapeHtml(message.senderName) + "撤回了一条消息</div>";
        }
        if (message.kind === "IMAGE") {
            return "<a class='image-link' href='/api/files/" + message.id + "' target='_blank' rel='noopener'>"
                + "<img class='chat-image' src='/api/files/" + message.id + "' alt='" + escapeHtml(message.body || "图片") + "'>"
                + "</a>";
        }
        if (message.kind === "FILE") {
            return "<a href='/api/files/" + message.id + "'>" + escapeHtml(message.body) + "</a> · "
                + sizeLabel(message.sizeBytes || 0);
        }
        return "<div class='bubble-text'>" + escapeHtml(message.body) + "</div>";
    }

    function bindActions(row, message) {
        var actions = row.querySelector(".actions");
        if (!actions) return;
        var quoteBtn = actions.querySelector("[data-quote]");
        if (quoteBtn) {
            quoteBtn.addEventListener("click", function () { setQuote(message); });
        }
        var recallBtn = actions.querySelector("[data-recall]");
        if (recallBtn) {
            recallBtn.addEventListener("click", function () {
                api("/api/messages/" + message.id + "/recall", {method: "POST"}).then(function (response) {
                    if (!response.ok) {
                        return response.json().then(function (body) { flash(body.message || "撤回失败"); });
                    }
                });
            });
        }
    }

    function renderMessage(message, prepend) {
        if (message.conversationId !== activeId) return;
        var existing = document.getElementById("m" + message.id);
        if (existing) {
            applyMessageDom(existing, message);
            return;
        }
        var mine = me && message.senderId === me.id;
        var row = document.createElement("article");
        row.className = "bubble" + (mine ? " mine" : "") + (message.recalled ? " recalled-row" : "");
        row.id = "m" + message.id;
        applyMessageDom(row, message);
        if (prepend) {
            var historyBtn = document.getElementById("historyMore");
            if (historyBtn) log.insertBefore(row, historyBtn.nextSibling);
            else log.insertBefore(row, log.firstChild);
        } else {
            log.appendChild(row);
            log.scrollTop = log.scrollHeight;
        }
    }

    function applyMessageDom(row, message) {
        var mine = me && message.senderId === me.id;
        var when = new Date(message.createdAt).toLocaleString();
        var actions = "";
        if (!message.recalled) {
            actions = "<div class='actions'>"
                + "<button type='button' class='linkish' data-quote>引用</button>"
                + (canRecall(message) ? "<button type='button' class='linkish' data-recall>撤回</button>" : "")
                + "</div>";
        }
        row.className = "bubble" + (mine ? " mine" : "") + (message.recalled ? " recalled-row" : "");
        row.innerHTML = "<div class='meta'><span class='who'>" + escapeHtml(message.senderName) + "</span>"
            + "<time class='when'>" + escapeHtml(when) + "</time></div>"
            + "<div class='bubble-body'>" + quoteBlock(message) + messageBodyHtml(message) + "</div>"
            + actions;
        bindActions(row, message);
    }

    function applyRecall(messageId, senderName) {
        var row = document.getElementById("m" + messageId);
        if (!row) return;
        applyMessageDom(row, {
            id: messageId,
            conversationId: activeId,
            senderId: 0,
            senderName: senderName,
            kind: "TEXT",
            body: "",
            recalled: true,
            createdAt: new Date().toISOString()
        });
    }

    function ensureHistoryButton() {
        var existing = document.getElementById("historyMore");
        if (!hasMore) {
            if (existing) existing.remove();
            return;
        }
        if (!existing) {
            existing = document.createElement("button");
            existing.type = "button";
            existing.id = "historyMore";
            existing.className = "history-more";
            existing.textContent = "查看历史";
            existing.addEventListener("click", loadOlder);
            log.insertBefore(existing, log.firstChild);
        }
    }

    function loadHistory() {
        if (!activeId) return;
        api("/api/conversations/" + activeId + "/messages?limit=50").then(function (response) {
            return response.json();
        }).then(function (page) {
            log.innerHTML = "";
            hasMore = !!page.hasMore;
            var messages = page.messages || [];
            oldestId = messages.length ? messages[0].id : null;
            messages.forEach(function (message) { renderMessage(message, false); });
            ensureHistoryButton();
            markRead();
        });
    }

    function loadOlder() {
        if (!activeId || !hasMore || !oldestId) return;
        var previousHeight = log.scrollHeight;
        api("/api/conversations/" + activeId + "/messages?limit=50&beforeId=" + oldestId).then(function (response) {
            return response.json();
        }).then(function (page) {
            hasMore = !!page.hasMore;
            var messages = page.messages || [];
            if (messages.length) oldestId = messages[0].id;
            for (var i = messages.length - 1; i >= 0; i--) {
                renderMessage(messages[i], true);
            }
            ensureHistoryButton();
            log.scrollTop = log.scrollHeight - previousHeight;
        });
    }

    function markRead() {
        if (!activeId) return;
        api("/api/conversations/" + activeId + "/read", {method: "POST"}).then(function () {
            if (activeTitle === "大厅") {
                hallUnread = 0;
                renderHallBadge();
            } else {
                users.forEach(function (user) {
                    if (user.username === activeTitle) user.unreadCount = 0;
                });
                renderUsers();
            }
            stopTitleFlash();
        });
    }

    function bumpUnread(conversationId, senderId) {
        if (!me || senderId === me.id) return;
        if (conversationId === activeId && !document.hidden) {
            markRead();
            return;
        }
        if (me.hallConversationId === conversationId) {
            hallUnread += 1;
            renderHallBadge();
        } else {
            users.forEach(function (user) {
                if (user.conversationId === conversationId) {
                    user.unreadCount = (user.unreadCount || 0) + 1;
                }
            });
            renderUsers();
        }
        maybeFlashTitle();
    }

    function showConversation(id, label) {
        activeId = id;
        activeTitle = label;
        title.textContent = label;
        hallButton.className = "nav" + (label === "大厅" ? " active" : "");
        clearQuote();
        renderUsers();
        loadHistory();
    }

    function openDirect(user) {
        api("/api/conversations/direct", {
            method: "POST",
            headers: {"Content-Type": "application/json", "X-XSRF-TOKEN": readCsrf()},
            body: JSON.stringify({userId: user.id})
        }).then(function (response) {
            if (!response.ok) {
                return response.json().then(function (body) { flash(body.message || "无法打开私聊"); });
            }
            return response.json().then(function (conversation) {
                user.conversationId = conversation.id;
                showConversation(conversation.id, user.username);
            });
        });
    }

    function setConnected(connected) {
        status.hidden = connected;
        status.textContent = "连接已断开，正在重连";
        text.disabled = !connected;
        fileInput.disabled = !connected;
        imageInput.disabled = !connected;
        form.querySelector("button[type='submit']").disabled = !connected;
    }

    function connect() {
        if (ws && (ws.readyState === 0 || ws.readyState === 1)) return;
        var protocol = location.protocol === "https:" ? "wss" : "ws";
        ws = new WebSocket(protocol + "://" + location.host + "/ws");
        ws.onopen = function () {
            retry = 1000;
            setConnected(true);
            loadHistory();
        };
        ws.onclose = function () {
            setConnected(false);
            api("/api/me").then(function (response) {
                if (!response || response.status === 401) return;
                clearTimeout(reconnectTimer);
                reconnectTimer = setTimeout(connect, retry);
                retry = Math.min(retry * 2, 10000);
            }).catch(function () {});
        };
        ws.onmessage = function (event) {
            var data = JSON.parse(event.data);
            if (data.type === "message") {
                var message = data.message;
                if (message.conversationId === activeId) {
                    renderMessage(message, false);
                    if (!document.hidden) markRead();
                    else if (!me || message.senderId !== me.id) maybeFlashTitle();
                } else {
                    bumpUnread(message.conversationId, message.senderId);
                }
                if (message.conversationId !== me.hallConversationId) {
                    users.forEach(function (user) {
                        if (user.id === message.senderId && !user.conversationId) {
                            user.conversationId = message.conversationId;
                        }
                    });
                }
            }
            if (data.type === "recall") {
                if (data.conversationId === activeId) applyRecall(data.messageId, data.senderName);
            }
            if (data.type === "read") {
                if (data.conversationId === me.hallConversationId) {
                    hallUnread = 0;
                    renderHallBadge();
                } else {
                    users.forEach(function (user) {
                        if (user.conversationId === data.conversationId) user.unreadCount = 0;
                    });
                    renderUsers();
                }
            }
            if (data.type === "error") flash(data.message);
            if (data.type === "presence") {
                var found = false;
                users.forEach(function (user) {
                    if (user.id === data.userId) {
                        user.online = data.online;
                        found = true;
                    }
                });
                if (!found && data.userId !== me.id) {
                    users.push({id: data.userId, username: data.username, online: data.online, unreadCount: 0});
                }
                renderUsers();
            }
            if (data.type === "user.deleted") {
                users = users.filter(function (user) { return user.id !== data.userId; });
                renderUsers();
            }
            if (data.type === "conversations.deleted" && data.conversationIds.indexOf(activeId) >= 0) {
                showConversation(me.hallConversationId, "大厅");
            }
            if (data.type === "cleared") {
                flash("聊天记录和文件已清空");
                showConversation(me.hallConversationId, "大厅");
            }
            if (data.type === "theme") applyTheme(data.theme);
        };
    }

    function isImageFile(file) {
        return !!file && (
            /^image\/(jpeg|png|gif|webp)$/i.test(file.type || "") ||
            /\.(jpe?g|png|gif|webp)$/i.test(file.name || "")
        );
    }

    function normalizeImage(file, index) {
        var ext = ((file.type || "image/png").split("/")[1] || "png").replace("jpeg", "jpg");
        var name = file.name && file.name !== "image.png" ? file.name : ("粘贴图片" + (index + 1) + "." + ext);
        return new File([file], name, {type: file.type || "image/png"});
    }

    function clearAttachment() {
        pendingImages.forEach(function (item) {
            if (item.url) URL.revokeObjectURL(item.url);
        });
        pendingImages = [];
        pendingFile = null;
        imageInput.value = "";
        fileInput.value = "";
        fileName.textContent = "";
        attachThumbs.innerHTML = "";
        attachPreview.hidden = true;
    }

    function renderAttachments() {
        attachThumbs.innerHTML = "";
        if (!pendingImages.length && !pendingFile) {
            attachPreview.hidden = true;
            fileName.textContent = "";
            return;
        }
        attachPreview.hidden = false;
        if (pendingFile) {
            fileName.textContent = pendingFile.name || "附件";
            return;
        }
        fileName.textContent = pendingImages.length + " 张图片";
        pendingImages.forEach(function (item, index) {
            var wrap = document.createElement("div");
            wrap.className = "attach-thumb";
            var img = document.createElement("img");
            img.alt = item.file.name || "图片";
            img.src = item.url;
            var remove = document.createElement("button");
            remove.type = "button";
            remove.className = "remove";
            remove.textContent = "×";
            remove.addEventListener("click", function () {
                if (item.url) URL.revokeObjectURL(item.url);
                pendingImages.splice(index, 1);
                renderAttachments();
            });
            wrap.appendChild(img);
            wrap.appendChild(remove);
            attachThumbs.appendChild(wrap);
        });
    }

    function addImageFiles(fileList) {
        var added = 0;
        var skipped = 0;
        Array.prototype.forEach.call(fileList, function (file) {
            if (!isImageFile(file)) {
                skipped += 1;
                return;
            }
            if (file.size > 100 * 1024 * 1024 || file.size === 0) {
                skipped += 1;
                return;
            }
            if (pendingImages.length >= MAX_IMAGES) {
                skipped += 1;
                return;
            }
            var named = normalizeImage(file, pendingImages.length);
            pendingImages.push({file: named, url: URL.createObjectURL(named)});
            added += 1;
        });
        if (added) {
            pendingFile = null;
            fileInput.value = "";
            renderAttachments();
            flash("");
        }
        if (skipped && !added) flash("请选择有效图片，最多 " + MAX_IMAGES + " 张");
        else if (skipped) flash("已添加部分图片，最多 " + MAX_IMAGES + " 张");
    }

    function uploadOne(file, kind, quoteId) {
        var data = new FormData();
        data.append("file", file);
        if (quoteId) data.append("quoteMessageId", quoteId);
        var url = kind === "image"
            ? "/api/conversations/" + activeId + "/images"
            : "/api/conversations/" + activeId + "/files";
        return api(url, {method: "POST", body: data}).then(function (response) {
            if (!response.ok) {
                return response.json().then(function (body) {
                    return Promise.reject(new Error(body.message || "上传失败"));
                });
            }
            return response.json();
        });
    }

    function uploadImagesSequentially(files) {
        var quoteId = quoteTarget ? quoteTarget.id : null;
        var chain = Promise.resolve();
        files.forEach(function (file, index) {
            chain = chain.then(function () {
                return uploadOne(file, "image", index === 0 ? quoteId : null);
            });
        });
        return chain;
    }

    fileInput.addEventListener("change", function () {
        if (fileInput.files[0]) {
            clearAttachment();
            pendingFile = fileInput.files[0];
            renderAttachments();
        }
    });
    imageInput.addEventListener("change", function () {
        if (imageInput.files && imageInput.files.length) {
            addImageFiles(imageInput.files);
            imageInput.value = "";
        }
    });
    attachClear.addEventListener("click", clearAttachment);

    function readFontPrefs() {
        try {
            var raw = localStorage.getItem(FONT_KEY);
            if (raw) return JSON.parse(raw);
        } catch (ignore) {}
        return {size: "md", style: "sans"};
    }

    function writeFontPrefs(prefs) {
        try {
            localStorage.setItem(FONT_KEY, JSON.stringify(prefs));
        } catch (ignore) {}
    }

    function applyComposerFont(prefs) {
        var size = FONT_SIZES.filter(function (item) { return item.id === prefs.size; })[0] || FONT_SIZES[2];
        var style = FONT_STYLES.filter(function (item) { return item.id === prefs.style; })[0] || FONT_STYLES[0];
        text.style.fontSize = size.size;
        text.style.fontFamily = style.family;
        form.style.setProperty("--composer-font-size", size.size);
        form.style.setProperty("--composer-font-family", style.family);
        Array.prototype.forEach.call(fontSizeChips.querySelectorAll(".font-chip"), function (chip) {
            chip.classList.toggle("active", chip.getAttribute("data-size") === size.id);
        });
        Array.prototype.forEach.call(fontStyleChips.querySelectorAll(".font-chip"), function (chip) {
            chip.classList.toggle("active", chip.getAttribute("data-style") === style.id);
        });
    }

    function setEmojiOpen(open) {
        emojiPanel.hidden = !open;
        emojiToggle.setAttribute("aria-expanded", open ? "true" : "false");
        if (open) setFontOpen(false);
    }

    function closeEmoji() {
        setEmojiOpen(false);
    }

    function setFontOpen(open) {
        fontPanel.hidden = !open;
        fontToggle.setAttribute("aria-expanded", open ? "true" : "false");
        if (open) setEmojiOpen(false);
    }

    function closeFont() {
        setFontOpen(false);
    }

    function closePopovers() {
        closeEmoji();
        closeFont();
    }

    FONT_SIZES.forEach(function (item) {
        var chip = document.createElement("button");
        chip.type = "button";
        chip.className = "font-chip";
        chip.setAttribute("data-size", item.id);
        chip.textContent = item.label;
        chip.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();
            var prefs = readFontPrefs();
            prefs.size = item.id;
            writeFontPrefs(prefs);
            applyComposerFont(prefs);
        });
        fontSizeChips.appendChild(chip);
    });
    FONT_STYLES.forEach(function (item) {
        var chip = document.createElement("button");
        chip.type = "button";
        chip.className = "font-chip";
        chip.setAttribute("data-style", item.id);
        chip.textContent = item.label;
        chip.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();
            var prefs = readFontPrefs();
            prefs.style = item.id;
            writeFontPrefs(prefs);
            applyComposerFont(prefs);
        });
        fontStyleChips.appendChild(chip);
    });
    applyComposerFont(readFontPrefs());

    EMOJIS.forEach(function (emoji) {
        var button = document.createElement("button");
        button.type = "button";
        button.textContent = emoji;
        button.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();
            var start = text.selectionStart || text.value.length;
            var end = text.selectionEnd || text.value.length;
            var before = text.value.slice(0, start);
            var after = text.value.slice(end);
            if ((before + emoji + after).length > 2000) {
                flash("消息不能超过 2000 个字符");
                return;
            }
            text.value = before + emoji + after;
            var pos = start + emoji.length;
            text.focus();
            text.setSelectionRange(pos, pos);
        });
        emojiPanel.appendChild(button);
    });
    emojiToggle.addEventListener("click", function (event) {
        event.preventDefault();
        event.stopPropagation();
        setEmojiOpen(emojiPanel.hidden);
    });
    fontToggle.addEventListener("click", function (event) {
        event.preventDefault();
        event.stopPropagation();
        setFontOpen(fontPanel.hidden);
    });
    document.addEventListener("click", function (event) {
        var emojiWrap = emojiToggle.closest(".emoji-wrap");
        var fontWrap = fontToggle.closest(".font-wrap");
        if (!emojiPanel.hidden && !(emojiWrap && emojiWrap.contains(event.target))) closeEmoji();
        if (!fontPanel.hidden && !(fontWrap && fontWrap.contains(event.target))) closeFont();
    });
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") closePopovers();
    });

    text.addEventListener("paste", function (event) {
        var clipboard = event.clipboardData || window.clipboardData;
        if (!clipboard) return;
        var images = [];
        var items = clipboard.items;
        if (items) {
            for (var i = 0; i < items.length; i++) {
                if (items[i].type && items[i].type.indexOf("image/") === 0) {
                    images.push(items[i].getAsFile());
                }
            }
        }
        if (!images.length && clipboard.files && clipboard.files.length) {
            for (var j = 0; j < clipboard.files.length; j++) {
                if (clipboard.files[j].type && clipboard.files[j].type.indexOf("image/") === 0) {
                    images.push(clipboard.files[j]);
                }
            }
        }
        if (!images.length) return;
        event.preventDefault();
        addImageFiles(images);
    });

    text.addEventListener("keydown", function (event) {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit ? form.requestSubmit() : form.dispatchEvent(new Event("submit", {cancelable: true}));
        }
    });

    form.addEventListener("submit", function (event) {
        event.preventDefault();
        flash("");
        if (!ws || ws.readyState !== 1) return;
        closePopovers();
        if (pendingImages.length) {
            var files = pendingImages.map(function (item) { return item.file; });
            uploadImagesSequentially(files).then(function () {
                clearAttachment();
                clearQuote();
            }).catch(function (err) {
                flash(err.message || "上传失败");
            });
            return;
        }
        if (pendingFile) {
            if (pendingFile.size > 100 * 1024 * 1024) {
                flash("文件不能超过 100MB");
                return;
            }
            if (pendingFile.size === 0) {
                flash("不能上传空文件");
                return;
            }
            uploadOne(pendingFile, "file", quoteTarget ? quoteTarget.id : null).then(function () {
                clearAttachment();
                clearQuote();
            }).catch(function (err) {
                flash(err.message || "上传失败");
            });
            return;
        }
        var value = text.value;
        if (!value.trim()) return;
        var payload = {type: "send", conversationId: activeId, text: value};
        if (quoteTarget) payload.quoteMessageId = quoteTarget.id;
        ws.send(JSON.stringify(payload));
        text.value = "";
        clearQuote();
    });

    hallButton.addEventListener("click", function () {
        showConversation(me.hallConversationId, "大厅");
    });

    api("/api/me").then(function (response) { return response.json(); }).then(function (profile) {
        me = profile;
        hallUnread = profile.hallUnreadCount || 0;
        document.getElementById("me").textContent = profile.username;
        if (profile.role === "ADMIN") document.getElementById("adminLink").hidden = false;
        renderHallBadge();
        api("/api/users").then(function (response) { return response.json(); }).then(function (list) {
            users = list;
            renderUsers();
            showConversation(profile.hallConversationId, "大厅");
            connect();
        });
    });

    // WebSocket 不会刷新 HTTP 会话；聊天页打开时定期续期，避免只聊不动就被踢
    setInterval(function () {
        api("/api/me").catch(function () {});
    }, 15 * 60 * 1000);
})();
