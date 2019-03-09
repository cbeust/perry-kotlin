var app = new Vue({
    el: '#app',
    computed: {
        cycle: function() {
            var urlParams = new URLSearchParams(window.location.search);
            var number = urlParams.get("number");
            var cycle = JSON.parse(httpGet('/api/cycles/' + number));
            return cycle;
        }
    }
});