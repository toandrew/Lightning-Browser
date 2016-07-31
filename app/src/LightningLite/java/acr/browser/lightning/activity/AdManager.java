package acr.browser.lightning.activity;

import android.app.Activity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdListener;

import android.widget.RelativeLayout;

import acr.browser.lightning.R;
import android.view.ViewGroup;

public class AdManager {
    private Activity mActivity;
    AdView mAdView;

    public AdManager(Activity activity) {
        mActivity = activity;

        ViewGroup contentView = (ViewGroup)activity.findViewById(R.id.ui_layout);
        mAdView = new AdView(activity);
        mAdView.setAdUnitId("ca-app-pub-7879734750226076/6792006747");
        mAdView.setAdSize(AdSize.BANNER);
        mAdView.setAdListener(new AdListener(){});
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        contentView.addView(mAdView, params);
    }

    public void onCreate() {
        try {
            if (mAdView != null) {
                mAdView.loadAd(new AdRequest.Builder().build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onResume() {
        try {
            if (mAdView != null) {
                mAdView.resume();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPause() {
        try {
            if (mAdView != null) {
                mAdView.pause();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDestroy() {
        try {
            if (mAdView != null) {
                mAdView.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
