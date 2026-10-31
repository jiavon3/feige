(function () {
    var rows = document.getElementById("rows");
    var empty = document.getElementById("empty");
    var flash = document.getElementById("flash");

    function show(message) {
        flash.textContent = message;
        flash.hidden = !message;
    }

    function load() {
        fetch("/api/admin/users", {credentials: "same-origin", headers: {"X-XSRF-TOKEN": readCsrf()}})
            .then(function (response) { return response.json(); })
            .then(function (users) {
                rows.innerHTML = "";
                empty.hidden = users.length > 0;
                users.forEach(function (user) {
                    var tr = document.createElement("tr");
                    var when = new Date(user.createdAt).toLocaleString();
                    tr.innerHTML = "<td>" + escapeHtml(user.username) + "</td><td>" + escapeHtml(when) + "</td><td></td>";
                    var button = document.createElement("button");
                    button.type = "button";
                    button.textContent = "删除";
                    button.addEventListener("click", function () {
                        if (!confirm("确定删除用户「" + user.username + "」及其消息和文件吗？")) return;
                        fetch("/api/admin/users/" + user.id, {
                            method: "DELETE",
                            credentials: "same-origin",
                            headers: {"X-XSRF-TOKEN": readCsrf()}
                        }).then(function (response) {
                            if (!response.ok) {
                                return response.json().then(function (body) { show(body.message || "删除失败"); });
                            }
                            show("已删除 " + user.username);
                            load();
                        });
                    });
                    tr.lastChild.appendChild(button);
                    rows.appendChild(tr);
                });
            });
    }

    function escapeHtml(value) {
        return String(value).replace(/[&<>"']/g, function (char) {
            return {"&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;"}[char];
        });
    }

    var choices = document.querySelectorAll("[data-theme-choice]");
    for (var i = 0; i < choices.length; i++) {
        choices[i].addEventListener("click", function (event) {
            var theme = event.currentTarget.getAttribute("data-theme-choice");
            fetch("/api/admin/theme", {
                method: "POST",
                credentials: "same-origin",
                headers: {"Content-Type": "application/json", "X-XSRF-TOKEN": readCsrf()},
                body: JSON.stringify({theme: theme})
            }).then(function (response) {
                if (!response.ok) {
                    return response.json().then(function (body) { show(body.message || "切换失败"); });
                }
                return response.json().then(function (body) {
                    applyTheme(body.theme);
                    show(body.theme === "signal" ? "已切换为信标" : "已切换为信笺");
                });
            });
        });
    }

    document.getElementById("clear").addEventListener("click", function () {
        if (!confirm("确定清空全部聊天记录和文件吗？账号会保留。")) return;
        fetch("/api/admin/clear", {
            method: "POST",
            credentials: "same-origin",
            headers: {"X-XSRF-TOKEN": readCsrf()}
        }).then(function (response) {
            if (!response.ok) {
                return response.json().then(function (body) { show(body.message || "清空失败"); });
            }
            show("已清空全部聊天记录和文件");
        });
    });

    load();
})();
