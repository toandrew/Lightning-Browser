package acr.browser.lightning.flint;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import acr.browser.lightning.R;
import acr.browser.lightning.activity.BrowserActivity;
import acr.browser.lightning.view.LightningView;

import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.capability.MediaControl;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.ApiUtils;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by jianmin on 16-4-25.
 */
public class CrossKrFlintManager implements FlintStatusChangeListener {
    private static final String TAG = "CrossKrFlintManager";

    private BrowserActivity mActivity;

    private static final String APPLICATION_ID = "~flintplayer";

    protected static final int PLAYER_STATE_NONE = 0;
    protected static final int PLAYER_STATE_PLAYING = 1;
    protected static final int PLAYER_STATE_PAUSED = 2;
    protected static final int PLAYER_STATE_BUFFERING = 3;
    protected static final int PLAYER_STATE_FINISHED = 4;

    private static final int REFRESH_INTERVAL_MS = (int) TimeUnit.SECONDS.toMillis(1);

    private static final String VIDEO_URL_PREFIX = "xxx:";

    private static final int HINT_SINGLE_ID = 0x123456;

    private MyHandler mHandler = null;

    private boolean mIsZh = true;

    private MediaFlintBar mMediaFlintBar;

    private ImageButton mMediaRouteButton;

    private ImageButton mPlayPauseButton;

    private ImageButton mVideoRefreshBtn;

    private SeekBar mMediaSeekBar;

    private FlintVideoManager mFlintVideoManager;

    private int mPlayerState = PLAYER_STATE_NONE;

    private boolean mIsUserSeeking = false;

    private TextView mFlingCurrentTimeTextView;

    private TextView mFlingTotalTimeTextView;

    private TextView mFlingDeviceNameTextView;

    private TextView mVideoResolutionTextView;

    private CustomDialog listDialog;

    private Runnable mRefreshRunnable;

    private boolean mQuit = false;

    private String mCurrentUrl = null;

    private Context mContext;

    private String mCurrentVideoUrl;

    private String mCurrentVideoTitle;

    private String mSiteUrl;

    private Runnable mRefreshFlingRunnable;

    private Runnable mGetVideoUrlRunnable;

    private Runnable mVideoUrlRunnable;

    /**
     * Use the followings to chech whether input method is active!
     */
    boolean isKeyBoardOpened = false;

    private CheckBox mHardwareDecoderCheckbox;

    private CheckBox mAutoplayCheckbox;

    private boolean mShouldAutoPlayMedia = true;

    private ProgressBar mVideoRefreshProgressBar;

    private boolean mSeeking;

    private ShowcaseView mShowcaseView;

    private int mCounter = 0;

    HttpsURLConnection httpsConn = null;

    private SSLSocketFactory mSSLSocketFactory;

    private X509HostnameVerifier mHostnameVerifier = null;

    private String mFetchedVideoUrl;

    Map<String, String> videoUrls = new HashMap<String, String>();

    ArrayList<String> videoList = new ArrayList<String>();

    private final ApiUtils apiUtils = new ApiUtils();

    public CrossKrFlintManager(Context context) {
        mContext = context;
    }

    public void onCreate(BrowserActivity activity) {
        mActivity = activity;

        mHandler = new MyHandler(mActivity);

        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                initFlint();
            }

        }, 1000);
    }

    public void onResume() {

    }

    public void onPause() {

    }

    public void onStart() {

    }

    public void onStop() {

    }

    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);

        mHandler = null;

        mQuit = true;

        if (mGetVideoUrlRunnable != null) {
            try {
                synchronized (mGetVideoUrlRunnable) {
                    mGetVideoUrlRunnable.notify();
                }
            } catch (Exception e) {

            }
        }
        if (mFlintVideoManager != null) {
            mFlintVideoManager.onStop();
        }
    }

    /**
     * Set current video url
     */
    public void setCurrentVideoUrl(String url) {
        mCurrentVideoUrl = url;
    }

    /**
     * Get current video url
     *
     * @return
     */
    public String getCurrentVideoUrl() {
        return mCurrentVideoUrl;
    }

    /**
     * Set current video title
     */
    public void setCurrentVideoTitle(String title) {
        mCurrentVideoTitle = title;
    }

    /**
     * Get current video title
     *
     * @return
     */
    public String getCurrentVideoTitle() {
        return mCurrentVideoTitle;
    }

    @Override
    public void onDeviceSelected(String name) {
        if (getCurrentVideoUrl() == null) {
            Log.d(TAG, "url is " + getCurrentVideoUrl() + " ignore it!");
            Toast.makeText(mContext,
                    mContext.getString(R.string.flint_empty_video_url),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        mMediaRouteButton
                .setImageResource(R.drawable.mr_ic_media_route_on_holo_dark);

        // ready to play media
        mFlintVideoManager.playVideo(getCurrentVideoUrl(),
                getCurrentVideoTitle());

        updateButtonStates();

        // show device info
        mFlingDeviceNameTextView.setText(name + "(Loading...)");
        updateFlingDispInfo(true);
    }

    @Override
    public void onDeviceUnselected() {
        // TODO Auto-generated method stub
        Log.e(TAG, "onDeviceUnselected!");

        mMediaRouteButton
                .setImageResource(R.drawable.mr_ic_media_route_off_holo_dark);

        cancelRefreshTimer();

        clearMediaState();
        updateButtonStates();
    }

    @Override
    public void onApplicationDisconnected() {
        clearMediaState();
        updateButtonStates();
    }

    @Override
    public void onConnectionFailed() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mMediaRouteButton
                        .setImageResource(R.drawable.mr_ic_media_route_off_holo_dark);

                updateButtonStates();
                clearMediaState();
                cancelRefreshTimer();
            }
        });
    }

    @Override
    public void onApplicationConnectionResult(String applicationStatus) {
        startRefreshTimer();
    }

    @Override
    public void onMediaSeekEnd() {
        mSeeking = false;
    }

    /**
     * Get current video's resolution name.
     *
     * @return
     */
    public String getCurrentResolution() {
        return mVideoResolutionTextView.getText().toString();
    }

    public Context getContext() {
        return mContext;
    }

    public Activity getActivity() {
        return mActivity;
    }

    private static class MyHandler extends Handler {
        private final WeakReference<BrowserActivity> mActivity;

        public MyHandler(BrowserActivity activity) {
            mActivity = new WeakReference(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            BrowserActivity activity = mActivity.get();
            if (activity != null) {
                switch(msg.what) {
                }
            }
        }
    }

    /**
     * Init all flint related
     */
    private void initFlint() {
        // get current system language.
        String lang = Locale.getDefault().getLanguage();
        if (lang.equals("zh")) {
            mIsZh = true;
        } else {
            mIsZh = false;
        }

        mMediaFlintBar = (MediaFlintBar) mActivity.findViewById(R.id.media_fling);
        mMediaFlintBar.show();

        mMediaRouteButton = (ImageButton) mMediaFlintBar
                .findViewById(R.id.media_route_button);

        mMediaRouteButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                mFlintVideoManager.doMediaRouteButtonClicked();
            }

        });

        mFlintVideoManager = new FlintVideoManager(this, APPLICATION_ID, this);

        mPlayPauseButton = (ImageButton) mMediaFlintBar
                .findViewById(R.id.mediacontroller_play_pause);
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerState == PLAYER_STATE_FINISHED) {
                    doPlay();
                } else if (mPlayerState == PLAYER_STATE_PAUSED
                        || mPlayerState == PLAYER_STATE_BUFFERING) {
                    onPlayClicked();
                } else if (mPlayerState == PLAYER_STATE_PLAYING) {
                    onPauseClicked();
                } else {
                    Log.e(TAG, "ignore for player state:" + mPlayerState);
                }
            }
        });

        mMediaSeekBar = (SeekBar) mMediaFlintBar
                .findViewById(R.id.mediacontroller_seekbar);
        mMediaSeekBar
                .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mIsUserSeeking = false;

                        mMediaSeekBar.setSecondaryProgress(0);
                        onSeekBarMoved(TimeUnit.SECONDS.toMillis(seekBar
                                .getProgress()));

                        refreshSeekPosition(TimeUnit.SECONDS.toMillis(seekBar
                                .getProgress()), mFlintVideoManager
                                .getMediaDuration());
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        mIsUserSeeking = true;

                        refreshSeekPosition(TimeUnit.SECONDS.toMillis(seekBar
                                .getProgress()), mFlintVideoManager
                                .getMediaDuration());

                        mMediaSeekBar.setSecondaryProgress(seekBar
                                .getProgress());
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {

                        refreshSeekPosition(TimeUnit.SECONDS.toMillis(seekBar
                                .getProgress()), mFlintVideoManager
                                .getMediaDuration());
                    }
                });

        mFlingCurrentTimeTextView = (TextView) mMediaFlintBar
                .findViewById(R.id.mediacontroller_time_current);
        mFlingTotalTimeTextView = (TextView) mMediaFlintBar
                .findViewById(R.id.mediacontroller_time_total);

        mFlingDeviceNameTextView = (TextView) mMediaFlintBar
                .findViewById(R.id.fling_device_name);

        mVideoResolutionTextView = (TextView) mMediaFlintBar
                .findViewById(R.id.resolution);

        mVideoResolutionTextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (listDialog != null) {
                    listDialog.dismiss();
                }
                Log.e(TAG, "onClick!");

                initListDialog();
            }

        });

        hideVideoResolutionView();

        setPlayerState(PLAYER_STATE_NONE);

        mRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (mQuit) {
                    Log.e(TAG, "mRefreshRunnable:quit!");
                    return;
                }

                Log.e(TAG, "show media cast control?!["
                        + DiscoveryManager.getInstance().getCompatibleDevices()
                        .size() + "]");

                if (DiscoveryManager.getInstance().getCompatibleDevices()
                        .size() > 0) {

                    if (getCurrentView() == null) {
                        return;
                    }

                    mCurrentUrl = getCurrentView().getUrl();

                    setCurrentVideoTitle(getCurrentView().getTitle());

                    Toast.makeText(mContext, getCurrentVideoUrl(),
                            Toast.LENGTH_SHORT).show();

                    mMediaFlintBar.show();

                    // hide
                    hideVideoResolutionView();

                    final String url = getCurrentView().getUrl();
                    if (!url.equals(mSiteUrl) && mIsZh) {
                        try {
                            synchronized (mGetVideoUrlRunnable) {
                                mGetVideoUrlRunnable.notify();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        autoPlayIfIsNecessary(getCurrentVideoUrl());
                    }
                }
            }

        };

        mRefreshFlingRunnable = new Runnable() {
            @Override
            public void run() {
                if (mQuit) {
                    Log.e(TAG, "mRefreshFlingRunnable:quit!");
                    return;
                }

                if (getCurrentView() != null) {
                    mCurrentUrl = getCurrentView().getUrl();
                }

                onRefreshEvent();

                startRefreshTimer();
            }
        };

        mGetVideoUrlRunnable = new Runnable() {
            @Override
            public void run() {
                while (!mQuit) {
                    try {
                        synchronized (this) {
                            Log.e(TAG, "mGetVideoUrlRunnable:wait!");
                            this.wait();
                            Log.e(TAG, "mGetVideoUrlRunnable:quit wait!!");
                        }

                        if (mQuit) {
                            Log.e(TAG, "mGetVideoUrlRunnable:quit!");
                            return;
                        }

                        final String url = mCurrentUrl;

                        getVideoPlayUrlByApi(url);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                Log.e(TAG, "mGetVideoUrlRunnable:quit!");
            }
        };

        new Thread(mGetVideoUrlRunnable).start();

        // use this thread to get video url!
        mVideoUrlRunnable = new Runnable() {
            @Override
            public void run() {
                if (mQuit) {
                    Log.e(TAG, "mVideoUrlRunnable:quit!");
                    return;
                }

                // Get Video's url.
                if (getCurrentView() != null && !isKeyBoardOpened) {
                    // Log.e(TAG, "try to extract real video url!");
                    String GET_VIDEO_URL_SCRIPT = "(function () {var videos = document.getElementsByTagName('video'); if (videos != null && videos[0] != null) {alert('xxx:' + videos[0].src);}})();";
                    getCurrentView().getWebView().loadUrl(
                            "javascript:" + GET_VIDEO_URL_SCRIPT);
                }

                mHandler.removeCallbacks(mVideoUrlRunnable);

                mHandler.postDelayed(mVideoUrlRunnable, REFRESH_INTERVAL_MS);
            }
        };
        mHandler.postDelayed(mVideoUrlRunnable, REFRESH_INTERVAL_MS);

        mHardwareDecoderCheckbox = (CheckBox) mMediaFlintBar
                .findViewById(R.id.device_hardware_decoder);
        mHardwareDecoderCheckbox
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        // TODO Auto-generated method stub

                        Log.e(TAG, "setHardwareDecoder:" + isChecked);
                    }

                });

        mAutoplayCheckbox = (CheckBox) mMediaFlintBar
                .findViewById(R.id.media_auto_play);
        mAutoplayCheckbox
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        // TODO Auto-generated method stub

                        Log.e(TAG, "auto play:" + isChecked);

                        mShouldAutoPlayMedia = isChecked;

                        if (mShouldAutoPlayMedia) {
                            doPlay();
                        }
                    }
                });

        mVideoRefreshBtn = (ImageButton) mMediaFlintBar
                .findViewById(R.id.media_get_video_url_btn);
        mVideoRefreshBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (getCurrentView() == null || !mIsZh) {
                    return;
                }

                updateGetVideoRealBtnStatus(false);

                mCurrentUrl = getCurrentView().getUrl();

                try {
                    synchronized (mGetVideoUrlRunnable) {
                        mGetVideoUrlRunnable.notify();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        mVideoRefreshProgressBar = (ProgressBar) mMediaFlintBar
                .findViewById(R.id.media_get_video_url_progressbar);

        mFlintVideoManager.onStart();
    }

    /**
     * directly play current video
     */
    private void doPlay() {
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                if (DiscoveryManager.getInstance().getCompatibleDevices()
                        .size() > 0) {
                    autoPlayIfIsNecessary(getCurrentVideoUrl());
                }

            }
        }, 50);
    }

    /**
     * PLAY
     */
    private void onPlayClicked() {
        mFlintVideoManager.playMedia();
    }

    /**
     * PAUSE
     */
    protected void onPauseClicked() {
        mFlintVideoManager.pauseMedia();
    }

    /**
     * SEEK
     *
     * @param position
     */
    protected void onSeekBarMoved(long position) {
        refreshPlaybackPosition(position, -1);

        mSeeking = true;

        mFlintVideoManager.seekMedia(position);
    }

    /**
     * refresh current time
     *
     * @param position
     * @param duration
     */
    protected final void refreshSeekPosition(long position, long duration) {
        mFlingCurrentTimeTextView.setText(formatTime(position));
    }

    /**
     * Show video's play url list
     */
    private void initListDialog() {
        listDialog = CustomDialog.createListDialog(mContext,
                new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                                            int arg2, long arg3) {
                        listDialog.dismiss();

                        // disable auto play
                        mAutoplayCheckbox.setChecked(false);
                        mShouldAutoPlayMedia = false;

                        setCurrentVideoUrl(videoUrls.get(videoList.get(arg2)));

                        mVideoResolutionTextView.setText(videoList.get(arg2));

                        if (mFlintVideoManager.isMediaConnected()) {
                            mFlintVideoManager.playVideo(
                                    videoUrls.get(videoList.get(arg2)),
                                    getCurrentVideoTitle());
                        }
                    }
                });
        if (mSiteUrl != null) {
            final String title = getCurrentView().getTitle();
            listDialog.setDialogTitile(title);
        } else {
            listDialog.setDialogTitile(mContext.getResources().getString(
                    R.string.custom_dialog_list_title_str));
        }

        videoList.clear();

        videoList.addAll(videoUrls.keySet());

        listDialog.setListData(videoList);
        listDialog.show();
    }

    /**
     * Hide video resolution view.
     */
    private void hideVideoResolutionView() {
        mVideoResolutionTextView.setText("");
        mVideoResolutionTextView.setVisibility(View.INVISIBLE);
    }

    /**
     * Set current player's status and update play/pause button status.
     *
     * @param playerState
     */
    protected final void setPlayerState(int playerState) {
        mPlayerState = playerState;
        if (mPlayerState == PLAYER_STATE_PAUSED) {
            mPlayPauseButton.setImageResource(R.drawable.mediacontroller_play);
        } else if (mPlayerState == PLAYER_STATE_PLAYING) {
            mPlayPauseButton.setImageResource(R.drawable.mediacontroller_pause);
        } else {
            mPlayPauseButton.setImageResource(R.drawable.mediacontroller_play);
        }

        mPlayPauseButton.setEnabled((mPlayerState == PLAYER_STATE_PAUSED)
                || (mPlayerState == PLAYER_STATE_PLAYING)
                || mPlayerState == PLAYER_STATE_FINISHED
                || mPlayerState == PLAYER_STATE_BUFFERING);
    }

    private LightningView getCurrentView() {
        return mActivity.getCurrentView();
    }

    /**
     * auto fling if necessary
     */
    private void autoPlayIfIsNecessary(String url) {
        if (!mShouldAutoPlayMedia || url == null) {
            return;
        }

        Log.e(TAG, "should show!");

        if (mFlintVideoManager.isMediaConnected()) {
            mFlintVideoManager.playVideo(url, getCurrentVideoTitle());
        }
    }

    /**
     * Called in UI timer thread to update current UI views.
     */
    protected void onRefreshEvent() {
        if (!mSeeking) {
            updatePlaybackPosition();
        }

        updateButtonStates();
    }

    /**
     * start timer to update current playback's UI.
     */
    private final void startRefreshTimer() {
        mHandler.removeCallbacks(mRefreshFlingRunnable);

        mHandler.postDelayed(mRefreshFlingRunnable, REFRESH_INTERVAL_MS);
    }

    /**
     * get video real play url by rabbit's api
     *
     * @param url
     */
    private void getVideoPlayUrlByApi(final String url) {
        Log.e(TAG, "getVideoPlayUrlByApi: " + url + "]mCurrentView["
                + getCurrentView() + "]");

        if (getCurrentView() == null) {
            return;
        }

        // TODO Auto-generated method stub
        List<NameValuePair> param = new ArrayList<NameValuePair>();

        param.add(new BasicNameValuePair("apptoken",
                "3e52201f5037ad9bd8e389348916bd3a"));
        param.add(new BasicNameValuePair("method", "core.video.realurl"));
        param.add(new BasicNameValuePair("packageName", "com.infthink.test"));
        param.add(new BasicNameValuePair("url", url));

        Log.e(TAG, "get real video url[" + url + "]site[" + mSiteUrl + "]");

        SendHttpsPOST("https://play.aituzi.com", param, null);
    }

    private void setListenerToRootView() {
        final View activityRootView = mActivity.getWindow().getDecorView().findViewById(
                R.id.content_frame);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        int heightDiff = activityRootView.getRootView()
                                .getHeight() - activityRootView.getHeight();
                        if (heightDiff > 600) { // 99% of the time the height
                            // diff will be due to a
                            // keyboard.
                            // Toast.makeText(getApplicationContext(),
                            // "Gotcha!!! softKeyboardup", 0).show();
                            Log.e(TAG, "keyboard is shown?![" + heightDiff
                                    + "]");
                            if (isKeyBoardOpened == false) {
                                // Do two things, make the view top visible and
                                // the editText smaller
                            }
                            isKeyBoardOpened = true;
                        } else if (isKeyBoardOpened == true) {
                            Log.e(TAG, "keyboard is hidden?![" + heightDiff
                                    + "]");

                            // Toast.makeText(getApplicationContext(),
                            // "softkeyborad Down!!!", 0).show();
                            isKeyBoardOpened = false;
                        }
                    }
                });
    }

    /**
     * show or hide views related with get video's real play url.
     *
     * @param show
     */
    private void updateGetVideoRealBtnStatus(boolean show) {
        if (!show) {
            mVideoRefreshBtn.setVisibility(View.GONE);
            mVideoRefreshProgressBar.setVisibility(View.VISIBLE);
        } else {
            mVideoRefreshBtn.setVisibility(View.VISIBLE);
            mVideoRefreshProgressBar.setVisibility(View.GONE);
        }

    }

    /**
     * Use this to show user some hints on UI about how to use Flint functions.
     */
    public void showHint(LightningView view) {
        if (mShowcaseView == null) {
            mShowcaseView = new ShowcaseView.Builder(mActivity, true)
                    .setTarget(new ViewTarget(view.getWebView()))
                    .setStyle(R.style.CustomShowcaseTheme2)
                    .singleShot(HINT_SINGLE_ID)
                    .setContentTitle(
                            mContext.getString(R.string.flint_hint_webview_title))
                    .setContentText(
                            mContext.getString(R.string.flint_hint_webview_details))
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // TODO Auto-generated method stub
                            Log.e(TAG, "showHint:mCounter" + mCounter);
                            switch (mCounter) {
                                case 0:
                                    mShowcaseView.setShowcase(new ViewTarget(
                                            mMediaRouteButton), true);
                                    mShowcaseView
                                            .setContentTitle(mContext.getString(R.string.flint_hint_control_title));
                                    mShowcaseView
                                            .setContentText(mContext.getString(R.string.flint_hint_control_details));
                                    break;

                                case 1:
                                    mShowcaseView.setShowcase(new ViewTarget(
                                            mVideoRefreshBtn), true);
                                    mShowcaseView
                                            .setContentTitle(mContext.getString(R.string.flint_hint_video_quality_title));
                                    mShowcaseView
                                            .setContentText(mContext.getString(R.string.flint_hint_video_quality_details));
                                    break;

                                case 2:
                                    mShowcaseView.setTarget(Target.NONE);
                                    mShowcaseView
                                            .setContentTitle(mContext.getString(R.string.flint_hint_final_title));
                                    mShowcaseView
                                            .setContentText(mContext.getString(R.string.flint_hint_final_details));
                                    mShowcaseView
                                            .setButtonText(mContext.getString(R.string.flint_hint_close));
                                    setAlpha(0.4f, mMediaRouteButton,
                                            mVideoRefreshBtn,
                                            getCurrentView().getWebView());
                                    break;

                                case 3:
                                    mShowcaseView.hide();
                                    setAlpha(1.0f, mMediaRouteButton,
                                            mVideoRefreshBtn,
                                            getCurrentView().getWebView());
                                    break;
                            }
                            mCounter++;
                        }

                    }).build();
        }
        mShowcaseView.setButtonText(mContext.getString(R.string.flint_hint_next));
        mShowcaseView.setShouldCentreText(false);
    }

    /**
     * Set current playback's position
     *
     * @param position
     * @param duration
     */
    protected final void refreshPlaybackPosition(long position, long duration) {
        if (!mIsUserSeeking) {
            if (position == 0) {
                mFlingTotalTimeTextView.setText("N/A");
                mMediaSeekBar.setProgress(0);
            } else if (position > 0) {
                mMediaSeekBar.setProgress((int) TimeUnit.MILLISECONDS
                        .toSeconds(position));
            }
            mFlingCurrentTimeTextView.setText(formatTime(position));
        }

        if (duration == 0) {
            mMediaSeekBar.setProgress(0);
            mFlingTotalTimeTextView.setText("N/A");
            mMediaSeekBar.setMax(0);
        } else if (duration > 0) {
            mFlingTotalTimeTextView.setText(formatTime(duration));
            if (!mIsUserSeeking) {
                mMediaSeekBar.setMax((int) TimeUnit.MILLISECONDS
                        .toSeconds(duration));
            }
        }
    }

    /**
     * update current playback's position
     */
    private void updatePlaybackPosition() {
        refreshPlaybackPosition(mFlintVideoManager.getMediaCurrentTime(),
                mFlintVideoManager.getMediaDuration());
    }

    /**
     * Update all views according to current application status.
     */
    private void updateButtonStates() {
        boolean hasMediaConnection = mFlintVideoManager.isMediaConnected();

        if (hasMediaConnection) {
            mFlingDeviceNameTextView.setText(mFlintVideoManager
                    .getCurrentSelectDeviceName());

            MediaControl.PlayStateStatus mediaStatus = mFlintVideoManager.getMediaStatus();
            Log.e(TAG, "mediaStatus:" + mediaStatus);
            if (mediaStatus != null) {
                int playerState = PLAYER_STATE_NONE;
                if (mediaStatus == MediaControl.PlayStateStatus.Paused) {
                    playerState = PLAYER_STATE_PAUSED;
                } else if (mediaStatus == MediaControl.PlayStateStatus.Playing) {
                    playerState = PLAYER_STATE_PLAYING;
                } else if (mediaStatus == MediaControl.PlayStateStatus.Buffering) {
                    mFlingDeviceNameTextView.setText(mFlintVideoManager
                            .getCurrentSelectDeviceName() + "(Buffering...)");
                    playerState = PLAYER_STATE_BUFFERING;
                } else if (mediaStatus == MediaControl.PlayStateStatus.Finished) {
                    Log.e(TAG, "PlayStateStatus.Finished");
                    playerState = PLAYER_STATE_FINISHED;

                    mSeeking = false;

                    refreshPlaybackPosition(0,
                            mFlintVideoManager.getMediaDuration());
                }
                setPlayerState(playerState);

                updateFlingDispInfo(true);

                setSeekBarEnabled(playerState != PLAYER_STATE_FINISHED
                        && playerState != PLAYER_STATE_NONE);
            }
        } else {
            setPlayerState(PLAYER_STATE_NONE);

            updateFlingDispInfo(false);

            setSeekBarEnabled(false);

            clearMediaState();
        }
    }

    /**
     * Get time by string format.
     *
     * @param millisec
     * @return
     */
    private String formatTime(long millisec) {
        int seconds = (int) (millisec / 1000);
        int hours = seconds / (60 * 60);
        seconds %= (60 * 60);
        int minutes = seconds / 60;
        seconds %= 60;

        String time;
        if (hours > 0) {
            time = String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            time = String.format("%d:%02d", minutes, seconds);
        }
        return time;
    }

    /**
     * Send POST request.
     *
     * @param url
     * @param param
     * @param data
     * @return
     */
    public String SendHttpsPOST(String url, List<NameValuePair> param,
                                String data) {
        String result = null;
        Log.e(TAG, "SendHttpsPOST!");

        // 使用此工具可以将键值对编码成"Key=Value&amp;Key2=Value2&amp;Key3=Value3&rdquo;形式的请求参数
        String requestParam = URLEncodedUtils.format(param, "UTF-8");

        try {
            URL requestUrl = new URL(url);
            httpsConn = (HttpsURLConnection) requestUrl.openConnection();

            httpsConn.setSSLSocketFactory(mSSLSocketFactory);
            httpsConn.setHostnameVerifier(mHostnameVerifier);

            // POST
            httpsConn.setRequestMethod("POST");

            httpsConn.setConnectTimeout(5000);
            httpsConn.setDoOutput(true);
            httpsConn.setDoInput(true);
            httpsConn.setUseCaches(false);
            httpsConn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            // send the POST request to server
            OutputStream outputStream = null;
            try {
                outputStream = httpsConn.getOutputStream();
                outputStream.write(requestParam.toString().getBytes("utf-8"));
                outputStream.flush();
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }

            int code = httpsConn.getResponseCode();
            if (HttpsURLConnection.HTTP_OK == code) {

                // 获取输入流
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        httpsConn.getInputStream()));

                String temp = in.readLine();
                /* 连接成一个字符串 */
                while (temp != null) {
                    if (result != null)
                        result += temp;
                    else
                        result = temp;
                    temp = in.readLine();
                }
                in.close();

                Log.e(TAG, "SendHttpsPOST:response[" + result + "]");

                // ready to processs video urls!
                processVideoUrls(result);
            }

            Log.e(TAG, "SendHttpsPOST![" + code + "]");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpsConn != null) {
                httpsConn.disconnect();
                httpsConn = null;
            }
        }

        return result;
    }

    /**
     * set alpha
     *
     * @param alpha
     * @param views
     */
    private void setAlpha(float alpha, View... views) {
        if (apiUtils.isCompatWithHoneycomb()) {
            for (View view : views) {
                view.setAlpha(alpha);
            }
        }
    }

    /**
     * show or hide device name or other views when device connected.
     *
     * @param show
     */
    private void updateFlingDispInfo(boolean show) {
        if (show) {
            mFlingDeviceNameTextView.setVisibility(View.VISIBLE);
        } else {
            mFlingDeviceNameTextView.setVisibility(View.GONE);
            mFlingDeviceNameTextView.setText("");
        }
    }

    /**
     * whether enable seek bar.
     *
     * @param enabled
     */
    protected final void setSeekBarEnabled(boolean enabled) {
        mMediaSeekBar.setEnabled(enabled);
    }

    /**
     * clear media status when application disconnected.
     */
    private void clearMediaState() {
        mSeeking = false;

        refreshPlaybackPosition(0, 0);
    }

    /**
     * Get video url list
     *
     * @param result
     */
    private void processVideoUrls(String result) {
        videoUrls.clear();

        // {"status":200,"data":[{"url":"http:\/\/v.youku.com\/v_show\/id_XODUwMDM0NDUy.html?from=y1.3-tv-grid-1007-9910.86804.1-1","id":"","num":"","vid":"","source":"youku","sourceName":"\u4f18\u9177\u89c6\u9891","playUrl":{"HD":["http:\/\/pl.youku.com\/playlist\/m3u8?ts=1427076444&keyframe=1&vid=XODUwMDM0NDUy&type=hd2&sid=442707644496121c78ca6&token=5158&oip=1008521675&ep=v4TS4z2PnxtMZfqTd5f%2FdgrHMEbE4Lhvk9YdQoGTJsv7lbbElD2WtWp9mT7DI5SF&did=3f2189e6a744e68de6761a20ceaf379aa8acfad4&ctype=21&ev=1"],"SD":["http:\/\/pl.youku.com\/playlist\/m3u8?ts=1427076444&keyframe=1&vid=XODUwMDM0NDUy&type=mp4&sid=442707644496121c78ca6&token=5158&oip=1008521675&ep=v4TS4z2PnxtMZfqTd5f%2FdgrHMEbE4Lhvk9YdQoGTJsv7lbbElD2WtWp9mT7DI5SF&did=3f2189e6a744e68de6761a20ceaf379aa8acfad4&ctype=21&ev=1"]}}]}
        try {
            JSONObject obj = new JSONObject(result);
            String status = obj.getString("status");

            if ("200".equals(status)) {
                JSONArray data = obj.getJSONArray("data");

                JSONObject r = data.getJSONObject(0);
                final String url = r.getString("url");
                JSONObject playUrl = r.getJSONObject("playUrl");

                String label = "";

                try {
                    JSONArray Smooth = playUrl.getJSONArray("Smooth");
                    videoUrls.put(mContext.getString(R.string.resolution_Smooth),
                            Smooth.getString(0));
                    label = mContext.getString(R.string.resolution_Smooth);
                } catch (Exception e) {
                    // e.printStackTrace();
                }

                try {
                    JSONArray SD = playUrl.getJSONArray("SD");
                    videoUrls.put(mContext.getString(R.string.resolution_SD),
                            SD.getString(0));
                    label = mContext.getString(R.string.resolution_SD);
                } catch (Exception e) {
                    // e.printStackTrace();
                }

                try {
                    JSONArray HD = playUrl.getJSONArray("HD");
                    videoUrls.put(mContext.getString(R.string.resolution_HD),
                            HD.getString(0));

                    label = mContext.getString(R.string.resolution_HD);
                } catch (Exception e) {
                    // e.printStackTrace();
                }

                try {
                    JSONArray Ultraclear = playUrl.getJSONArray("Ultraclear");
                    videoUrls.put(mContext.getString(R.string.resolution_Ultraclear),
                            Ultraclear.getString(0));

                    label = mContext.getString(R.string.resolution_Ultraclear);
                } catch (Exception e) {
                    // e.printStackTrace();
                }

                try {
                    JSONArray Bluray = playUrl.getJSONArray("Bluray");
                    videoUrls.put(mContext.getString(R.string.resolution_Bluray),
                            Bluray.getString(0));

                    label = mContext.getString(R.string.resolution_Bluray);
                } catch (Exception e) {
                    // e.printStackTrace();
                }

                Log.e(TAG,
                        "playUrl:" + playUrl.toString() + "["
                                + videoUrls.toString() + "]");

                final String videoQualityLabel = label;

                if (videoUrls.size() > 0) {
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub

                            mSiteUrl = url;

                            if (listDialog != null) {
                                listDialog.setDialogTitile(url);
                            }

                            videoList.clear();

                            videoList.addAll(videoUrls.keySet());

                            mMediaFlintBar.show();

                            mVideoResolutionTextView.setText(videoQualityLabel);

                            setCurrentVideoUrl(videoUrls.get(videoQualityLabel));

                            mVideoResolutionTextView
                                    .setVisibility(View.VISIBLE);

                            updateGetVideoRealBtnStatus(true);

                            autoPlayIfIsNecessary(videoUrls
                                    .get(videoQualityLabel));
                        }

                    });
                } else {
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub

                            hideVideoResolutionView();

                            updateGetVideoRealBtnStatus(true);

                            autoPlayIfIsNecessary(getCurrentVideoUrl());
                        }

                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub

                    hideVideoResolutionView();

                    updateGetVideoRealBtnStatus(true);

                    autoPlayIfIsNecessary(getCurrentVideoUrl());
                }

            });
        }
    }

    /**
     * stop timer to stop refresh current playback's UI.
     */
    private final void cancelRefreshTimer() {
        mHandler.removeCallbacks(mRefreshFlingRunnable);
    }

    public void notifyGetVideoUrl(String url) {
        if ((url != null && url.startsWith(VIDEO_URL_PREFIX))
                && url.length() > 4
                && (mFetchedVideoUrl == null || !mFetchedVideoUrl.equals(url
                .substring(4)))) {
            Log.e(TAG, "Get valid video Url[" + url + "]fetched["
                    + mFetchedVideoUrl + "]");

            mFetchedVideoUrl = url.substring(4);

            setCurrentVideoUrl(mFetchedVideoUrl);
            mHandler.postDelayed(mRefreshRunnable, 0);
        }
    }

}
