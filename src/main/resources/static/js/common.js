function httpGet(url, xmlHttp = new XMLHttpRequest()) {
    xmlHttp.open("GET", url, false ); // false for synchronous request
    xmlHttp.send(null);
    return xmlHttp.responseText;
}

function sendEmailMailingList(number) {
    httpGet("/api/sendEmail?number=" + number);
}

function numberFromPath() {
    const u = new URL(window.location);
    const paths = u.pathname.split("/");
    return parseInt(paths[paths.length - 1]);
}

function openForm() {
    document.getElementById("login").style.display = "none";
    document.getElementById("login-modal").style.display = "block";
}

function closeForm() {
    document.getElementById("login-modal").style.display = "none";
}

const createApp = function (htmlUrl, apiUrl, text) {
    return {
        el: '#app',
        data: {
            currentNumber: 0,
            result: null
        },
        created: function () {
            this.currentNumber = numberFromPath();
            this.result = this.fetch();
        },
        methods: {
            fetch: function () {
                const result = this.find(this.currentNumber);
                window.history.pushState(result,
                    text + " " + this.currentNumber, htmlUrl + "/" + this.currentNumber);
                return result;
            },
            find: function (number) {
                return JSON.parse(httpGet(apiUrl + "/" + number));
            },
            next: function () {
                this.currentNumber++;
                this.result = this.fetch();
            },
            previous: function () {
                if (this.currentNumber > 1) {
                    this.currentNumber--;
                    this.result = this.fetch();
                }
            }
        }
    };
};

function submitSummary() {
    $("#editSummaryForm").submit( function(eventObj) {
        $('<input />')
            .attr('type', 'hidden')
            .attr('name', 'summary')
            .attr('value', document.getElementById("summaryText").innerHTML)
            .appendTo('#editSummaryForm');

        $('<input />')
            .attr('type', 'hidden')
            .attr('name', 'date')
            .attr('value', document.getElementById("date").innerHTML)
            .appendTo('#editSummaryForm');

        return true;
    });
}

function cancelSummary(cancelUrl) {
    document.location.href = cancelUrl;
}