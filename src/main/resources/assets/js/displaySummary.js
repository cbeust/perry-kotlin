var app = new Vue({
    el: '#app',
    data: {
        currentNumber: 0
    },
    created: function() {
        var urlParams = new URLSearchParams(window.location.search);
        this.currentNumber = urlParams.get("number");
    },
    computed: {
        summary: function() {
            var result = this.findSummary(this.currentNumber);
            window.history.pushState(result, "Issue " + this.currentNumber,
                "/static/displaySummary.html?number=" + this.currentNumber);
            return result;
        }
    },
    methods: {
        findSummary: function(number) {
            var summary = JSON.parse(httpGet('/api/summaries/' + number));
            return summary;
        },
        nextSummary: function() {
            this.currentNumber++;
        },
        previousSummary: function() {
            if (this.currentNumber > 0) this.currentNumber--;
        }
    }
});
