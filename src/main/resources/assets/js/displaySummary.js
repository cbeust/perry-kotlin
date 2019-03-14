var app = new Vue({
    el: '#app',
    data: {
        currentNumber: 0
    },
    created: function() {
        this.currentNumber = numberFromPath();
    },
    computed: {
        summary: function() {
            var result = this.findSummary(this.currentNumber);
            window.history.pushState(result, "Issue " + this.currentNumber,
                "/summaries/" + this.currentNumber);
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
