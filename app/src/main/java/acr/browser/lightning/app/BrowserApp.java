package acr.browser.lightning.app;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.webkit.WebView;

import com.crashlytics.android.Crashlytics;
//import com.squareup.leakcanary.LeakCanary;
import com.squareup.otto.Bus;

import io.fabric.sdk.android.Fabric;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import acr.browser.lightning.BuildConfig;
import acr.browser.lightning.preference.PreferenceManager;

public class BrowserApp extends Application {

    private static AppComponent mAppComponent;
    private static final Executor mIOThread = Executors.newSingleThreadExecutor();
    private static final Executor mTaskThread = Executors.newCachedThreadPool();

    @Inject Bus mBus;
    @Inject PreferenceManager mPreferenceManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        mAppComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
        mAppComponent.inject(this);

        if (mPreferenceManager.getUseLeakCanary() && !isRelease()) {
            //LeakCanary.install(this);
        }
        if (!isRelease() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    @NonNull
    public static BrowserApp get(@NonNull Context context) {
        return (BrowserApp) context.getApplicationContext();
    }

    public static AppComponent getAppComponent() {
        return mAppComponent;
    }

    @NonNull
    public static Executor getIOThread() {
        return mIOThread;
    }

    @NonNull
    public static Executor getTaskThread() {
        return mTaskThread;
    }

    public static Bus getBus(@NonNull Context context) {
        return get(context).mBus;
    }

    /**
     * Determines whether this is a release build.
     *
     * @return true if this is a release build, false otherwise.
     */
    public static boolean isRelease() {
        return !BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.toLowerCase().equals("release");
    }

}
