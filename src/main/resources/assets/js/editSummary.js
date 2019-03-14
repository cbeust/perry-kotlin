var app = new Vue({
    el: '#app',
    computed: {
        summary: function() {
            var urlParams = new URLSearchParams(window.location.search);
            var number = urlParams.get("number");
            return JSON.parse(httpGet('/api/editSummary/' + number));
        }
    }
});
