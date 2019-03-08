var app = new Vue({
    el: '#app',
    computed: {
        allCycles: function() {
            var cycles = JSON.parse(httpGet('/api/cycles'));
            var cycleCount = cycles.cycles.length;
            console.log("Number of cycles: " + cycleCount);
            return cycles;
        }
    }
});