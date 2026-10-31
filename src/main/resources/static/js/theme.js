function applyTheme(theme) {
    if (theme !== "classic" && theme !== "signal") theme = "classic";
    document.documentElement.setAttribute("data-theme", theme);
    document.cookie = "feige-theme=" + encodeURIComponent(theme) + ";path=/;max-age=31536000;SameSite=Lax";
    var buttons = document.querySelectorAll("[data-theme-choice]");
    for (var i = 0; i < buttons.length; i++) {
        if (buttons[i].getAttribute("data-theme-choice") === theme) {
            buttons[i].className = "choice active";
        } else {
            buttons[i].className = "choice";
        }
    }
}

fetch("/api/theme", {credentials: "same-origin"}).then(function (response) {
    if (!response.ok) return;
    return response.json().then(function (body) {
        applyTheme(body.theme);
    });
}).catch(function () {});
