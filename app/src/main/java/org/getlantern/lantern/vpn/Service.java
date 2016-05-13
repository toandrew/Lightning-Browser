package org.getlantern.lantern.vpn;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.getlantern.lantern.model.Utils;

import acr.browser.lightning.constant.MyConstants;
import go.lantern.Lantern;

import acr.browser.lightning.constant.Constants;

public class Service extends VpnBuilder implements Runnable {

    private static final String TAG = "VpnService";
    public static boolean IsRunning = false;

    private Thread mThread = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "VpnService created");
        mThread = new Thread(this, "VpnService");
        mThread.start();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IsRunning = true;
        return super.onStartCommand(intent, flags, startId);
    }

    // isRunning checks to see if the VPN service is already running
    // in the background.
    public static boolean isRunning(Context c) {
        ActivityManager manager = (ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (Service.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void run() {
        try {
            Log.d(TAG, "Loading Lantern library");
            Lantern.ProtectConnections(getDnsResolver(getApplicationContext()), new Lantern.SocketProtector.Stub() {
                // Protect is used to exclude a socket specified by fileDescriptor
                // from the VPN connection. Once protected, the underlying connection
                // is bound to the VPN device and won't be forwarded
                @Override
                public void Protect(long fileDescriptor) throws Exception {
                    if (!protect((int) fileDescriptor)) {
                        throw new Exception("protect socket failed");
                    }
                }
            });
            int startTimeoutMillis = 60000;
            String analyticsTrackingID = MyConstants.GOOGLE_ANALYTICS_TRACKING_ID;
            //String analyticsTrackingID = null; // does not use gms to track info.
            org.lantern.mobilesdk.StartResult result = org.lantern.mobilesdk.Lantern.enable(getApplicationContext(), startTimeoutMillis, analyticsTrackingID);
            configure(result.getSOCKS5Addr());

            updateVpnStatus(Constants.VPN_SERVICE_STATUS_STARTED);

            while (IsRunning) {
                // sleep to avoid busy looping
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Exception", e);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Fatal error", e);
        } finally {
            Log.e(TAG, "Lantern terminated.[" + IsRunning + "]");
            stop();
        }
    }

    private synchronized void stop() {
        try {
            super.close();
            Log.d(TAG, "Closing VPN interface..");
            Utils.clearPreferences(this);
        } catch (Exception e) {
        }

        stopSelf();
        IsRunning = false;

        // show sth when service stopped.
        updateVpnStatus(Constants.VPN_SERVICE_STATUS_STOPPED);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Lantern VpnService destroyed");
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    @Override
    public void onRevoke() {
        Log.d(TAG, "onRevoke!!!!");
        super.onRevoke();
    }
}
