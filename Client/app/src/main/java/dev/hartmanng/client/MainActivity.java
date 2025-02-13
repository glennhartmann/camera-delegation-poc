// Copyright 2025 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package dev.hartmanng.client;

import android.Manifest;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import dev.hartmanng.server.ICameraService;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Client.MainActivity";

    private ICameraService mService = null;
    private ServiceConnection mConnection = null;

    private static Intent getCameraServiceIntent() {
        Intent intent = new Intent(ICameraService.class.getName());
        intent.setComponent(new ComponentName("dev.hartmanng.server",
                "dev.hartmanng.server.CameraServiceImpl"));
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout, style, theme, etc.
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                Log.i(TAG, "onServiceConnected(): " + className);

                mService = ICameraService.Stub.asInterface(service);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.i(TAG, "onServiceDisconnected(): " + componentName);
            }
        };

        findViewById(R.id.bindServiceButton).setOnClickListener(v -> {
            if (!bindService(getCameraServiceIntent(), mConnection,
                    Context.BIND_AUTO_CREATE)) {
                Log.e(TAG, "bindService() returned false");
            }
        });

        findViewById(R.id.requestNotificationPermissionButton).setOnClickListener(
                v -> requestDelegatedPermission(Manifest.permission.POST_NOTIFICATIONS));

        findViewById(R.id.requestCameraPermissionButton).setOnClickListener(
                v -> requestDelegatedPermission(Manifest.permission.CAMERA));

        findViewById(R.id.startForegroundServiceButton).setOnClickListener(
                v -> getApplicationContext().startForegroundService(getCameraServiceIntent()));

        findViewById(R.id.delegateCameraButton).setOnClickListener(v -> {
            try {
                mService.connectCameraToSurface(((VideoView) findViewById(R.id.videoView))
                        .getHolder().getSurface());
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException: ", e);
            }
        });
    }

    private void requestDelegatedPermission(String permission) {
        try {
            PendingIntent pendingIntent = mService.getRequestPermissionPendingIntent(permission);

            ActivityOptions activityOptions = ActivityOptions.makeBasic();
            activityOptions.setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);

            pendingIntent.send(getApplicationContext(), 0 /* code */, null /* intent */,
                    null /* onFinished */, null /* handler */, null /* requiredPermission */,
                    activityOptions.toBundle());
        } catch (RemoteException e) {
            Log.e(TAG, "remote exception: " + e);
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "pending intent canceled: " + e);
        }
    }
}