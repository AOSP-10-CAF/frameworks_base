/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.signedconfig;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.net.Uri;
import android.os.Bundle;
import android.util.Slog;

import com.android.server.LocalServices;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Signed config service. This is not an Android Service, but just owns a broadcast receiver for
 * receiving package install and update notifications from the package manager.
 */
public class SignedConfigService {

    private static final boolean DBG = false;
    private static final String TAG = "SignedConfig";

    // TODO should these be elsewhere? In a public API?
    private static final String KEY_CONFIG = "android.signedconfig";
    private static final String KEY_CONFIG_SIGNATURE = "android.signedconfig.signature";

    private static class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            new SignedConfigService(context).handlePackageBroadcast(intent);
        }
    }

    private final Context mContext;
    private final PackageManagerInternal mPacMan;

    public SignedConfigService(Context context) {
        mContext = context;
        mPacMan = LocalServices.getService(PackageManagerInternal.class);
    }

    void handlePackageBroadcast(Intent intent) {
        if (DBG) Slog.d(TAG, "handlePackageBroadcast " + intent);
        Uri packageData = intent.getData();
        String packageName = packageData == null ? null : packageData.getSchemeSpecificPart();
        if (DBG) Slog.d(TAG, "handlePackageBroadcast package=" + packageName);
        if (packageName == null) {
            return;
        }
        int userId = mContext.getUser().getIdentifier();
        PackageInfo pi = mPacMan.getPackageInfo(packageName, PackageManager.GET_META_DATA,
                android.os.Process.SYSTEM_UID, userId);
        if (pi == null) {
            Slog.w(TAG, "Got null PackageInfo for " + packageName + "; user " + userId);
            return;
        }
        Bundle metaData = pi.applicationInfo.metaData;
        if (metaData == null) {
            if (DBG) Slog.d(TAG, "handlePackageBroadcast: no metadata");
            return;
        }
        if (metaData.containsKey(KEY_CONFIG)
                && metaData.containsKey(KEY_CONFIG_SIGNATURE)) {
            String config = metaData.getString(KEY_CONFIG);
            String signature = metaData.getString(KEY_CONFIG_SIGNATURE);
            try {
                // Base64 encoding is standard (not URL safe) encoding: RFC4648
                config = new String(Base64.getDecoder().decode(config), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException iae) {
                Slog.e(TAG, "Failed to base64 decode config from " + packageName);
                return;
            }
            if (DBG) {
                Slog.d(TAG, "Got signed config: " + config);
                Slog.d(TAG, "Got config signature: " + signature);
            }
            new SignedConfigApplicator(mContext, packageName).applyConfig(
                    config, signature);
        } else {
            if (DBG) Slog.d(TAG, "Package has no config/signature.");
        }
    }

    /**
     * Register to receive broadcasts from the package manager.
     */
    public static void registerUpdateReceiver(Context context) {
        if (DBG) Slog.d(TAG, "Registering receiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        context.registerReceiver(new UpdateReceiver(), filter);
    }
}
