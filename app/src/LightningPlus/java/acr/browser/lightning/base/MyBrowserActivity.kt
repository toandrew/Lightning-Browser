package acr.browser.lightning.base

import acr.browser.lightning.browser.activity.BrowserActivity
import android.os.Bundle

abstract class MyBrowserActivity : BrowserActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}