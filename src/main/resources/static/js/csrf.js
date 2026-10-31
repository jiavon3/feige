function readCsrf() {
    var match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
    return match ? decodeURIComponent(match[1]) : "";
}

function fillCsrfInputs() {
    var token = readCsrf();
    var inputs = document.querySelectorAll("input[name=_csrf]");
    for (var i = 0; i < inputs.length; i++) {
        inputs[i].value = token;
    }
}

fillCsrfInputs();
