// Copyright 2025 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package dev.hartmanng.server;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Server.MainActivity";

    private VideoView mVideoView;
    private CameraManager mCameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mVideoView = (VideoView) findViewById(R.id.videoView);
        mVideoView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                Log.i(TAG, "SurfaceHolder.Callback.surfaceCreated()");

                // Attach the camera to the local surface as soon as the Activity starts up. Not
                // very exciting - this app mostly exists for the Service it exposes, not for this.
                mCameraManager = new CameraManager(surfaceHolder.getSurface());
                mCameraManager.connect(getApplicationContext());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1,
                                       int i2) {
                Log.i(TAG, "SurfaceHolder.Callback.surfaceChanged()");
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                Log.i(TAG, "SurfaceHolder.Callback.surfaceDestroyed()");
            }
        });

        try {
            PermissionRequestActivity.getPendingIntent(getApplicationContext(),
                    android.Manifest.permission.CAMERA).send();
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "pending intent canceled: " + e);
        }
    }
}