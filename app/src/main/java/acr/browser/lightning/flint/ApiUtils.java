package acr.browser.lightning.flint;

import android.os.Build;

public class ApiUtils {

    public boolean isCompatWith(int versionCode) {
        return Build.VERSION.SDK_INT >= versionCode;
    }

    public boolean isCompatWithHoneycomb() {
        return isCompatWith(Build.VERSION_CODES.HONEYCOMB);
    }

}
