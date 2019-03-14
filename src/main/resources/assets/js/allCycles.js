var app = new Vue({
    el: '#app',
    computed: {
        allCycles: function() {
            var cycles = JSON.parse(httpGet('/api/cycles'));
            return cycles;
        }
    }
});