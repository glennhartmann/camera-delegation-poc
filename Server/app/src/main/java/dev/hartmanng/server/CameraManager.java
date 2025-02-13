// Copyright 2025 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package dev.hartmanng.server;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.util.Log;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import androidx.annotation.NonNull;

public class CameraManager implements Executor {
    private static final String TAG = "Server.CameraManager";

    private Handler mHandler;
    private final Surface mSurface;

    private final CameraDevice.StateCallback mCameraDeviceStateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    Log.i(TAG, "mCameraDeviceStateCallback.onOpened()");

                    attemptCreateCaptureSession(cameraDevice, Collections.singletonList(
                            new OutputConfiguration(mSurface)));
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    Log.i(TAG, "mCameraDeviceStateCallback.onDisconnected()");
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    Log.i(TAG, "mCameraDeviceStateCallback.onError(): " + i);
                }
            };

    private final CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.i(TAG, "mCameraCaptureSessionStateCallback.onConfigured()");

                    CaptureRequest.Builder captureRequestBuilder =
                            attemptCreateCaptureRequestBuilder(cameraCaptureSession);
                    assert captureRequestBuilder != null;

                    captureRequestBuilder.addTarget(mSurface);
                    attemptSetRepeatingRequest(cameraCaptureSession, captureRequestBuilder.build());
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.i(TAG, "mCameraCaptureSessionStateCallback.onConfigureFailed()");
                }
            };

    public CameraManager(Surface surface) {
        mSurface = surface;
    }

    public void connect(Context applicationContext) {
        android.hardware.camera2.CameraManager cameraManager =
                (android.hardware.camera2.CameraManager) applicationContext.getSystemService(
                        Context.CAMERA_SERVICE);

        String[] cameraIds;
        try {
            cameraIds = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.e(TAG, "camera access exception: " + e);
            return;
        }

        if (cameraIds.length == 0) {
            Log.e(TAG, "no cameras found");
            return;
        }

        HandlerThread handlerThread = new HandlerThread("CameraThread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        try {
            // Obviously there are better ways to select a camera than just picking the first one.
            // But this is good enough for demo purposes.
            cameraManager.openCamera(cameraIds[0],
                    mCameraDeviceStateCallback,
                    mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "connect(): camera access exception: " + e);
        } catch (SecurityException e) {
            Log.e(TAG, "connect(): security exception: " + e);
        }
    }

    // Implements Executor interface.
    @Override
    public void execute(Runnable command) {
        if (!mHandler.post(command)) {
            Log.e(TAG, "mHandler failed to post");
        }
    }

    private void attemptCreateCaptureSession(CameraDevice cameraDevice,
                                             List<OutputConfiguration> outputConfigs) {
        try {
            cameraDevice.createCaptureSession(
                    new SessionConfiguration(SessionConfiguration.SESSION_REGULAR, outputConfigs,
                            CameraManager.this /* executor */, mCameraCaptureSessionStateCallback));
        } catch (CameraAccessException e) {
            Log.e(TAG, "attemptCreateCaptureSession(): camera access exception: " + e);
        }
    }

    private CaptureRequest.Builder attemptCreateCaptureRequestBuilder(
            CameraCaptureSession cameraCaptureSession) {
        try {
            return cameraCaptureSession.getDevice()
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            Log.e(TAG, "attemptCreateCaptureRequestBuilder(): camera access exception: " + e);
        }
        return null;
    }

    private void attemptSetRepeatingRequest(CameraCaptureSession cameraCaptureSession,
                                            CaptureRequest captureRequest) {
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequest, null /* captureCallback */,
                    mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "attemptSetRepeatingRequest(): camera access exception: " + e);
        }
    }
}
