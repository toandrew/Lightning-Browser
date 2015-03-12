package com.infthink.miuicastsender;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import tv.matchstick.flint.ApplicationMetadata;
import tv.matchstick.flint.ConnectionResult;
import tv.matchstick.flint.Flint;
import tv.matchstick.flint.Flint.ApplicationConnectionResult;
import tv.matchstick.flint.FlintDevice;
import tv.matchstick.flint.FlintManager;
import tv.matchstick.flint.FlintMediaControlIntent;
import tv.matchstick.flint.MediaInfo;
import tv.matchstick.flint.MediaMetadata;
import tv.matchstick.flint.MediaStatus;
import tv.matchstick.flint.RemoteMediaPlayer;
import tv.matchstick.flint.RemoteMediaPlayer.MediaChannelResult;
import tv.matchstick.flint.ResultCallback;
import tv.matchstick.flint.Status;

import com.nanohttpd.webserver.src.main.java.fi.iki.elonen.SimpleWebServer;

import acr.browser.lightning.BrowserApp;
import acr.browser.lightning.FlintMsgChannel;
import acr.browser.lightning.R;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class SenderDemo extends ActionBarActivity {
    private final String TAG = "SenderDemo";
    private final static String ACTION_DUOKAN_VIDEOPLAY = "duokan.intent.action.VIDEO_PLAY";
    private static final int REFRESH_INTERVAL_MS = (int) TimeUnit.SECONDS
            .toMillis(1);
    private static final int EXIT_PLAY_MENU_ID = 1;
    private static final int PLAYER_STATE_NONE = 0;
    private static final int PLAYER_STATE_PLAYING = 1;
    private static final int PLAYER_STATE_PAUSED = 2;
    private static final int PLAYER_STATE_BUFFERING = 3;
    private static final int PLAYER_STATE_FINISHED = 4;
    
    private int mPlayerState = PLAYER_STATE_NONE;

    private ImageButton mBtnPlay;
    private ImageButton mBtnVolumeUp;
    private ImageButton mBtnVolumeDown;
    private TextView mTextViewName;
    private TextView mTextViewDescription;
    private TextView mTextViewCurtime;
    private TextView mTextViewDuration;
    private ImageView mImageVideo;

    private BrowserApp mApplication;
    private SeekBar mSeekbar;
    private Handler mHandler = new Handler();
    private Intent mIntent = null;
    private AlertDialog mAlertDialog = null;

    private Animation mAnimation = null;
    LinearInterpolator mLin = new LinearInterpolator();

    private String mVideoId = "";

    private NotificationManager mNotificationManager;
    private String mIpAddress;

    private SimpleWebServer mNanoHTTPD;
    private int port = 8080;
    private String mRootDir = "/";

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private ConnectionCallbacks mConnectionCallbacks;
    private boolean mResumePlay;
    private boolean mIsUserSeeking;

    private boolean mSeeking;
    private boolean mFirst = true;
    private FlintDevice mSelectedDevice;
    private RemoteMediaPlayer mMediaPlayer;
    private FlintManager mApiClient;
    private CastListener mCastListener;
    private ApplicationMetadata mAppMetadata;
    private Runnable mRefreshRunnable;
    private boolean mWaitingForReconnect;

    private Uri mUri;
    private String mTitle;

    // used to send cust messages.
    private FlintMsgChannel mFlintMsgChannel;

    private CheckBox mHardwareDecoderCheckbox;

    private boolean mIsHardwareDecoder = true;

    private String processLocalVideoUrl(String url) {
        String real_url = url;
        if (url != null && url.startsWith("file://")) {

            initWebserver();
            // remove "file://"
            real_url = url.replaceAll("file://", "");
            return "http://" + mIpAddress + ":8080" + real_url;

        }
        return url;
    }

    private String mApplicationId = "~flintplayer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getActionBar().setBackgroundDrawable(null);

        NotificationManager notificationManager = (NotificationManager) this
                .getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);

        setContentView(R.layout.video_details);
        Flint.FlintApi.setApplicationId(mApplicationId);

        mApplication = (BrowserApp) this.getApplicationContext();
        mMediaRouter = MediaRouter.getInstance(this);
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(
                        FlintMediaControlIntent
                                .categoryForFlint(mApplicationId)).build();

        mMediaRouterCallback = new MyMediaRouterCallback();
        mCastListener = new CastListener();
        mConnectionCallbacks = new ConnectionCallbacks();

        Intent intent = getIntent();
        Log.e(TAG, "intent");
        if (intent != null
                && intent.getAction() != null
                && (intent.getAction().equals(ACTION_DUOKAN_VIDEOPLAY) || intent
                        .getAction().equals(Intent.ACTION_VIEW))) {
            Uri videoURI = intent.getData();
            mVideoId = processLocalVideoUrl(videoURI.toString());
            String curId = mApplication.getVideoId();
            if (mVideoId.equals(curId)) {
                mResumePlay = true;
            }
            android.util.Log.d(TAG, "videoURI = " + mVideoId);
            android.util.Log.d(TAG, "curId = " + curId);

            StringBuffer sb = new StringBuffer();
            String mediaTitle = intent.getStringExtra("mediaTitle");
            sb.append(mediaTitle);
            int availableEpisodeCount = intent.getIntExtra(
                    "available_episode_count", 0);
            if (availableEpisodeCount > 0) {
                int currentEpisode = intent.getIntExtra("current_episode", 1);
                sb.append("第 " + currentEpisode + " 集");
            }
            String name = sb.toString();
            intent.putExtra("vname", name);
            intent.putExtra("type", "net");
            intent.putExtra("url", mVideoId);
        } else {
            mResumePlay = true;
        }

        // if (mApplication.getCastDevice() != null) {
        // mVideoId = intent.getData().toString();
        // this.setSelectedDevice(mApplication.getCastDevice());
        // }

        initView();

        mRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                onRefreshEvent();
                startRefreshTimer();
            }
        };

        updateCastBtnState();

        mTextViewName.setText(mIntent.getStringExtra("vname"));
        mTextViewDescription.setText(mIntent.getStringExtra("vdes"));
        // imgvideo.setImageResource(mIntent.getExtras().getInt("vicon"));
        mImageVideo.setVisibility(View.GONE);
        mBtnPlay.setClickable(false);
        mBtnPlay.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mFirst) {
                    sendURL(mVideoId, mIntent.getStringExtra("vname"));
                } else {
                    if (mMediaPlayer == null
                            || mMediaPlayer.getMediaStatus() == null
                            || mApiClient == null || !mApiClient.isConnected())
                        return;
                    
                    if (mPlayerState == PLAYER_STATE_PAUSED || mPlayerState == PLAYER_STATE_BUFFERING) {
                        mMediaPlayer.play(mApiClient).setResultCallback(
                                new MediaResultCallback("play"));
                    } else if (mPlayerState == PLAYER_STATE_FINISHED) {
                        sendURL(mVideoId, mIntent.getStringExtra("vname"));
                    } else if (mPlayerState == PLAYER_STATE_PLAYING) {
                        mMediaPlayer.pause(mApiClient).setResultCallback(
                                new MediaResultCallback("pause"));
                    } else {
                        Toast.makeText(SenderDemo.this, "ignore!unknown state![" + mPlayerState + "]", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        mBtnVolumeUp.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                setVolumeUp();
            }

        });

        mBtnVolumeDown.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                setVolumeDown();
            }

        });

        mSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsUserSeeking = false;
                mSeekbar.setSecondaryProgress(0);
                onSeekBarMoved(TimeUnit.SECONDS.toMillis(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsUserSeeking = true;
                mSeekbar.setSecondaryProgress(seekBar.getProgress());
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                if (fromUser) {
                    mTextViewCurtime.setText(formatTime(progress * 1000));
                }
            }
        });

        mFlintMsgChannel = new FlintMsgChannel() {
            @Override
            public void onMessageReceived(FlintDevice flingDevice,
                    String namespace, final String message) {
                super.onMessageReceived(flingDevice, namespace, message);

                // show received custom messages.
                Log.d(TAG, "onMessageReceived: " + message);

                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            JSONObject obj = new JSONObject(message);
                            mIsHardwareDecoder = obj
                                    .getBoolean("isHardwareDecoder");

                            mHardwareDecoderCheckbox
                                    .setChecked(mIsHardwareDecoder);

                            mHardwareDecoderCheckbox
                                    .setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        };

        mHardwareDecoderCheckbox = (CheckBox) findViewById(R.id.device_hardware_decoder);
        mHardwareDecoderCheckbox.setVisibility(View.GONE);
        mHardwareDecoderCheckbox
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        // TODO Auto-generated method stub
                        if (mApiClient != null) {
                            Log.e(TAG, "setHardwareDecoder:" + isChecked);
                            setHardwareDecoder(isChecked);
                        }
                    }

                });
    }

    /**
     * Set custom message to device. let device use hardware decoder or not
     * 
     * @param flag
     */
    private void setHardwareDecoder(boolean flag) {
        if (mApiClient == null || !mApiClient.isConnected()) {
            return;
        }

        mFlintMsgChannel.setHardwareDecoder(mApiClient, flag);
    }

    private void onSeekBarMoved(long position) {
        if (mMediaPlayer == null) {
            return;
        }

        refreshPlaybackPosition(position, -1);

        mSeeking = true;
        mMediaPlayer.seek(mApiClient, position,
                RemoteMediaPlayer.RESUME_STATE_PLAY).setResultCallback(
                new MediaResultCallback("seek") {
                    @Override
                    protected void onFinished() {
                        mSeeking = false;
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void setVolumeUp() {
        if (mMediaPlayer == null) {
            return;
        }
        try {
            mMediaPlayer
                    .setStreamVolume(
                            mApiClient,
                            Math.min(mMediaPlayer.getMediaStatus()
                                    .getStreamVolume() + 0.1, 1.0))
                    .setResultCallback(new MediaResultCallback("volume"));
        } catch (IllegalStateException e) {
            // showErrorDialog(e.getMessage());
        }
    }

    private void onRefreshEvent() {
        if (!mSeeking) {
            updatePlaybackPosition();
        }
        updateButtonStates();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            setVolumeDown();
            return true;
        case KeyEvent.KEYCODE_VOLUME_UP:
            setVolumeUp();
            return true;
        default:
            break;
        }
        return super.dispatchKeyEvent(event);
    }

    private void setVolumeDown() {
        if (mMediaPlayer == null) {
            return;
        }
        try {
            mMediaPlayer
                    .setStreamVolume(
                            mApiClient,
                            Math.max(mMediaPlayer.getMediaStatus()
                                    .getStreamVolume() - 0.1, 0.0))
                    .setResultCallback(new MediaResultCallback("volume"));
        } catch (IllegalStateException e) {
            // showErrorDialog(e.getMessage());
        }
    }

    private void initWebserver() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        startServer(8080);
    }

    private void initView() {

        mIntent = getIntent();

        // btncast = (ImageButton) findViewById(R.id.btncast);
        mBtnPlay = (ImageButton) findViewById(R.id.btnplay);
        mBtnVolumeUp = (ImageButton) findViewById(R.id.btnpre);
        mBtnVolumeDown = (ImageButton) findViewById(R.id.btnnext);
        mTextViewName = (TextView) findViewById(R.id.tvname);
        // tvdescription = (TextView) findViewById(R.id.tvdescription);
        mTextViewDescription = new TextView(this);
        // imgvideo = (ImageView) findViewById(R.id.imgvideo);
        mImageVideo = new ImageView(this);
        mSeekbar = (SeekBar) findViewById(R.id.videoprogress);
        mTextViewCurtime = (TextView) findViewById(R.id.tvcurtime);
        mTextViewDuration = (TextView) findViewById(R.id.tvduration);
        mAnimation = AnimationUtils.loadAnimation(this, R.anim.play_anim);
        mAnimation.setInterpolator(mLin);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_fling, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat
                .getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        menu.add(0, EXIT_PLAY_MENU_ID, 0, "退出播放");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case EXIT_PLAY_MENU_ID:
            if (mApiClient != null && mApiClient.isConnected()) {
                Flint.FlintApi.stopApplication(mApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status result) {
                                if (result.isSuccess()) {
                                    mAppMetadata = null;
                                    refreshPlaybackPosition(0, 0);
                                    detachMediaPlayer();
                                    updateButtonStates();
                                } else {
                                    // showErrorDialog(getString(R.string.error_app_stop_failed));
                                }
                            }
                        });
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateCastBtnState() {

    }

    private void alert(String msg) {
        Toast.makeText(SenderDemo.this, msg, Toast.LENGTH_SHORT).show();
    }

    // private void showWarning(String msg) {
    // if (mAlertDialog == null) {
    // mAlertDialog = new AlertDialog.Builder(SenderDemo.this)
    // .setTitle("Warning").setMessage(msg)
    // .setPositiveButton("OK", null).create();
    // }
    // mAlertDialog.show();
    // }

    private void sendURL(String url, String name) {
        android.util.Log.d(TAG, "url = " + url);
        android.util.Log.d(TAG, "videoname = " + name);
        if (mMediaPlayer == null)
            return;
        
        Toast.makeText(SenderDemo.this, url,
                Toast.LENGTH_SHORT).show();
        
        MediaMetadata metadata = new MediaMetadata(
                MediaMetadata.MEDIA_TYPE_MOVIE);
        metadata.putString(MediaMetadata.KEY_TITLE, name);

        MediaInfo mediaInfo = new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("video/mp4").setMetadata(metadata).build();
        mMediaPlayer.load(mApiClient, mediaInfo, true).setResultCallback(
                new MediaResultCallback("load"));
        mFirst = false;
        mBtnPlay.setEnabled(false);
    }

    private String formatTime(long millisec) {
        int seconds = (int) (millisec / 1000);
        int hours = seconds / (60 * 60);
        seconds %= (60 * 60);
        int minutes = seconds / 60;
        seconds %= 60;

        String time;
        if (hours > 0) {
            time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            time = String.format("%02d:%02d", minutes, seconds);
        }
        return time;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);

        requestMediaStatus();
    }

    @Override
    protected void onPause() {
        String videoName = "";
        mMediaRouter.removeCallback(mMediaRouterCallback);
        if (this.mMediaPlayer != null && mMediaPlayer.getMediaInfo() != null) {
            videoName = mMediaPlayer.getMediaInfo().getMetadata()
                    .getString(MediaMetadata.KEY_TITLE);
        }
        if (videoName.length() > 0) {
            NotificationManager notificationManager = (NotificationManager) this
                    .getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            String appName = this.getResources().getString(
                    R.string.cast_application_name);
            Notification notification = new Notification(
                    R.drawable.ic_launcher, appName, System.currentTimeMillis());
            // notification.flags |= Notification.FLAG_ONGOING_EVENT;
            // notification.flags |= Notification.FLAG_NO_CLEAR;
            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
            CharSequence contentTitle = appName;
            CharSequence contentText = "正在播放: " + videoName;

            Intent notificationIntent = new Intent(SenderDemo.this,
                    SenderDemo.class);
            notificationIntent.putExtra("vname", videoName);
            notificationIntent.setData(Uri.parse(mVideoId));
            PendingIntent contentItent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);
            notification.setLatestEventInfo(this, contentTitle, contentText,
                    contentItent);
            notificationManager.notify(0, notification);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopServer();
        super.onDestroy();
    }

    public static String intToIp(int i) {
        return ((i) & 0xFF) + "." + ((i >> 8) & 0xFF) + "."
                + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
    }

    private void startServer(int port) {
        try {
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            mIpAddress = intToIp(wifiInfo.getIpAddress());

            if (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage(
                                "Please connect to a WIFI-network for starting the webserver.")
                        .setPositiveButton("OK", null).show();
                throw new Exception("Please connect to a WIFI-network.");
            }

            Log.e(TAG, "Starting server " + mIpAddress + ":" + port + ".");

            List<File> rootDirs = new ArrayList<File>();
            boolean quiet = false;
            Map<String, String> options = new HashMap<String, String>();
            rootDirs.add(new File(mRootDir).getAbsoluteFile());

            // mNanoHTTPD
            try {
                mNanoHTTPD = new SimpleWebServer(mIpAddress, port, rootDirs,
                        quiet);
                mNanoHTTPD.start();
            } catch (IOException ioe) {
                Log.e(TAG, "Couldn't start server:\n" + ioe);
            }

            Intent i = new Intent(this, SenderDemo.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i,
                    0);

            Notification notif = new Notification(R.drawable.ic_launcher,
                    "Webserver is running:" + mIpAddress + ":" + port,
                    System.currentTimeMillis());
            notif.setLatestEventInfo(this, "Webserver", "Webserver is running:"
                    + mIpAddress + ":" + port, contentIntent);
            notif.flags = Notification.FLAG_NO_CLEAR;
            mNotificationManager.notify(1234, notif);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void stopServer() {
        if (mNanoHTTPD != null) {
            mNanoHTTPD.stop();
            Log.e(TAG, "Server was killed.");
            mNotificationManager.cancelAll();
        } else {
            Log.e(TAG, "Cannot kill server!? Please restart your phone.");
        }
    }

    private void attachMediaPlayer() {
        if (mMediaPlayer != null) {
            return;
        }

        mMediaPlayer = new RemoteMediaPlayer();
        mMediaPlayer
                .setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {

                    @Override
                    public void onStatusUpdated() {
                        MediaStatus mediaStatus = mMediaPlayer.getMediaStatus();
                        if (mResumePlay
                                || mMediaPlayer.getMediaInfo() != null
                                && mMediaPlayer.getMediaInfo().getContentId()
                                        .equals(mVideoId)) {
                            if ((mediaStatus != null)
                                    && (mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_IDLE)) {
                                clearMediaState();
                            }

                            updatePlaybackPosition();
                            updateButtonStates();
                            startRefreshTimer();
                            mFirst = false;
                        } else {
                            cancelRefreshTimer();
                            mFirst = true;
                        }
                    }
                });

        mMediaPlayer
                .setOnMetadataUpdatedListener(new RemoteMediaPlayer.OnMetadataUpdatedListener() {
                    @Override
                    public void onMetadataUpdated() {
                        Log.d(TAG, "MediaControlChannel.onMetadataUpdated");
                    }
                });

        try {
            Flint.FlintApi.setMessageReceivedCallbacks(mApiClient,
                    mMediaPlayer.getNamespace(), mMediaPlayer);

            // use this channel to send message channel.
            Flint.FlintApi.setMessageReceivedCallbacks(mApiClient,
                    mFlintMsgChannel.getNamespace(), mFlintMsgChannel);
        } catch (IOException e) {
            Log.w(TAG, "Exception while launching application", e);
        }
    }

    private void updatePlaybackPosition() {
        if (mMediaPlayer == null) {
            return;
        }
        refreshPlaybackPosition(mMediaPlayer.getApproximateStreamPosition(),
                mMediaPlayer.getStreamDuration());
    }

    private void clearMediaState() {
        refreshPlaybackPosition(0, 0);
    }

    private void refreshPlaybackPosition(long position, long duration) {
        if (!mIsUserSeeking) {
            if (position == 0) {
                mTextViewCurtime.setText("00:00");
                mSeekbar.setProgress(0);
            } else if (position > 0) {
                mSeekbar.setProgress((int) TimeUnit.MILLISECONDS
                        .toSeconds(position));
            }
            mTextViewCurtime.setText(formatTime(position));
        }

        if (duration == 0) {
            mTextViewDuration.setText("00:00");
            mSeekbar.setMax(0);
        } else if (duration > 0) {
            mTextViewDuration.setText(formatTime(duration));
            if (!mIsUserSeeking) {
                mSeekbar.setMax((int) TimeUnit.MILLISECONDS.toSeconds(duration));
            }
        }
    }

    private void detachMediaPlayer() {
        if ((mMediaPlayer != null) && (mApiClient != null)) {
            try {
                Flint.FlintApi.removeMessageReceivedCallbacks(mApiClient,
                        mMediaPlayer.getNamespace());

                Flint.FlintApi.removeMessageReceivedCallbacks(mApiClient,
                        mFlintMsgChannel.getNamespace());
            } catch (IOException e) {
                Log.w(TAG, "Exception while detaching media player", e);
            }
        }
        mMediaPlayer = null;
    }

    private void setSelectedDevice(FlintDevice device) {
        mSelectedDevice = device;
        if (mSelectedDevice == null) {
            detachMediaPlayer();
            
            if (mApiClient != null && mApiClient.isConnected()) {
                Flint.FlintApi.stopApplication(mApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status result) {
                                if (result.isSuccess()) {
                                    mAppMetadata = null;
                                    refreshPlaybackPosition(0, 0);
                                    detachMediaPlayer();
                                    updateButtonStates();
                                } else {
                                    // showErrorDialog(getString(R.string.error_app_stop_failed));
                                }
                            }
                        });
            }
            
            if ((mApiClient != null) && mApiClient.isConnected()) {
                mApiClient.disconnect();
            }
        } else {
            Log.d(TAG, "acquiring controller for " + mSelectedDevice);
            try {
                Flint.FlintOptions.Builder apiOptionsBuilder = Flint.FlintOptions
                        .builder(mSelectedDevice, mCastListener);

                mApiClient = new FlintManager.Builder(this)
                        .addApi(Flint.API, apiOptionsBuilder.build())
                        .addConnectionCallbacks(mConnectionCallbacks).build();
                mApiClient.connect();
            } catch (IllegalStateException e) {
                Log.w(TAG, "error while creating a device controller", e);
            }
        }
    }

    private void startRefreshTimer() {
        mHandler.postDelayed(mRefreshRunnable, REFRESH_INTERVAL_MS);
    }

    private void updateButtonStates() {
        boolean hasMediaConnection = (mMediaPlayer != null)
                && !mWaitingForReconnect;

        if (hasMediaConnection) {
            MediaStatus mediaStatus = mMediaPlayer.getMediaStatus();
            if (mediaStatus != null) {
                int mediaPlayerState = mediaStatus.getPlayerState();
                int playerState = PLAYER_STATE_NONE;
                mFirst = false;
                if (mediaPlayerState == MediaStatus.PLAYER_STATE_PAUSED) {
                    playerState = PLAYER_STATE_PAUSED;
                } else if (mediaPlayerState == MediaStatus.PLAYER_STATE_PLAYING) {
                    playerState = PLAYER_STATE_PLAYING;
                } else if (mediaPlayerState == MediaStatus.PLAYER_STATE_BUFFERING) {
                    playerState = PLAYER_STATE_BUFFERING;
                } else if (mediaPlayerState == MediaStatus.PLAYER_STATE_IDLE) {
                    playerState = PLAYER_STATE_FINISHED;
                    
                    mFirst = true;
                    mBtnPlay.setBackgroundResource(R.drawable.btn_play);
                }
                setPlayerState(playerState);

            }
        } else {
            setPlayerState(PLAYER_STATE_NONE);
        }
    }

    private void setPlayerState(int playerState) {
        mPlayerState = playerState;
        if (mPlayerState == PLAYER_STATE_PAUSED || mPlayerState == PLAYER_STATE_NONE || mPlayerState == PLAYER_STATE_FINISHED) {
            mBtnPlay.setBackgroundResource(R.drawable.btn_play);
        } else if (mPlayerState == PLAYER_STATE_PLAYING) {
            mBtnPlay.setBackgroundResource(R.drawable.btn_pause);
        }

         mBtnPlay.setEnabled((mPlayerState == PLAYER_STATE_PAUSED)
         || (mPlayerState == PLAYER_STATE_PLAYING) || (mPlayerState == PLAYER_STATE_FINISHED) );
    }

    class MyMediaRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteAdded(MediaRouter router, RouteInfo route) {
            super.onRouteAdded(router, route);
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteVolumeChanged: route=" + route);
            super.onRouteVolumeChanged(router, route);
        }

        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteSelected: " + route);

            FlintDevice device = FlintDevice.getFromBundle(route.getExtras());
            setSelectedDevice(device);
            alert("selected: " + device.getFriendlyName());
            updateButtonStates();

        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteUnselected: route=" + route);
            setSelectedDevice(null);
            mAppMetadata = null;
            clearMediaState();
            updateButtonStates();
        }
    }

    private class CastListener extends Flint.Listener {
        @Override
        public void onVolumeChanged() {
            // refreshDeviceVolume(Cast.CastApi.getVolume(mApiClient),
            // Cast.CastApi.isMute(mApiClient));
        }

        @Override
        public void onApplicationStatusChanged() {
            if (mApiClient.isConnected()) {
                String status = Flint.FlintApi.getApplicationStatus(mApiClient);
                Log.d(TAG, "onApplicationStatusChanged; status=" + status);
            }
        }

        @Override
        public void onApplicationDisconnected(int statusCode) {
            Log.d(TAG, "onApplicationDisconnected: statusCode=" + statusCode);
            mAppMetadata = null;
            detachMediaPlayer();
            clearMediaState();
            updateButtonStates();
            // if (statusCode != CastStatusCodes.SUCCESS) {
            // // This is an unexpected disconnect.
            // setApplicationStatus(getString(R.string.status_app_disconnected));
            // }
            if (mMediaRouter != null) {
                mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
            }
        }
    }

    private void requestMediaStatus() {
        if (mMediaPlayer == null) {
            return;
        }

        Log.d(TAG, "requesting current media status");
        mMediaPlayer.requestStatus(mApiClient).setResultCallback(
                new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                    @Override
                    public void onResult(MediaChannelResult result) {
                        Status status = result.getStatus();
                        if (!status.isSuccess()) {
                            Log.w(TAG,
                                    "Unable to request status: "
                                            + status.getStatusCode());
                        }
                    }
                });
    }

    private final class ApplicationConnectionResultCallback implements
            ResultCallback<Flint.ApplicationConnectionResult> {

        public ApplicationConnectionResultCallback(String suffix) {
        }

        @Override
        public void onResult(ApplicationConnectionResult result) {
            Status status = result.getStatus();
            alert("join app: " + status.isSuccess());
            if (status.isSuccess()) {
                attachMediaPlayer();
                // mAppMetadata = applicationMetadata;
                //
                // updateButtonStates();
                requestMediaStatus();
                // Log.d(mClassTag,
                // mMediaPlayer.getMediaStatus().getMediaInfo().getContentId());

                // mBtnPlay.setClickable(true);

                sendURL(mVideoId, mIntent.getStringExtra("vname"));
            } else {
            }
        }
    }

    private class MediaResultCallback implements
            ResultCallback<MediaChannelResult> {
        private final String mOperationName;

        public MediaResultCallback(String operationName) {
            mOperationName = operationName;
        }

        @Override
        public void onResult(MediaChannelResult result) {
            Status status = result.getStatus();
            mBtnPlay.setEnabled(true);
            if (!status.isSuccess()) {
                Log.w(TAG,
                        mOperationName + " failed: " + status.getStatusCode());
            }
            onFinished();
        }

        protected void onFinished() {
        }
    }

    private void cancelRefreshTimer() {
        mHandler.removeCallbacks(mRefreshRunnable);
    }

    private class ConnectionCallbacks implements
            FlintManager.ConnectionCallbacks {
        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "ConnectionCallbacks.onConnectionSuspended");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWaitingForReconnect = true;
                    cancelRefreshTimer();
                    detachMediaPlayer();
                    updateButtonStates();
                }
            });
        }

        @Override
        public void onConnected(final Bundle connectionHint) {
            Log.d(TAG, "ConnectionCallbacks.onConnected");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    alert("Connection success");
                    if (!mApiClient.isConnected()) {
                        // We got disconnected while this runnable was pending
                        // execution.
                        return;
                    }
                    try {
                        Flint.FlintApi.requestStatus(mApiClient);
                    } catch (IOException e) {
                        Log.d(TAG, "error requesting status", e);
                    }
                    android.util.Log.d(TAG, "start launchApplication");

                    // attachMediaPlayer();

                    // setDeviceVolumeControlsEnabled(true);
                    // mLaunchAppButton.setEnabled(true);
                    // mJoinAppButton.setEnabled(true);
                    //
                    if (mWaitingForReconnect) {
                        mWaitingForReconnect = false;
                        if ((connectionHint != null)
                                && connectionHint
                                        .getBoolean(Flint.EXTRA_APP_NO_LONGER_RUNNING)) {
                            Log.d(TAG, "App  is no longer running");
                            detachMediaPlayer();
                            mAppMetadata = null;
                            clearMediaState();
                            updateButtonStates();
                        } else {
                            attachMediaPlayer();
                            requestMediaStatus();
                            startRefreshTimer();
                        }
                    } else {
                        Flint.FlintApi
                                .launchApplication(
                                        mApiClient,
                                        "http://openflint.github.io/flint-player/player.html",
                                        false)
                                .setResultCallback(
                                        new ApplicationConnectionResultCallback(
                                                "LaunchApp"));
                    }
                }
            });
        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.d(TAG, "onConnectionFailed");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateButtonStates();
                    clearMediaState();
                    cancelRefreshTimer();
                }
            });
        }
    }
}
