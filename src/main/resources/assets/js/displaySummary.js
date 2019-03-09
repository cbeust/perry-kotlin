var app = new Vue({
    el: '#app',
    computed: {
        summary: function() {
            var urlParams = new URLSearchParams(window.location.search);
            var number = urlParams.get("number");
            var summary = JSON.parse(httpGet('/api/summaries/' + number));
            return summary;
        }
    }
});