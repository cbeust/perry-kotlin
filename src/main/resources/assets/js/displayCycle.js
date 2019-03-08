var app = new Vue({
    el: '#app',
    computed: {
        booksForCycle: function() {
            var number = 3;
            var books = JSON.parse(httpGet('/api/booksForCycle/' + number));
            return books;
        }
    }
});