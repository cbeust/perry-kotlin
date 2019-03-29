function httpGet(url, xmlHttp = new XMLHttpRequest()) {
    xmlHttp.open("GET", url, false ); // false for synchronous request
    xmlHttp.send(null);
    return xmlHttp.responseText;
}

function numberFromPath() {
    var u = new URL(window.location);
    var paths = u.pathname.split("/");
    return parseInt(paths[paths.length - 1]);
}
