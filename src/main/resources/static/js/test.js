console.log("Creating Vue application");

var app = new Vue({
    el: '#app',
    data: {
        twitter: "<unknown>",
        gmail: "<unknown>",
    },
    created: function () {
        this.twitter = httpGet("/api/test/authTwitter");
        this.gmail = httpGet("/api/test/authGmail");
    },
    methods: {
    }
});

