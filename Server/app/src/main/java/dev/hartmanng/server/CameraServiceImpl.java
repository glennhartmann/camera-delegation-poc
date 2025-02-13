// Copyright 2025 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package dev.hartmanng.server;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

public class CameraServiceImpl extends Service {
    private static final String TAG = "Server.CameraServiceImpl";

    private ICameraService.Stub mBinder = null;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");

        mBinder = new CameraServiceBinder(getApplicationContext());
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

        int foregroundServiceType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA : 0;
        ServiceCompat.startForeground(this /* service */, startId,
                notificationCompatBuilder.build(), foregroundServiceType);

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

        mBinder = null;
    }
}
