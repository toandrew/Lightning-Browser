package acr.browser.lightning.flint;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.capability.MediaControl;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import acr.browser.lightning.R;
import acr.browser.lightning.browser.activity.BrowserActivity;
import acr.browser.lightning.view.LightningView;

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

    private static final int GET_VIDEO_URL_INTERVAL_MS = (int) TimeUnit.SECONDS.toMillis(3);

    private static final int REFRESH_INTERVAL_MS = (int) TimeUnit.SECONDS.toMillis(1);

    private static final String VIDEO_URL_PREFIX = "xxx:";

    private static final int HINT_SINGLE_ID = 0x123456;

    private MyHandler mHandler = null;

    private MediaFlintBar mMediaFlintBar;

    private ImageButton mMediaRouteButton;

    private ImageButton mPlayPauseButton;

    private ImageButton mClosePanelButton;

    private SeekBar mMediaSeekBar;

    private FlintVideoManager mFlintVideoManager;

    private int mPlayerState = PLAYER_STATE_NONE;

    private boolean mIsUserSeeking = false;

    private TextView mFlingCurrentTimeTextView;

    private TextView mFlingTotalTimeTextView;

    private TextView mFlingDeviceNameTextView;

    private Runnable mRefreshRunnable;

    private boolean mQuit = false;

    private String mCurrentUrl = null;

    private Context mContext;

    private String mCurrentVideoUrl;

    private String mCurrentVideoTitle;

    private String mSiteUrl;

    private Runnable mRefreshFlingRunnable;

    private Runnable mVideoUrlRunnable;

    /**
     * Use the followings to check whether input method is active!
     */
    boolean isKeyBoardOpened = false;

    private CheckBox mAutoplayCheckbox;

    private boolean mShouldAutoPlayMedia = true;

    private boolean mSeeking;

    private ShowcaseView mShowcaseView;

    private int mCounter = 0;

    private String mFetchedVideoUrl;

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

        }, 0);
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

        mQuit = true;

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
                .setImageResource(R.drawable.mediacontroller_route_off_button);

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
                        .setImageResource(R.drawable.mediacontroller_route_off_button);

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

    public Context getContext() {
        return mContext;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public void showFlintPanel() {
        if (mMediaFlintBar != null) {
            mMediaFlintBar.show();
        }
    }

    public void notifyGetVideoUrl(String url, boolean castForDownload) {
        if (((url != null && url.startsWith(VIDEO_URL_PREFIX))
                && url.length() > 4
                && (mFetchedVideoUrl == null || !mFetchedVideoUrl.equals(url
                .substring(4)))) || castForDownload) {
            Log.e(TAG, "Get valid video Url[" + url + "]fetched["
                    + mFetchedVideoUrl + "]");

            if (castForDownload) {
                mFetchedVideoUrl = url;
            } else {
                mFetchedVideoUrl = url.substring(4);
            }

            setCurrentVideoUrl(mFetchedVideoUrl);
            mHandler.postDelayed(mRefreshRunnable, 0);
        }
    }


    public boolean canControlVolume() {
        if (mMediaFlintBar != null
                && mMediaFlintBar.getVisibility() == View.VISIBLE
                && mFlintVideoManager.isDeviceConnected()) {
            return true;
        }

        return false;
    }

    /**
     * Called when volume changed.
     *
     * @param volumeIncrement
     */
    public void onVolumeChange(double volumeIncrement) {
        Log.e(TAG, "volumeIncrement:" + volumeIncrement);

        try {
            double v = mFlintVideoManager.getMediaVolume();

            Log.e("DLNA", "volumeIncrement:" + volumeIncrement + " v[" + v
                    + "]");
            v += volumeIncrement;
            if (v > 1.0) {
                v = 1.0;
            } else if (v < 0) {
                v = 0.0;
            }

            mFlintVideoManager.setMediaVolume(v);

        } catch (Exception e) {
            // showErrorDialog(e.getMessage());
        }
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
                switch (msg.what) {
                }
            }
        }
    }

    /**
     * Init all flint related
     */
    private void initFlint() {
        mMediaFlintBar = (MediaFlintBar) mActivity.findViewById(R.id.media_fling);

        // hide it when startup
        mMediaFlintBar.hide();

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

                    if (mMediaFlintBar.getVisibility() != View.VISIBLE) {
                        mMediaFlintBar.show();
                    }

                    if (getCurrentView() == null || mMediaFlintBar.getVisibility() != View.VISIBLE) {
                        return;
                    }

                    mCurrentUrl = getCurrentView().getUrl();

                    setCurrentVideoTitle(getCurrentView().getTitle());

                    Toast.makeText(mContext, getCurrentVideoUrl(),
                            Toast.LENGTH_SHORT).show();

                    autoPlayIfIsNecessary(getCurrentVideoUrl());
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

        // Use this thread to get video url!
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
                    String GET_VIDEO_URL_SCRIPT = "(" +
                            "function () " +
                            "{" +
                            "function canAccessIFrame(iFrame) {\n" +
                            "    var ihtml = null;\n" +
                            "    try {\n" +
                            "        var doc = iFrame.contentDocument || iFrame.contentWindow.document;\n" +
                            "        ihtml = doc.body.innerHTML;\n" +
                            "    } catch (err) {\n" +
                            "    }\n" +
                            "\n" +
                            "    return (ihtml !== null);\n" +
                            "}" +
                            "function isDocumentSomewhatReady(doc) {\n" +
                            "    if (document.readyState === 'complete' || document.readyState === 'interactive') {\n" +
                            "        return true;\n" +
                            "    } else {\n" +
                            "        return false;\n" +
                            "    }\n" +
                            "}" +
                            "function getFirstVideo(d) {\n" +
                            "  var videos = d.getElementsByTagName('video');\n" +
                            "  if (!videos|| !videos[0]) {\n" +
                            "    return null;\n" +
                            "  }\n" +
                            "  if (!!videos[0].src) {\n" +
                            "    return videos[0].src;\n" +
                            "  }\n" +
                            "  var videoSrc = videos[0].getElementsByTagName('source');\n" +
                            "  if (!!videoSrc && !!videoSrc[0]) {\n" +
                            "    return videoSrc[0].src;\n" +
                            "  }\n" +
                            "  \n" +
                            "  return null;\n" +
                            "}" +
                            "function fullscreen() {\n" +
                            "return document.fullscreen || \n" +
                            "    document.webkitIsFullScreen || \n" +
                            "    document.mozFullScreen || \n" +
                            "    false;\n" +
                            "}\n" +
                            "function findVideoInIFrames(d) {\n" +
                            "    var iFrames = d.getElementsByTagName('iframe');\n" +
                            "    for (var i = 0; i < iFrames.length; i++) {\n" +
                            "        try {\n" +
                            "            var iFrame = iFrames[i];\n" +
                            "            if (canAccessIFrame(iFrame)) {\n" +
                            "                var doc = iFrame.contentDocument;\n" +
                            "                if (isDocumentSomewhatReady(doc)) {\n" +
                            "                    return getFirstVideo(doc);\n" +
                            "                } else {\n" +
                            "                }\n" +
                            "            } else {\n" +
                            "                console.log(\"Unable to access iframe \" + iFrame.src );\n" +
                            "            }\n" +
                            "        } catch (e) {\n" +
                            "            console.log(e);\n" +
                            "        }\n" +
                            "    }\n" +
                            "    return null;\n" +
                            "}" +
                            "  var videos = document.getElementsByTagName('video'); " +
                            "  /*console.log('videos：' + videos.length);*/" +
                            "  if (!videos|| !videos[0]) { " +
                            "    var v = findVideoInIFrames(document);" +
                            "    if (!!v && !fullscreen()) {" +
                            "      alert('xxx:' + v); " +
                            "    }" +
                            "    return;" +
                            "  }" +
                            " /*console.log('haha：' + videos[0].src + ' currentSrc:' + videos[0].currentSrc);*/" +
                            "  if (!!videos[0].src && !fullscreen()) {" +
                            "    /*console.log('videos[0].src:' + videos[0].src);*/" +
                            "    alert('xxx:' + videos[0].src);" +
                            "  } else {" +
                            "    var videoSrc = videos[0].getElementsByTagName('source');" +
                            "    if (!!videoSrc && !!videoSrc[0] && !fullscreen()) {" +
                            "      /*console.log('videoSrc[0].src:' + videoSrc[0].src);*/" +
                            "      alert('xxx:' + videoSrc[0].src);" +
                            "    }" +
                            "  }" +
                            "}" +
                            ")();";
                    getCurrentView().getWebView().loadUrl(
                            "javascript:" + GET_VIDEO_URL_SCRIPT);
                }

                mHandler.removeCallbacks(mVideoUrlRunnable);

                mHandler.postDelayed(mVideoUrlRunnable, GET_VIDEO_URL_INTERVAL_MS);
            }
        };
        mHandler.postDelayed(mVideoUrlRunnable, GET_VIDEO_URL_INTERVAL_MS);

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

        mClosePanelButton = (ImageButton) mMediaFlintBar
                .findViewById(R.id.media_close_btn);
        mClosePanelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaFlintBar.hide();
            }
        });

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
                                            mClosePanelButton), true);
                                    mShowcaseView
                                            .setContentTitle(mContext.getString(R.string.flint_hint_close_btn_title));
                                    mShowcaseView
                                            .setContentText(mContext.getString(R.string.flint_hint_close_btn_details));
                                    break;

                                case 2:
                                    mShowcaseView.setTarget(Target.NONE);
                                    mShowcaseView
                                            .setContentTitle(mContext.getString(R.string.flint_hint_final_title));
                                    mShowcaseView
                                            .setContentText(mContext.getString(R.string.flint_hint_final_details));
                                    mShowcaseView
                                            .setButtonText(mContext.getString(R.string.flint_hint_close));
                                    setAlpha(0.4f, mMediaRouteButton, mClosePanelButton, getCurrentView().getWebView());
                                    break;

                                case 3:
                                    mShowcaseView.hide();

                                    // hide flint control bar.
                                    mMediaFlintBar.hide();

                                    setAlpha(1.0f, mMediaRouteButton, getCurrentView().getWebView());
                                    break;
                            }
                            mCounter++;
                        }

                    }).build();

            if (mShowcaseView.isShowing()) {
                showFlintPanel();
            }

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
     * stop timer to stop refresh current playback's UI.
     */
    private final void cancelRefreshTimer() {
        mHandler.removeCallbacks(mRefreshFlingRunnable);
    }
}
