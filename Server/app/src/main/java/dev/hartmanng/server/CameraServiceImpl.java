// Copyright 2025 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package dev.hartmanng.server;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

public class CameraServiceImpl extends Service {
    private static final String TAG = "Server.CameraServiceImpl";

    private class CameraServiceBinder extends ICameraService.Stub {
        @Override
        public PendingIntent getRequestPermissionPendingIntent(String permission) {
            Log.i(TAG, "getRequestPermissionPendingIntent(): " + permission);

            return PermissionRequestActivity.getPendingIntent(getApplicationContext(), permission);
        }

        @Override
        public void connectCameraToSurface(Surface surface) {
            Log.i(TAG, "connectCameraToSurface()");

            // This is probably not cleaned up properly (the CameraManager never really "lets go" of the
            // camera).
            (new CameraManager(surface)).connect(getApplicationContext());
        }
    }

    private ICameraService.Stub mBinder = null;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");

        mBinder = new CameraServiceBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");

        // This is a Foreground Service:
        // https://developer.android.com/develop/background-work/services/fgs/declare
        String channelId = "ServerChannel";
        NotificationChannel channel = new NotificationChannel(channelId, "ServerChannel" /* name */,
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Server notification channel.");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        NotificationCompat.Builder notificationCompatBuilder =
                new NotificationCompat.Builder(this /* context */, channelId);
        notificationCompatBuilder.setContentTitle("Server");
        notificationCompatBuilder.setContentText("Server is running.");

        ServiceCompat.startForeground(this /* service */, startId,
                notificationCompatBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);

        // I don't think this matters for our purposes.
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind()");

        return false /* allowRebind */;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
    }
}
