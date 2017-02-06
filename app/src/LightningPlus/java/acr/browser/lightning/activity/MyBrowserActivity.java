package acr.browser.lightning.activity;

import android.os.Bundle;

import com.umeng.analytics.MobclickAgent;

/**
 * Created by jianmin on 17-2-6.
 */

public class MyBrowserActivity extends ThemableBrowserActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // UMENG
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onResume();

        // UMENG
        MobclickAgent.onPause(this);
    }
}
