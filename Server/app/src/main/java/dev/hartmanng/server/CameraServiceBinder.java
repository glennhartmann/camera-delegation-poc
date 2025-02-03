// Copyright 2025 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package dev.hartmanng.server;

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;
import android.view.Surface;

public class CameraServiceBinder extends ICameraService.Stub {
    private static final String TAG = "Server.CameraServiceBinder";

    private final Context mApplicationContext;

    public CameraServiceBinder(Context applicationContext) {
        mApplicationContext = applicationContext;
    }

    @Override
    public PendingIntent getRequestPermissionPendingIntent(String permission) {
        Log.i(TAG, "getRequestPermissionPendingIntent(): " + permission);

        return PermissionRequestActivity.getPendingIntent(mApplicationContext, permission);
    }

    @Override
    public void connectCameraToSurface(Surface surface) {
        Log.i(TAG, "connectCameraToSurface()");

        // This is probably not cleaned up properly (the CameraManager never really "lets go" of the
        // camera).
        CameraManager cameraManager = new CameraManager(surface);
        cameraManager.connect(mApplicationContext);
    }
}
