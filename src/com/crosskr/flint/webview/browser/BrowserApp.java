package com.crosskr.flint.webview.browser;

import com.connectsdk.discovery.DiscoveryManager;

import android.app.Application;
import android.content.Context;

public class BrowserApp extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        
        DiscoveryManager.init(getApplicationContext());
    }

    public static Context getAppContext() {
        return context;
    }

    private String appname;
    private String videoId;
    private String videoName;
    private String type;

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getType() {
        return videoId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }
}
