// Copyright 2025 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package dev.hartmanng.server;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

// Based on
// https://source.chromium.org/chromium/chromium/src/+/main:chrome/android/webapk/shell_apk/src/org/chromium/webapk/shell_apk/NotificationPermissionRequestActivity.java.
public class PermissionRequestActivity extends Activity {
    private static final String TAG = "Server.PermissionRequestActivity";

    public static final String EXTRA_PERMISSION = "permission";

    public static PendingIntent getPendingIntent(Context applicationContext, String permission) {
        Intent intent = new Intent(applicationContext, PermissionRequestActivity.class);
        intent.putExtra(EXTRA_PERMISSION, permission);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return PendingIntent.getActivity(applicationContext, 0 /* requestCode */, intent,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "PermissionRequestActivity.onCreate()");

        ActivityCompat.requestPermissions(this /* activity */,
                new String[]{getIntent().getStringExtra(EXTRA_PERMISSION) /* assume non-null */},
                0 /* requestCode */);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "PermissionRequestActivity.onRequestPermissionsResult(): " + requestCode);

        for (int i = 0; i < permissions.length; i++) {
            Log.i(TAG, permissions[i] +
                    (grantResults[i] == PackageManager.PERMISSION_GRANTED ?
                            " granted" : " denied"));
        }

        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.i(TAG, "PermissionRequestActivity.onStop()");
    }
}
