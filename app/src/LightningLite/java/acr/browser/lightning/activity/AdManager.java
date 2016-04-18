package acr.browser.lightning.activity;

import android.app.Activity;

import com.mopub.mobileads.MoPubView;

import acr.browser.lightning.R;

public class AdManager {
    private Activity mActivity;
    MoPubView moPubView;

    public AdManager(Activity activity) {
        mActivity = activity;
        moPubView = (MoPubView) mActivity.findViewById(R.id.my_ad);
    }

    public void onCreate() {
        if (moPubView != null) {
            moPubView.setAdUnitId("e3dafa7b980e4a25b92de8d8983ce00e");
            moPubView.loadAd();
        }
    }

    public void onDestroy() {
        if (moPubView != null) {
            moPubView.destroy();
        }
    }
}
