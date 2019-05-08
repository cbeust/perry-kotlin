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
            if (summary.found) {
                window.history.pushState(summary, "Issue " + this.currentNumber, "/pending/" + this.currentNumber);
            }
            return summary;
        }
    },
    methods: {
        findSummary: function(number) {
            var summary = JSON.parse(httpGet('/api/pending/' + number));
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
