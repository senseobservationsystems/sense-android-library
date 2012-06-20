/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.c2dm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.util.*;

/**
 * @author cristian
 */
public class C2DMBroadcastReceiver extends BroadcastReceiver {

    private static final String DATA_IMPLEMENTATION = "com.google.android.c2dm.implementation";
    private static final Map<String, Class<?>> mCache = new HashMap<String, Class<?>>();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!mCache.containsKey(context.getPackageName())) {
            mCache.put(context.getPackageName(), getC2DMImplementation(context));
        }
        Class<?> implementation = mCache.get(context.getPackageName());
        C2DMBaseReceiver.runIntentInService(context, intent, implementation);
        setResult(Activity.RESULT_OK, null /* data */, null /* extra */);
    }

    private Class<?> getC2DMImplementation(Context context) {
        // let's first check the required permissions are set
        PackageManager pm = context.getPackageManager();
        verifyPermissions(pm, context.getPackageName());

        // get the class name implementation
        Class<?> implementation;
        try {
            ComponentName componentName = new ComponentName(context, C2DMBroadcastReceiver.class);
            ActivityInfo info = pm.getReceiverInfo(componentName, PackageManager.GET_META_DATA);
            Bundle bundle = info.metaData;
            String implementationClassName = bundle.getString(DATA_IMPLEMENTATION);
            implementation = Class.forName(implementationClassName);
            boolean extendsC2DMBaseReceiver = false;
            Class<?> superClass;
            while ((superClass = implementation.getSuperclass()) != Object.class) {
                if (superClass == C2DMBaseReceiver.class) {
                    extendsC2DMBaseReceiver = true;
                    break;
                }
            }
            if (!extendsC2DMBaseReceiver) {
                throw new IllegalStateException("Implemenation class must extend " + C2DMBaseReceiver.class.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("You did not supply a meta-data with the receiver implementation. Refer to the documentation to know how to use this BroadcastReceiver: https://github.com/casidiablo/c2dm-library");
        }

        return implementation;
    }

    private void verifyPermissions(PackageManager pm, String packageName) {
        PackageInfo permissionsInfo = null;
        try {
            permissionsInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (permissionsInfo != null) {
            List requiredPermissions = new ArrayList(Arrays.asList("android.permission.INTERNET",
                    "android.permission.WAKE_LOCK", "com.google.android.c2dm.permission.RECEIVE"));
            for (String permission : permissionsInfo.requestedPermissions) {
                if (requiredPermissions.contains(permission)) {
                    requiredPermissions.remove(permission);
                }
            }
            if (requiredPermissions.size() > 0) {
                throw new IllegalStateException("Permissions missing: " + requiredPermissions + "; if you are in doubt, refer to the documentation: https://github.com/casidiablo/c2dm-library");
            }
        }
    }
}