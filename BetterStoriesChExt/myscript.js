var dataForAnalize = getElementByXpath('div.news_list > div.item > a[title]');
var request = {
    classify: []
};
$.each(dataForAnalize, function (key, value) {
    var data = $(this).attr('title');
    request.classify.push({
        "index": "" + key,
        "data": data
    });

    var detailVal = {'detail': data};
    var valTxt = JSON.stringify(detailVal);

    var buttonBad = "<button betterStories='bad' id='betterStoriesBad' onclick='var e = new CustomEvent(\"betterStoriesBad\" , %data% ); document.dispatchEvent(e);'>Лошо</button>".replace("%data%", valTxt);
    var buttonGood = "<button betterStories='good' id='betterStoriesGood' onclick='var e = new CustomEvent(\"betterStoriesGood\" , %data% ); document.dispatchEvent(e);'>Добро</button>".replace("%data%", valTxt);

    value.outerHTML = value.outerHTML +
    buttonGood + buttonBad;
});

makeCorsRequest(request);

document.addEventListener("betterStoriesBad", function (e) {
    sendCorpus('bad', e.detail);
});


document.addEventListener("betterStoriesGood", function (e) {
    sendCorpus('good', e.detail);
});

function sendCorpus(type, data) {
    console.log("send corpus");
    var corpus = {
        "store": {
            "type": type,
            "data": data
        }
    };
    console.log(JSON.stringify(corpus));
    makeCorsRequest(corpus);
}

function getElementByXpath(path) {
    return $(path);
}

// Create the XHR object.
function createCORSRequest(method, url) {
    var xhr = new XMLHttpRequest();
    if ("withCredentials" in xhr) {
        // XHR for Chrome/Firefox/Opera/Safari.
        xhr.open(method, url, true);
    } else if (typeof XDomainRequest != "undefined") {
        // XDomainRequest for IE.
        xhr = new XDomainRequest();
        xhr.open(method, url);
    } else {
        // CORS not supported.
        xhr = null;
    }
    return xhr;
}

// Make the actual CORS request.
function makeCorsRequest(data) {
    // All HTML5 Rocks properties support CORS.
    var url = 'http://localhost:8080/BetterStoriesServer/classify';

    var xhr = createCORSRequest('POST', url);
    if (!xhr) {
        alert('CORS not supported');
        return;
    }

    // Response handlers.
    xhr.onload = function () {
        var text = xhr.responseText;

        var obj = JSON.parse(text);
        for (var i = obj.classified.length - 1; i >= 0; i--) {
            if (obj.classified[i].result == "1") {
                var index = obj.classified[i].index;
                var item = $("div.news_list > div.item:has(a[title]):nth-child" + "(" + (i + 2) + ")");
                item.attr("betterStories", "block");
            }
        }
        ;
    };

    xhr.onerror = function () {
        alert('Woops, there was an error making the request.');
    };

    xhr.send(JSON.stringify(data));
}