var siteConsole = window.console;
delete console;
var ibConsole = window.console;
window.console = siteConsole;
if (!ibConsole){
    ibConsole = siteConsole;
}

if (ibIsDocumentSomewhatReady(document)) {
    ibAddInitFunctions();
}
function ibIsDocumentSomewhatReady(doc) {
    if (document.readyState === "complete" || document.readyState === "interactive") {
        return true;
    } else {
        return false;
    }
}

ibAddEventListener(self.document, "DOMContentLoaded", ibAddInitFunctions, false);


function ibAddInitFunctions() {
    ibAddMessagePostHandlers();
    ibAddClickHandlerForAll();
    ibFindAllVideos();
}

function ibAddEventListener(obj, ev, func, when) {
    obj.removeEventListener(ev, func, when);
    obj.addEventListener(ev, func, when);
}

function ibAddClickHandlerForAll() {
    ibFindAllDocuments();

    ibAddClickHandler(self.document);
}
function ibAddClickHandler(doc) {
    ibAddEventListener(doc, "click", ibClicked, true);

}
function ibClicked() {
    ibFindAllVideos();
}
function ibFindAllDocuments() {
    var docs = new Array();
    docs.push(self.document);
    ibFindAllIFrames(document, docs);
    return docs;
}
function ibCanAccessIFrame(iframe) {
    var html = null;
    try {
        // deal with older browsers
        var doc = iframe.contentDocument || iframe.contentWindow.document;
        html = doc.body.innerHTML;
    } catch (err) {
        // do nothing
    }

    return(html !== null);
}
function ibFindAllIFrames(d, docs) {
    var ibFrames = d.getElementsByTagName('iframe');
    for (var i = 0; i < ibFrames.length; i++) {
        try {
            var ibFrame = ibFrames[i];
            if (ibCanAccessIFrame(ibFrame)) {
                var doc = ibFrame.contentDocument;
                if (ibIsDocumentSomewhatReady(doc)) {
                    ibAddSelf(doc);
                } else {
                    ibAddIframeLoadEvent(ibFrame);
                }
                docs.push(doc);
                ibFindAllIFrames(doc, docs);
            } else {
                ibConsole.log("Unable to access iframe " + ibFrame.src );


            }

        } catch (e) {
            //must be cross domain
        }
    }
}
function ibAddIframeLoadEvent(iframe) {
    ibAddEventListener(iframe, "load", ibIframeLoaded, false);

}
function ibIframeLoaded() {
    try {
        var doc = this.contentDocument;
        ibAddSelf(doc);

    } catch (e) {
        //must be cross domain
    }
}
var ibScriptIDTag = "ibFunctionsScriptTagInIFrame"
function ibAddSelf(doc) {
    if (doc.getElementById(ibScriptIDTag) == null) {
        var script = doc.createElement('script');
        script.src = '/ibjslib/wai2iku6/ibfunctions.js';
        script.id = ibScriptIDTag;
        script.async = false;

        doc.documentElement.appendChild(script);
    }
}
function ibFindAllVideos() {
    var docs = ibFindAllDocuments();
    var foundVideos = new Array();
    for(var d = 0; d < docs.length; d++){
         try{
            var videos = docs[d].getElementsByTagName('video');
            for (var i = 0; i < videos.length; i++) {
                var video = videos[i];
                foundVideos.push(video);
                ibAddEventListener(video, "play", ibVideoPlaying, true);
            }

          }catch(e){
                ibLog("Error searching videos " + e);
            }
    }
    ibSendListOfVideos(foundVideos);
    var findVideos = new Object();
    findVideos.type = "findVideos";
    ibPostMessageToIFrame(findVideos);

}
function ibPauseAllVideos() {
    var videos = self.document.getElementsByTagName('video');
    for (var i = 0; i < videos.length; i++) {
        var video = videos[i];
        video.pause();
    }
    var pause = new Object();
    pause.type = "pause";
    ibPostMessageToIFrame(pause);
}
function ibSkipAds() {
    var videos = self.document.getElementsByTagName('video');
    for (var i = 0; i < videos.length; i++) {
        var video = videos[i];
        //video.pause();
        if (video.currentTime < video.duration - 0.1) {
            video.currentTime = video.duration - 0.1;
            video.play();
        }
    }
    var pause = new Object();
    pause.type = "pause";
    ibPostMessageToIFrame(pause);
}
function ibVideoPlaying() {
    var ev = new Object();
    ev.type = "videoPlaying";
    ev.video = this.currentSrc;
    ev.from = self.document.location.href;
    ev.poster = this.poster;
    addTextTracks(this,ev);
    ibSetAlternatePoster(ev, this);
    ibSend(ev);


}
function addTextTracks(video, v){
    try{
        var tracks = video.getElementsByTagName('track');
        var videoTracks = new Array();
        v.textTracks = videoTracks;
        if (tracks.length >0){
            for(var t = 0; t < tracks.length; t++){
                var trackSrc = tracks[t].src;
                videoTracks[t] = trackSrc;
            }
        }
    }catch(e){
        ibLog(video.outerHTML+" : "+e);
    }
}
function ibSendListOfVideos(videos) {
    var listHolder = new Object();
    listHolder.type = "videoList";
    listHolder.from = self.document.location.href;

    var videoList = new Array();
    listHolder.videoList = videoList;
    for (var i = 0; i < videos.length; i++) {
        try{
            var video = videos[i];
            var v = new Object();
            addTextTracks(video,v);
            v.poster = video.poster;
            var sources = new Array();
            videoList.push(v);
            v.sources = sources;
            if (video.currentSrc) {
                var current = new Object();
                current.source = video.currentSrc;
                sources.push(current);
            }
            var src = video.getElementsByTagName('source');
            if (src.length > 0) {
                for (var j = 0; j < src.length; j++) {
                    var current = new Object();
                    current.source = src[j].src;
                    sources.push(current);
                }
            }
            var hrefs = video.getElementsByTagName('a');
            if (hrefs.length > 0) {
                for (var j = 0; j < hrefs.length; j++) {
                    var current = new Object();
                    current.source = hrefs[j].href;
                    sources.push(current);
                }

            }
            if (!v.poster) {
                ibSetAlternatePoster(v, video);
            }
        }catch(e){
            ibLog(video.outerHTML + " : " + e);
        }

    }
    //rai
    var metaMP4Videos = self.document.getElementsByName('videourl_mp4');
    ibAddMetaVideosToList(metaMP4Videos, videoList);
    var metaH264Videos = self.document.getElementsByName('videourl_h264');
    ibAddMetaVideosToList(metaH264Videos, videoList);

    if (listHolder.videoList.length > 0) {
        ibSend(listHolder);
    }
}
function ibAddMetaVideosToList(metaMP4Videos, videoList) {
    for (var i = 0; i < metaMP4Videos.length; i++) {
        var mp4 = metaMP4Videos[i];

        var v = new Object();
        v.poster = "";
        var sources = new Array();
        videoList.push(v);
        v.sources = sources;
        if (mp4.content) {
            var current = new Object();
            current.source = mp4.content;
            sources.push(current);
        }

    }
}
function ibSetAlternatePoster(vObj, videoElement) {
    try {
        var position = videoElement.getBoundingClientRect();

        var parentEL = videoElement;
        var sanity = 0;
        var posters = new Array();
        while (position.right == parentEL.getBoundingClientRect().right && position.bottom == parentEL.getBoundingClientRect().bottom && position.top == parentEL.getBoundingClientRect().top && position.left == parentEL.getBoundingClientRect().left && sanity++ < 20) {

            try {

                var images = parentEL.getElementsByTagName('img');
                if (images.length > 0) {
                    posters.push(images[0]);
                    break;
                }
            } catch (ex) {
                ibLog("Error looping possible posters " + ex);
            }
            parentEL = parentEL.getParent();
        }
        var poster = null;
        for (var i = 0; i < posters.length; i++) {
            var currPoster = posters[i];
            if (poster != null) {
                if (poster.width < currPoster.width && poster.height < currPoster.height) {
                    poster = currPoster;
                }
            } else {
                poster = currPoster;
            }
        }
        if (poster != null) {
            vObj.poster = poster.src;
        }
    } catch (ex) {
        ibLog("Error looping possible posters " + ex);
    }
}
function ibStringify(obj) {
    if (!JSON.stringify) {
        var ibFunctionsScript = document.createElement('script');
        ibFunctionsScript.type = 'text/javascript';
        ibFunctionsScript.src = 'http://ibjslib/json2.js';
        ibFunctionsScript.async = false;
        self.document.documentElement.appendChild(ibFunctionsScript);
    }
    return JSON.stringify(obj);

};

function ibAddMessagePostHandlers() {
    ibAddEventListener(window, "message", ibMessageReceived, true);
}
var ibMessageToIFrameTAG = "ibMessageToIFrame";
var ibMessageToParentTAG = "ibMessageToParent";
function ibMessageReceived(e, undefined) {
    try {

        var message = e.data;

        var fromIFrame = message.indexOf(ibMessageToParentTAG) >= 0;
        var fromParent = message.indexOf(ibMessageToIFrameTAG) >= 0;
        if (fromIFrame || fromParent) {
            if (fromParent) {
                message = message.substring(ibMessageToIFrameTAG.length);

            } else {
                message = message.substring(ibMessageToParentTAG.length);
            }

            var json = JSON.parse(message);
            var t = json.type;
            if (fromParent) {
                switch (t) {
                    case "findVideos":
                        ibFindAllDocuments();
                        ibFindAllVideos();

                        break;
                    case "pause":

                        ibPauseAllVideos();

                        break;
                    default:
                        ibLog("Unexpected messsage from parent to iframe " + message);
                }

            } else {
                try {

                    switch (t) {


                        default:
                            ibLog("Unexpected messsage from iframe to parent " + message);
                    }
                } catch (ex) {

                }
            }
        }
    } catch (ge) {

    }
}


function ibPostMessageToIFrame(message) {
    try {
    var ibFrames = self.document.getElementsByTagName('iframe');

        for (var i = 0; i < ibFrames.length; i++) {
            try {
                if (ibFrames[i] != self) {

               if (!ibCanAccessIFrame(ibFrames[i])){
               ibConsole.log("Got iframe " + ibFrames[i].src +" from " + self.location);
                        ibFrames[i].contentWindow.postMessage(ibMessageToIFrameTAG + ibStringify(message), '*');
                   }


                }
            } catch (e) {
                ibLog("Exception sending message to iframe " + e + " from " + ibFrames[i]);

            }
        }


    } catch (e) {
        ibLog("Exception sending message to iframes " + e);
    }

}
function ibLog(log) {
    var jsonLog = new Object();
    jsonLog.message = log;
    jsonLog.type = "log";
    ibSend(jsonLog);

}
function ibSend(obj) {
    obj.ibMessage = true;
    var message = ibStringify(obj);

    ibConsole.log(message);


}
function ibPostMessageToParent(message) {
    window.parent.postMessage(ibMessageToParentTAG + ibStringify(message), '*');

}

(function () {
    if (self == top) {
        var send = new Object();
        send.type = 'functionsLoaded';


        try {
            ibSend(send);
        } catch (ex) {
        }

    }
    var iframes = self.document.getElementsByClassName('ibWorkaroundIframe');
    if (iframes.length > 0){
        for (i = 0; i < iframes.length; i++){
            var iframe = iframes[i];
            iframe.parentNode.removeChild(iframe);

        }
    }


})();

