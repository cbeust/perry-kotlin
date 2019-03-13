function httpGet(url, xmlHttp = new XMLHttpRequest()) {
    xmlHttp.open("GET", url, false ); // false for synchronous request
    xmlHttp.send(null);
    return xmlHttp.responseText;
}

function logout() {
    var xhr = new XMLHttpRequest();
    xhr.withCredentials = true;
    xhr.setRequestHeader("Authorization", 'Basic ' + btoa('a:b'));
    httpGet("/logout", xhr);
}
