console.log("Creating addCycle Vue application");

var app = new Vue({
    el: '#app',
    data: {
        number: 1,
        result: null,
        test: [0, 1, 2],
    },
    created: function () {
        this.result = this.fetch();
        console.log("Result: " + this.result.length + " cycles");
        this.number = this.result.length;
        // this.twitter = httpGet("/api/test/authTwitter");
        // this.gmail = httpGet("/api/test/authGmail");
    },
    methods: {
        find: function() {
            var apiUrl = "/api/cycles";
            return JSON.parse(httpGet(apiUrl));
        },

        fetch: function () {
            const result = this.find();
            // window.history.pushState(result,
            //     text + " " + this.currentNumber, htmlUrl + "/" + this.currentNumber);
            return result;
        },
    }
});

