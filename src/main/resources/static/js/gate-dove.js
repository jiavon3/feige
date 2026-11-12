(function () {
    var stage = document.querySelector(".gate-stage");
    var card = document.querySelector(".gate-panel-inner");
    var dove = document.getElementById("dove");
    if (!stage || !card || !dove) return;

    var usernameInput = card.querySelector("input[name=username]");
    var passwordInput = card.querySelector("input[name=password]");
    var submitBtn = card.querySelector(".gate-submit");
    var eyeBtn = card.querySelector(".gate-eye");
    var errorBox = document.getElementById("error");
    var tipBubble = document.getElementById("tipBubble");
    var pupilLeft = document.getElementById("pupilLeft");
    var pupilRight = document.getElementById("pupilRight");

    function tip(name) {
        return stage.getAttribute("data-tip-" + name) || "";
    }

    function moveEyes(x, y) {
        var value = "translate(calc(-50% + " + x + "px), calc(-50% + " + y + "px))";
        pupilLeft.style.transform = value;
        pupilRight.style.transform = value;
    }

    var tipTimer = 0;
    function setTip(text) {
        window.clearTimeout(tipTimer);
        if (tipBubble.textContent === text) {
            tipBubble.style.opacity = "1";
            return;
        }
        tipBubble.style.opacity = "0";
        tipTimer = window.setTimeout(function () {
            tipBubble.textContent = text;
            tipBubble.style.opacity = "1";
        }, 120);
    }

    function blink() {
        var eyes = document.querySelectorAll(".dove-eye");
        eyes.forEach(function (eye) {
            eye.classList.add("blinking");
            window.setTimeout(function () { eye.classList.remove("blinking"); }, 250);
        });
    }

    function restDove() {
        dove.style.transform = "";
        dove.classList.remove("wing-action");
        moveEyes(0, 0);
        setTip(tip("idle"));
    }

    function isFocused() {
        var active = document.activeElement;
        return active === usernameInput || active === passwordInput || active === submitBtn;
    }

    function showError(message) {
        if (!errorBox) return;
        errorBox.textContent = message;
        errorBox.hidden = false;
        setTip(tip("error"));
    }

    card.addEventListener("mouseenter", function () {
        if (document.activeElement === usernameInput || document.activeElement === passwordInput) return;
        dove.style.transform = "translateY(6px)";
        moveEyes(0, 6);
    });
    card.addEventListener("mouseleave", function () {
        if (document.activeElement === usernameInput || document.activeElement === passwordInput) return;
        restDove();
    });

    if (usernameInput) {
        usernameInput.addEventListener("focus", function () {
            moveEyes(-6, 4);
            dove.style.transform = "rotate(-3deg)";
            setTip(tip("username"));
            blink();
        });
        usernameInput.addEventListener("blur", function () {
            window.setTimeout(function () {
                if (!isFocused()) restDove();
            }, 0);
        });
    }

    if (passwordInput) {
        passwordInput.addEventListener("focus", function () {
            moveEyes(2, -5);
            dove.style.transform = "rotate(4deg)";
            setTip(tip("password"));
            blink();
        });
        passwordInput.addEventListener("blur", function () {
            window.setTimeout(function () {
                if (!isFocused()) restDove();
            }, 0);
        });
    }

    if (submitBtn) {
        submitBtn.addEventListener("mouseenter", function () {
            moveEyes(0, 8);
            dove.classList.add("wing-action");
            dove.style.transform = "translateY(-4px)";
            setTip(tip("submit"));
        });
        submitBtn.addEventListener("mouseleave", function () {
            if (document.activeElement === usernameInput || document.activeElement === passwordInput) return;
            restDove();
        });
        submitBtn.addEventListener("click", function () {
            dove.classList.add("wing-action");
            dove.style.transform = "translateY(-8px) scale(1.04)";
            blink();
        });
    }

    window.setInterval(function () {
        if (document.activeElement !== usernameInput && document.activeElement !== passwordInput) blink();
    }, 4200);

    if (eyeBtn) {
        eyeBtn.addEventListener("click", function () {
            var input = document.getElementById("password");
            var show = input.type === "password";
            input.type = show ? "text" : "password";
            eyeBtn.setAttribute("aria-label", show ? "隐藏密码" : "显示密码");
            blink();
        });
    }

    if (errorBox && location.search.indexOf("error") >= 0 && errorBox.textContent) {
        showError(errorBox.textContent);
    }

    window.feigeGate = {
        setError: showError,
        rest: restDove
    };
})();
