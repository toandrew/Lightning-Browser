package acr.browser.lightning.ad

import acr.browser.lightning.R
import android.app.Activity
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.google.android.gms.ads.*

class AdManager(activity : Activity) {
    private val adView : AdView? = AdView(activity)

    init {
        MobileAds.initialize(activity, "ca-app-pub-7879734750226076~3453529948")

        val contentView = activity.findViewById<ViewGroup>(R.id.ui_layout)

        adView?.adUnitId = "ca-app-pub-7879734750226076/6406996343"
        adView?.adSize = AdSize.BANNER
        adView?.adListener = AdListener()

        val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)

        contentView.addView(adView)
    }

    fun onCreate() {
        adView?.loadAd(AdRequest.Builder().build())
    }

    fun onResume() {
        adView?.resume()
    }

    fun onPause() {
        adView?.pause()
    }

    fun onDestroy() {
        adView?.destroy()
    }
}