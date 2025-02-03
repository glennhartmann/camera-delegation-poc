// Copyright 2025 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package dev.hartmanng.client;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Client.MainActivity";

    public static final String ACTION_SERVER_CALLBACK = "callback";

    // I'll be honest - this is kind of a hack. I don't like the global state. But the callback
    // from the server causes a new MainActivity to start, so this is the easiest way to get it the
    // data it needs. Ideally we would be using a better API involving maybe another aidl or just
    // a Messenger?
    public static SimpleCallback sServerCallback = null;

    private ServerCameraServiceManager mServerCameraServiceManager = null;
    private Button mBindServiceButton;
    private Button mRequestPermissionsButton;
    private Button mStartForegroundServiceButton;
    private Button mDelegateCameraButton;
    private Button mUnbindServiceButton;
    private VideoView mVideoView;
    private boolean mSurfaceIsValid = false;

    private OnClickListener mOnBindServiceButtonClicked = v -> {
        Log.i(TAG, "mOnBindServiceButtonClicked()");

        mServerCameraServiceManager.bindService();
    };

    private OnClickListener mOnRequestPermissionsButtonClicked = v -> {
        Log.i(TAG, "mOnRequestPermissionsButtonClicked()");

        mServerCameraServiceManager.requestDelegatedPermissions();
    };

    private OnClickListener mOnStartForegroundServiceButtonClicked = v -> {
        Log.i(TAG, "mOnStartForegroundServiceButtonClicked()");

        mServerCameraServiceManager.startForegroundService();
    };

    private OnClickListener mOnDelegateCameraButtonClicked = v -> {
        Log.i(TAG, "mOnDelegateCameraButtonClicked()");

        if (!mSurfaceIsValid) {
            Log.e(TAG, "surface is not valid (yet?)");
            return;
        }

        mServerCameraServiceManager.delegateCameraToSurface(mVideoView.getHolder().getSurface());
    };

    private OnClickListener mOnUnbindServiceButtonClicked = v -> {
        Log.i(TAG, "mOnUnbindServiceButtonClicked()");

        mServerCameraServiceManager.unbindService();
        updateButtons();
    };

    public interface SimpleCallback {
        void run();
    }

    // Create a PendingIntent used for the Server app to send a callback to the client once the
    // first permission prompt is finished.
    public static PendingIntent getPendingIntent(Context applicationContext) {
        Intent intent = new Intent(applicationContext, MainActivity.class);
        intent.setAction(ACTION_SERVER_CALLBACK);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return PendingIntent.getActivity(applicationContext, 0 /* requestCode */, intent,
                PendingIntent.FLAG_IMMUTABLE);
    }

    public static void setServerCallback(SimpleCallback callback) {
        sServerCallback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate()");

        // If the Intent that created this activity is a callback from the Server app, then just run
        // the registered callback function and close the activity.
        if (getIntent().getAction().equals(ACTION_SERVER_CALLBACK)) {
            Log.i(TAG, "got server callback");
            if (sServerCallback != null) {
                SimpleCallback callback = sServerCallback;
                sServerCallback = null;
                callback.run();
            } else {
                Log.i(TAG, "sServerCallback == null");
            }
            finish();
            return;
        }

        // Layout, style, theme, etc.
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mServerCameraServiceManager = new ServerCameraServiceManager(this /* contextWrapper */,
                this::updateButtons, this::updateButtons);

        mBindServiceButton = (Button) findViewById(R.id.bindServiceButton);
        mBindServiceButton.setOnClickListener(mOnBindServiceButtonClicked);

        mRequestPermissionsButton = (Button) findViewById(R.id.requestPermissionsButton);
        mRequestPermissionsButton.setOnClickListener(mOnRequestPermissionsButtonClicked);

        mStartForegroundServiceButton = (Button) findViewById(R.id.startForegroundServiceButton);
        mStartForegroundServiceButton.setOnClickListener(mOnStartForegroundServiceButtonClicked);

        mDelegateCameraButton = (Button) findViewById(R.id.delegateCameraButton);
        mDelegateCameraButton.setOnClickListener(mOnDelegateCameraButtonClicked);

        mUnbindServiceButton = (Button) findViewById(R.id.unbindServiceButton);
        mUnbindServiceButton.setOnClickListener(mOnUnbindServiceButtonClicked);

        updateButtons();

        mVideoView = (VideoView) findViewById(R.id.videoView);

        mVideoView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                Log.i(TAG, "SurfaceHolder.Callback.surfaceCreated()");

                mSurfaceIsValid = true;
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1,
                                       int i2) {
                Log.i(TAG, "SurfaceHolder.Callback.surfaceChanged()");
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                Log.i(TAG, "SurfaceHolder.Callback.surfaceDestroyed()");

                mSurfaceIsValid = false;
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.i(TAG, "onStop()");

        if (mServerCameraServiceManager != null) {
            mServerCameraServiceManager.unbindService();
            mServerCameraServiceManager = null;
        }
    }

    private void updateButtons() {
        if (mBindServiceButton == null || mRequestPermissionsButton == null ||
                mStartForegroundServiceButton == null || mDelegateCameraButton == null ||
                mUnbindServiceButton == null) {
            Log.e(TAG, "buttons uninitialized?");
            return;
        }

        mBindServiceButton.setEnabled(!mServerCameraServiceManager.serviceExists());
        mRequestPermissionsButton.setEnabled(mServerCameraServiceManager.serviceExists());
        mStartForegroundServiceButton.setEnabled(true);
        mDelegateCameraButton.setEnabled(mServerCameraServiceManager.serviceExists());
        mUnbindServiceButton.setEnabled(mServerCameraServiceManager.serviceExists());
    }
}