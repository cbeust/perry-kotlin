var app = new Vue({
    el: '#app',
    data: {
        currentNumber: 0
    },
    created: function() {
        this.currentNumber = numberFromPath();
    },
    computed: {
        result: function() {
            var summary = this.findSummary(this.currentNumber);
            window.history.pushState(summary, "Issue " + this.currentNumber, "/summaries/" + this.currentNumber);
            return summary;
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
