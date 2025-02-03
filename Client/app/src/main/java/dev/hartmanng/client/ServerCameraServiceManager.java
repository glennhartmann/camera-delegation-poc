// Copyright 2025 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package dev.hartmanng.client;

import android.Manifest;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;

import dev.hartmanng.server.ICameraService;

public class ServerCameraServiceManager {
    private static final String TAG = "Client.ServerCameraServiceManager";

    // Must be the same as the one in Server's PermissionRequestActivity.java.
    private static final String SERVER_EXTRA_CALLBACK = "callback";

    private ICameraService mService = null;
    private final ContextWrapper mContextWrapper;
    private final ServiceConnection mConnection;

    public interface SimpleCallback {
        void run();
    }

    private static Intent getCameraServiceIntent() {
        Intent intent = new Intent(ICameraService.class.getName());
        intent.setComponent(new ComponentName("dev.hartmanng.server",
                "dev.hartmanng.server.CameraServiceImpl"));
        return intent;
    }

    public ServerCameraServiceManager(ContextWrapper contextWrapper,
                                      SimpleCallback onServiceConnectedCallback,
                                      SimpleCallback onServiceDisconnectedCallback) {
        mContextWrapper = contextWrapper;

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                Log.i(TAG, "onServiceConnected(): " + className);

                mService = ICameraService.Stub.asInterface(service);
                onServiceConnectedCallback.run();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.i(TAG, "onServiceDisconnected(): " + componentName);

                mService = null;
                onServiceDisconnectedCallback.run();
            }
        };
    }

    public void bindService() {
        if (!mContextWrapper.bindService(getCameraServiceIntent(), mConnection,
                Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "bindService() returned false");
        }
    }

    public void requestDelegatedPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // This is a bit of a pain. We can't just request permissions one after the other,
            // because the requests are async. That means we would call into the Server's
            // PermissionRequestActivity a second time before it had finished the first time.
            // Instead, we set it up such that we call it once, then it sends us a response (via a
            // PendingIntent to MainActivity) when it finishes, then we can freely call it the
            // second time.
            MainActivity.setServerCallback(new MainActivity.SimpleCallback() {
                @Override
                public void run() {
                    Log.i(TAG, "in MainActivity server callback");
                    requestDelegatedPermission(android.Manifest.permission.CAMERA,
                            false /* requestCallback */);
                }
            });
            requestDelegatedPermission(Manifest.permission.POST_NOTIFICATIONS,
                    true /* requestCallback */);
        } else {
            requestDelegatedPermission(android.Manifest.permission.CAMERA,
                    false /* requestCallback */);
        }
    }

    public void startForegroundService() {
        // Note that this service is never properly cleaned up :/
        mContextWrapper.getApplicationContext().startForegroundService(getCameraServiceIntent());
    }

    public void delegateCameraToSurface(Surface surface) {
        if (mService == null) {
            Log.e(TAG, "service not bound?");
            return;
        }

        try {
            mService.connectCameraToSurface(surface);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException: ", e);
        }
    }

    public void unbindService() {
        if (mService != null) {
            mContextWrapper.unbindService(mConnection);
            mService = null;
        }
    }

    public boolean serviceExists() {
        return mService != null;
    }

    private void requestDelegatedPermission(String permission, boolean requestCallback) {
        try {
            PendingIntent pendingIntent = mService.getRequestPermissionPendingIntent(permission);

            ActivityOptions activityOptions = ActivityOptions.makeBasic();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activityOptions.setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
            }

            Intent intent = new Intent();
            if (requestCallback) {
                intent.putExtra(SERVER_EXTRA_CALLBACK,
                        MainActivity.getPendingIntent(mContextWrapper));
            }

            pendingIntent.send(mContextWrapper.getApplicationContext(), 0 /* code */,
                    intent, null /* onFinished */, null /* handler */,
                    null /* requiredPermission */, activityOptions.toBundle());
        } catch (RemoteException e) {
            Log.e(TAG, "remote exception: " + e);
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "pending intent canceled: " + e);
        }
    }
}
