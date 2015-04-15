/*
 * Copyright (C) 2013-2015, The OpenFlint Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.crosskr.flint.browser;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.ConnectableDeviceListener;
import com.connectsdk.device.DevicePicker;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManager.PairingLevel;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.DeviceService.PairingType;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaControl.DurationListener;
import com.connectsdk.service.capability.MediaControl.PlayStateListener;
import com.connectsdk.service.capability.MediaControl.PlayStateStatus;
import com.connectsdk.service.capability.MediaControl.PositionListener;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.MediaPlayer.MediaLaunchObject;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.VolumeControl.MuteListener;
import com.connectsdk.service.capability.VolumeControl.VolumeListener;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.sessions.LaunchSession;

public class FlintVideoManager {
    private static final String TAG = FlintVideoManager.class.getSimpleName();

    public static final double MAX_VOLUME_LEVEL = 100;

    private Context mContext;

    private FlintStatusChangeListener mStatusChangeListener;

    ImageButton mMediaRouteButton;

    private String mCurrentDeviceName;

    private ConnectableDevice mTV;

    private AlertDialog mDeviceDialog;

    private AlertDialog mPairingAlertDialog;

    private AlertDialog mPairingCodeDialog;

    private DevicePicker mDevicePicker;

    private MediaPlayer mMediaPlayer;

    private VolumeControl mVolumeControl;

    public LaunchSession mLaunchSession = null;

    private MediaControl mMediaControl = null;

    private PlayStateStatus mCurrentPlayStateStatus;

    private long mDuration = 0;

    private long mCurrentTime;

    private double mCurrentStreamVolume = 0;

    private boolean mIsStreamMuted;
    
    private boolean mQuit = false;

    public FlintVideoManager(Context context, String applicationId,
            FlintStatusChangeListener listener, ImageButton mediaRouteButton) {

        mContext = context;

        mStatusChangeListener = listener;

        mMediaRouteButton = mediaRouteButton;

        // connectsdk
        setupPicker();

        DiscoveryManager.getInstance().setPairingLevel(PairingLevel.ON);
    }

    /**
     * start device found
     */
    public void onStart() {
        DiscoveryManager.getInstance().start();
    }

    /**
     * stop device found
     */
    public void onStop() {
        DiscoveryManager.getInstance().stop();
    }

    /**
     * Play
     */
    public void playMedia() {
        if (mMediaControl != null) {
            mMediaControl.play(new ResponseListener<Object>() {

                @Override
                public void onError(ServiceCommandError error) {
                    // TODO Auto-generated method stub

                    mCurrentPlayStateStatus = PlayStateStatus.Unknown;
                }

                @Override
                public void onSuccess(Object object) {
                    // TODO Auto-generated method stub

                    mCurrentPlayStateStatus = PlayStateStatus.Playing;
                }

            });
        }
    }

    /**
     * Pause
     */
    public void pauseMedia() {
        if (mMediaControl != null) {
            mMediaControl.pause(new ResponseListener<Object>() {

                @Override
                public void onError(ServiceCommandError error) {
                    // TODO Auto-generated method stub

                    mCurrentPlayStateStatus = PlayStateStatus.Unknown;
                }

                @Override
                public void onSuccess(Object object) {
                    // TODO Auto-generated method stub

                    mCurrentPlayStateStatus = PlayStateStatus.Paused;
                }

            });
        }
    }

    /**
     * Seek media
     * 
     * @param position
     */
    public void seekMedia(long position) {
        if (mMediaControl != null && getTv().hasCapability(MediaControl.Seek)) {

            mCurrentTime = position;

            mMediaControl.seek(position, new ResponseListener<Object>() {

                @Override
                public void onSuccess(Object response) {
                    Log.d("LG", "Success on Seeking");

                    mStatusChangeListener.onMediaSeekEnd();
                }

                @Override
                public void onError(ServiceCommandError error) {
                    Log.w("Connect SDK", "Unable to seek: " + error.getCode());

                    mStatusChangeListener.onMediaSeekEnd();
                }
            });
        }
    }

    /**
     * Set media's stream volume
     * 
     * @param volume
     */
    public void setMediaVolume(final double volume) {
        if (getVolumeControl() != null) {
            getVolumeControl().setVolume((float) volume,
                    new ResponseListener<Object>() {

                        @Override
                        public void onError(ServiceCommandError error) {
                            // TODO Auto-generated method stub

                        }

                        @Override
                        public void onSuccess(Object object) {
                            // TODO Auto-generated method stub

                            mCurrentStreamVolume = volume;

                            mStatusChangeListener
                                    .onVolumeChanged(volume, false);
                        }

                    });

        }
    }

    public double getMediaVolume() {
        if (getVolumeControl() != null) {
            getVolumeControl().getVolume(new VolumeListener() {

                @Override
                public void onSuccess(Float object) {
                    // TODO Auto-generated method stub

                    mCurrentStreamVolume = object;
                    Log.e(TAG, "getMediaVolume: " + mCurrentStreamVolume
                            + " vol:" + object.floatValue());
                }

                @Override
                public void onError(ServiceCommandError error) {
                    // TODO Auto-generated method stub

                }

            });
        }
        return mCurrentStreamVolume;
    }

    /**
     * Set media mute
     * 
     * @param on
     */
    public void setMediaMute(boolean on) {
        if (getVolumeControl() != null) {
            getVolumeControl().setMute(on, new ResponseListener<Object>() {

                @Override
                public void onError(ServiceCommandError error) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onSuccess(Object object) {
                    // TODO Auto-generated method stub

                }

            });
        }
    }

    public boolean isMedaiMuted() {
        if (getVolumeControl() != null) {
            getVolumeControl().getMute(new MuteListener() {

                @Override
                public void onSuccess(Boolean object) {
                    // TODO Auto-generated method stub

                    mIsStreamMuted = object;
                }

                @Override
                public void onError(ServiceCommandError error) {
                    // TODO Auto-generated method stub

                }

            });
        }
        return mIsStreamMuted;
    }

    /**
     * Whether device is connected
     * 
     * @return
     */
    public boolean isDeviceConnected() {
        return (mTV != null ? mTV.isConnected() : false);
    }

    /**
     * whether media is connected
     * 
     * @return
     */
    public boolean isMediaConnected() {
        return getMediaPlayer() != null;
    }

    /**
     * Get current media time
     * 
     * @return
     */
    public long getMediaCurrentTime() {
        if (mMediaControl != null) {
            mMediaControl.getPosition(mPositionListener);
        }

        return mCurrentTime;
    }

    /**
     * Get current media duration
     * 
     * @return
     */
    public long getMediaDuration() {
        if (mDuration <= 0 && mMediaControl != null) {
            mMediaControl.getDuration(mDurationListener);
        }
        return mDuration;
    }

    /**
     * Get media status
     * 
     * @return
     */
    public PlayStateStatus getMediaStatus() {
        if (mMediaControl != null) {
            mMediaControl.getPlayState(new PlayStateListener() {

                @Override
                public void onSuccess(PlayStateStatus object) {
                    // TODO Auto-generated method stub

                    mCurrentPlayStateStatus = object;
                }

                @Override
                public void onError(ServiceCommandError error) {
                    // TODO Auto-generated method stub

                    mCurrentPlayStateStatus = PlayStateStatus.Unknown;
                }

            });
        }

        return mCurrentPlayStateStatus;
    }

    /**
     * Send custom message
     * 
     * @param msg
     *            custom message
     */
    public void sendCustMsg(String msg) {
        // TODO
        // mFlintMsgChannel.show(mApiClient, msg);
    }

    /**
     * Set custom message to device. let device use hardware decoder or not
     * 
     * @param flag
     */
    public void setHardwareDecoder(boolean flag) {
        // TODO
        // if (mApiClient == null || !mApiClient.isConnected()) {
        // return;
        // }
        //
        // mFlintMsgChannel.setHardwareDecoder(mApiClient, flag);
    }

    private ConnectableDeviceListener mDeviceListener = new ConnectableDeviceListener() {

        @Override
        public void onDeviceReady(ConnectableDevice device) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onDeviceReady:" + device);

            onDeviceSelected(device);
        }

        @Override
        public void onDeviceDisconnected(ConnectableDevice device) {
            // TODO Auto-generated method stub

            Log.e(TAG, "onDeviceDisconnected:" + device);
        }

        @Override
        public void onPairingRequired(ConnectableDevice device,
                DeviceService service, PairingType pairingType) {
            // TODO Auto-generated method stub

            Log.e(TAG, "onPairingRequired:" + device);

        }

        @Override
        public void onCapabilityUpdated(ConnectableDevice device,
                List<String> added, List<String> removed) {
            // TODO Auto-generated method stub

            Log.e(TAG, "onCapabilityUpdated:" + device);
        }

        @Override
        public void onConnectionFailed(ConnectableDevice device,
                ServiceCommandError error) {
            // TODO Auto-generated method stub

            Log.e(TAG, "onConnectionFailed:" + device);

        }

    };

    private void setupPicker() {
        DiscoveryManager.getInstance().registerDefaultDeviceTypes();

        mDevicePicker = new DevicePicker((BrowserActivity) mContext);
        mDeviceDialog = mDevicePicker.getPickerDialog("Device List",
                new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                            int arg2, long arg3) {

                        mTV = (ConnectableDevice) arg0.getItemAtPosition(arg2);
                        mTV.addListener(mDeviceListener);
                        mTV.connect();

                        Log.e(TAG, "mTV:" + mTV.toString());

                        mMediaRouteButton
                                .setImageResource(R.drawable.mr_ic_media_route_on_holo_dark);

                        mDevicePicker.pickDevice(mTV);
                    }
                });

        mPairingAlertDialog = new AlertDialog.Builder(
                (BrowserActivity) mContext)
                .setTitle("Pairing with TV")
                .setMessage("Please confirm the connection on your TV")
                .setPositiveButton("Okay", null)
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                mDevicePicker.cancelPicker();

                                // hConnectToggle();
                            }
                        }).create();

        final EditText input = new EditText((BrowserActivity) mContext);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        final InputMethodManager imm = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        mPairingCodeDialog = new AlertDialog.Builder((BrowserActivity) mContext)
                .setTitle("Enter Pairing Code on TV")
                .setView(input)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                if (mTV != null) {
                                    String value = input.getText().toString()
                                            .trim();
                                    mTV.sendPairingKey(value);
                                    imm.hideSoftInputFromWindow(
                                            input.getWindowToken(), 0);
                                }
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                mDevicePicker.cancelPicker();

                                // hConnectToggle();
                                imm.hideSoftInputFromWindow(
                                        input.getWindowToken(), 0);
                            }
                        }).create();

        mMediaRouteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mTV == null || (getMediaPlayer() != null && mLaunchSession == null)) {
                    mDeviceDialog.show();
                } else {
                    mMediaRouteButton
                            .setImageResource(R.drawable.mr_ic_media_route_off_holo_dark);
                    onDeviceUnselected((ConnectableDevice) null);
                }
            }

        });
    }

    public boolean isConnected() {
        return true;
    }

    public ConnectableDevice getSelectedDevice() {
        return mTV;
    }

    public boolean isPlaying() {
        return true;
    }

    /**
     * Connect select device
     * 
     * @param device
     */
    private void onDeviceSelected(ConnectableDevice device) {
        mCurrentDeviceName = device.getFriendlyName();

        mQuit = false;
        
        setTv(device);

        if (mStatusChangeListener != null) {
            mStatusChangeListener.onDeviceSelected(device.getFriendlyName());
        }
    }

    /**
     * Disconnect device
     * 
     * @param device
     */
    private void onDeviceUnselected(ConnectableDevice device) {
        mCurrentDeviceName = "";

        mQuit = true;
        
        setTv(null);

        if (mStatusChangeListener != null)
            mStatusChangeListener.onDeviceUnselected();
    }

    public void playVideo(String url, String title) {
        String mimeType = "video/mp4";
        String description = "";
        String icon = "http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/videoIcon.jpg";// must
                                                                                                              // for
        String convertTitle = title;
        Log.e(TAG, "title:" + title);
        try {
            convertTitle = new String(new String(title.getBytes("gbk")));
        } catch (Exception e) {

        }
        if (getMediaPlayer() == null) {
            Log.e(TAG, "playVideo failed for mediaplayer is null!");
            return;
        }
        getMediaPlayer().playMedia(url, mimeType, title, description, icon,
                false, new MediaPlayer.LaunchListener() {

                    public void onSuccess(MediaLaunchObject object) {
                        mLaunchSession = object.launchSession;
                        Log.e(TAG, "type:" + mLaunchSession.getSessionType());

                        mMediaControl = object.mediaControl;
                        
                        if (mQuit) {
                            Log.e(TAG, "playMedia!quit?!");
                            setTv(null);
                            return;
                        }

                        stopUpdating();

                        enableMedia();
                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                        if (mLaunchSession != null) {
                            mLaunchSession.close(null);
                            mLaunchSession = null;
                            stopUpdating();
                        }

                        onDeviceUnselected(null);
                        
                        mStatusChangeListener.onConnectionFailed();

                        mStatusChangeListener.onMediaStatusUpdated();

                        mStatusChangeListener.onMediaMetadataUpdated("", "",
                                null);
                    }
                });
    }

    public void setTv(ConnectableDevice tv) {
        if (tv == null) {
            stopTvApplication();
        } else {
            mTV = tv;

            mMediaPlayer = mTV.getCapability(MediaPlayer.class);
            mMediaControl = mTV.getCapability(MediaControl.class);
            mVolumeControl = mTV.getCapability(VolumeControl.class);

            playVideo(((BrowserActivity) mContext).getCurentVideoUrl(),
                    ((BrowserActivity) mContext).getCurrentVideoTitle());
        }
    }

    /**
     * Stop receiver application.
     */
    public void stopTvApplication() {
        Log.e(TAG, "stopTvApplication!");
        if (getMediaPlayer() != null) {
            Log.e(TAG, "stopTvApplication 1");
            if (mLaunchSession != null) {
                Log.e(TAG,
                        "launchSession: type:"
                                + mLaunchSession.getSessionType());
                mLaunchSession.close(new ResponseListener<Object>() {

                    @Override
                    public void onError(ServiceCommandError error) {
                        // TODO Auto-generated method stub
                        if (mTV != null) {
                            mTV.disconnect();

                            mTV = null;
                        }
                        
                        mQuit = false;
                        
                        mMediaPlayer = null;
                        mMediaControl = null;
                        mVolumeControl = null;

                        mLaunchSession = null;
                        
                        disableMedia();

                        stopUpdating();

                        mStatusChangeListener.onStopApplication();

                        mStatusChangeListener.onApplicationDisconnected();
                    }

                    @Override
                    public void onSuccess(Object object) {
                        // TODO Auto-generated method stub
                        if (mTV != null) {
                            mTV.disconnect();

                            mTV = null;
                        }

                        mQuit = false;
                        
                        mMediaPlayer = null;
                        mMediaControl = null;
                        mVolumeControl = null;
                        
                        mLaunchSession = null;
                        
                        disableMedia();

                        stopUpdating();

                        mStatusChangeListener.onStopApplication();

                        mStatusChangeListener.onApplicationDisconnected();
                    }
                    
                });
            }
        }
    }

    public MediaPlayer getMediaPlayer() {
        Log.e(TAG, "getMediaPlayer: " + mMediaPlayer);
        return mMediaPlayer;
    }

    public ConnectableDevice getTv() {
        return mTV;
    }

    public VolumeControl getVolumeControl() {
        return mVolumeControl;
    }

    public void disableMedia() {
        // stopMedia();
    }

    private void stopUpdating() {
    }

    private void enableMedia() {
        // refresh current volume
        getMediaVolume();

        mStatusChangeListener.onConnected();

        mStatusChangeListener.onMediaStatusUpdated();

        mStatusChangeListener.onMediaMetadataUpdated("", "", null);

        mStatusChangeListener.onApplicationConnectionResult("enabled");

        if (getTv().hasCapability(MediaControl.PlayState_Subscribe)) {
            mMediaControl.subscribePlayState(mPlayStateListener);
        }
    }

    private void startUpdating() {

    }

    public String getCurrentSelectDeviceName() {
        return mCurrentDeviceName;
    }

    public PlayStateListener mPlayStateListener = new PlayStateListener() {

        @Override
        public void onError(ServiceCommandError error) {
            Log.d("LG", "Playstate Listener error = " + error);
        }

        @Override
        public void onSuccess(PlayStateStatus playState) {
            Log.d(TAG, "Playstate changed | playState = " + playState);

            switch (playState) {
            case Playing:
                startUpdating();

                if (mMediaControl != null
                        && getTv().hasCapability(MediaControl.Duration)) {
                    mMediaControl.getDuration(mDurationListener);
                }
                break;
            case Finished:
                break;
            case Idle:
                mMediaRouteButton
                        .setImageResource(R.drawable.mr_ic_media_route_off_holo_dark);
                onDeviceUnselected((ConnectableDevice) null);
                break;
            default:
                stopUpdating();
                break;
            }
        }
    };

    private DurationListener mDurationListener = new DurationListener() {

        @Override
        public void onError(ServiceCommandError error) {
            mDuration = -1;
        }

        @Override
        public void onSuccess(Long duration) {
            Log.e(TAG, "Duration:" + duration);
            mDuration = duration;
        }
    };

    private PositionListener mPositionListener = new PositionListener() {

        @Override
        public void onError(ServiceCommandError error) {
        }

        @Override
        public void onSuccess(Long position) {
            mCurrentTime = position;
        }
    };
}
