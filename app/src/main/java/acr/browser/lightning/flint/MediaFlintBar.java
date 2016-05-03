package acr.browser.lightning.flint;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import acr.browser.lightning.R;

public class MediaFlintBar extends RelativeLayout {
    private boolean mInflated;

    public MediaFlintBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void inflateContent() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View content = inflater.inflate(R.layout.flint_media_controller, this);

        mInflated = true;
    }

    public void show() {
        if (!mInflated) {
            inflateContent();
        }

        setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
    }

    public void onDestroy() {
    }

}
