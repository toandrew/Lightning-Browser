package acr.browser.lightning.app;

import android.app.Application;


import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import com.umeng.analytics.MobclickAgent;

import acr.browser.lightning.constant.MyConstants;

/**
 * Created by jianmin on 17-2-6.
 */

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics());

        MobclickAgent.startWithConfigure(new MobclickAgent.UMAnalyticsConfig(this.getApplicationContext(), MyConstants.UMENG_APP_KEY, MyConstants.UMENG_CHANNEL));
    }
}
