function httpGet(theUrl)
{
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.open( "GET", theUrl, false ); // false for synchronous request
    xmlHttp.send( null );
    return xmlHttp.responseText;
}

var app = new Vue({
    el: '#app',
    computed: {
        allCycles: function() {
            var cycles = JSON.parse(httpGet('/api/cycles'));
            var cycleCount = cycles.cycles.length;
            console.log("Number of cycles: " + cycleCount);
            return cycles;
        }
    }
});