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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * Utilities for device registration.
 *
 * Will keep track of the registration token in a private preference.
 */
public class C2DMessaging {
    public static final String EXTRA_SENDER = "sender";
    public static final String EXTRA_APPLICATION_PENDING_INTENT = "app";
    public static final String REQUEST_UNREGISTRATION_INTENT = "com.google.android.c2dm.intent.UNREGISTER";
    public static final String REQUEST_REGISTRATION_INTENT = "com.google.android.c2dm.intent.REGISTER";
    public static final String LAST_REGISTRATION_CHANGE = "last_registration_change";
    public static final String BACKOFF_KEY = "backoff";
    public static final String GSF_PACKAGE = "com.google.android.gsf";


    // package
    static final String PREFERENCE = "com.google.android.c2dm";
    
    private static final long DEFAULT_BACKOFF = 30000;
    private static final String DM_REGISTRATION_KEY = "dm_registration";
    private static final String TAG = "c2dm";

    /**
     * Initiate c2d messaging registration for the current application
     */
    public static void register(Context context, String senderId) {
    	Log.v(TAG, "Sending c2dm registration intent");
        Intent registrationIntent = new Intent(REQUEST_REGISTRATION_INTENT);
        registrationIntent.setPackage(GSF_PACKAGE);
        registrationIntent.putExtra(EXTRA_APPLICATION_PENDING_INTENT, PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        registrationIntent.putExtra(EXTRA_SENDER, senderId);
        context.startService(registrationIntent);
    }

    /**
     * Unregister the application. New messages will be blocked by server.
     */
    public static void unregister(Context context) {
        Intent regIntent = new Intent(REQUEST_UNREGISTRATION_INTENT);
        regIntent.setPackage(GSF_PACKAGE);
        regIntent.putExtra(EXTRA_APPLICATION_PENDING_INTENT, PendingIntent.getBroadcast(context, 0, new Intent(), 0));
        context.startService(regIntent);
    }

    public static String getRegistrationId(Context context) {
        return getPrefs(context).getString(DM_REGISTRATION_KEY, "");
    }

    public static long getLastRegistrationChange(Context context) {
        return getPrefs(context).getLong(LAST_REGISTRATION_CHANGE, 0);
    }

    static long getBackoff(Context context) {
        return getPrefs(context).getLong(BACKOFF_KEY, DEFAULT_BACKOFF);
    }

    static void setBackoff(Context context, long backoff) {
        Editor editor = getPrefs(context).edit();
        editor.putLong(BACKOFF_KEY, backoff);
        editor.commit();
    }

    static void clearRegistrationId(Context context) {
        Editor editor = getPrefs(context).edit();
        editor.remove(DM_REGISTRATION_KEY);
        editor.putLong(LAST_REGISTRATION_CHANGE, System.currentTimeMillis());
        editor.commit();
    }

    public static void setRegistrationId(Context context, String registrationId) {
        Editor editor = getPrefs(context).edit();
        editor.putString(DM_REGISTRATION_KEY, registrationId);
        editor.commit();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE);
    }
}
