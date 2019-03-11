function httpGet(url) {
    httpGet(url, new XMLHttpRequest());
}

function httpGet(theUrl, xmlHttp)
{
    xmlHttp.open("GET", theUrl, false ); // false for synchronous request
    xmlHttp.send(null);
    return xmlHttp.responseText;
}

function logout() {
    var xhr = new XMLHttpRequest();
    xhr.withCredentials = true;
    xhr.setRequestHeader("Authorization", 'Basic ' + btoa('a:b'));
    httpGet("/logout")
}
