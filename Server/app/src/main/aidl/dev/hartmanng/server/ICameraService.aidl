// Copyright 2025 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// ICameraService.aidl
package dev.hartmanng.server;

import android.app.PendingIntent;
import android.view.Surface;

interface ICameraService {
    PendingIntent getRequestPermissionPendingIntent(String permission);
    void connectCameraToSurface(in Surface surface);
}