var app = new Vue({
    el: '#app',
    computed: {
        cycle: function() {
            var number = 3;
            var cycle = JSON.parse(httpGet('/api/cycles/' + number));
            return cycle;
        }
    }
});