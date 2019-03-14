function httpGet(url, xmlHttp = new XMLHttpRequest()) {
    xmlHttp.open("GET", url, false ); // false for synchronous request
    xmlHttp.send(null);
    return xmlHttp.responseText;
}

function logout() {
    var xhr = new XMLHttpRequest();
    xhr.open("GET", "/api/logout", "logout", "logout");
    xhr.withCredentials = true;
    xhr.setRequestHeader("Authorization", 'Basic ' + btoa('logout:logout'));
    xhr.send(null);
    window.location.href = "/"
}

function numberFromPath() {
    var u = new URL(window.location);
    var paths = u.pathname.split("/");
    return parseInt(paths[paths.length - 1]);
}
