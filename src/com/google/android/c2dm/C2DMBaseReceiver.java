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

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * Base class for C2D message receiver. Includes constants for the
 * strings used in the protocol.
 */
public abstract class C2DMBaseReceiver extends IntentService {
    private static final String ACTION_REGISTRATION_CALLBACK_INTENT = "com.google.android.c2dm.intent.REGISTRATION";
    private static final String ACTION_C2DM_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";

    // Logging tag
    private static final String TAG = "c2dm";

    // Extras in the registration callback intents.
    private static final String EXTRA_UNREGISTERED = "unregistered";
    private static final String EXTRA_ERROR = "error";
    private static final String EXTRA_REGISTRATION_ID = "registration_id";
    private static final String EXTRA_RETRY = "retry_register";

    /**
     * The device can't read the response, or there was a 500/503 from the server that can be retried later.
     * The application should use exponential back off and retry.
     */
    private static final String ERR_SERVICE_NOT_AVAILABLE = "SERVICE_NOT_AVAILABLE";

    // wakelock
    private static final String WAKELOCK_KEY = C2DMBaseReceiver.class.getSimpleName();

    private static PowerManager.WakeLock mWakeLock;
    private final String mSenderId;

    /**
     * The C2DMReceiver class must create a no-arg constructor and pass the
     * sender id to be used for registration.
     *
     * @param senderId an email address of the registered sender ID
     */
    public C2DMBaseReceiver(String senderId) {
        // senderId is used as base name for threads, etc.
        super(senderId);
        mSenderId = senderId;
    }

    @Override
    public void onHandleIntent(Intent intent) {
    	Log.v(TAG, "C2DMBaseReceiver got a message");
        try {
            Context context = getApplicationContext();
            if (intent.getAction().equals(ACTION_REGISTRATION_CALLBACK_INTENT)) {
                if (intent.getBooleanExtra(EXTRA_RETRY, false)) {
                    C2DMessaging.register(context, mSenderId);
                } else {
                    handleRegistration(context, intent);
                }
            } else if (intent.getAction().equals(ACTION_C2DM_RECEIVE)) {
                onMessage(context, intent);
            }
        } finally {
            //  Release the power lock, so phone can get back to sleep.
            // The lock is reference counted by default, so multiple
            // messages are ok.

            // If the onMessage() needs to spawn a thread or do something else,
            // it should use it's own lock.
            mWakeLock.release();
        }
    }

    /**
     * Called from the broadcast receiver.
     * Will process the received intent, call handleMessage(), registered(), etc.
     * in background threads, with a wake lock, while keeping the service
     * alive.
     *
     * @param context used to start the service
     * @param intent  the intent received by the C2DM broadcast receiver
     * @param impl    the class implementing {@link C2DMBaseReceiver}
     */
    static void runIntentInService(Context context, Intent intent, Class<?> impl) {
        if (mWakeLock == null) {
            // This is called from BroadcastReceiver, there is no init.
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_KEY);
        }
        mWakeLock.acquire();

        intent.setClass(context, impl);
        context.startService(intent);
    }

    /**
     * Called when the device has been unregistered.
     */
    public void onUnregistered(Context context) {
    }

    /**
     * Called when a cloud message has been received.
     */
    protected abstract void onMessage(Context context, Intent intent);


    /**
     * Called on registration error. Override to provide better
     * error messages.
     * <p/>
     * This is called in the context of a Service - no dialog or UI.
     *
     * @param context
     * @param errorId refer to http://code.google.com/android/c2dm/index.html#handling_reg
     */
    public abstract void onError(Context context, String errorId);

    /**
     * Called when a registration token has been received.
     */
    public abstract void onRegistration(Context context, String registrationId);


    private void handleRegistration(final Context context, Intent intent) {
        final String registrationId = intent.getStringExtra(EXTRA_REGISTRATION_ID);
        String error = intent.getStringExtra(EXTRA_ERROR);
        String removed = intent.getStringExtra(EXTRA_UNREGISTERED);

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "dmControl: registrationId = " + registrationId + ", error = " + error + ", removed = " + removed);
        }

        if (removed != null) {
            // Remember we are unregistered
            C2DMessaging.clearRegistrationId(context);
            onUnregistered(context);
            return;
        } else if (error != null) {
            // we are not registered, can try again
            C2DMessaging.clearRegistrationId(context);
            // Registration failed
            Log.e(TAG, "Registration error " + error);
            onError(context, error);
            if (ERR_SERVICE_NOT_AVAILABLE.equals(error)) {
                long backoffTimeMs = C2DMessaging.getBackoff(context);

                Log.d(TAG, "Scheduling registration retry, backoff = " + backoffTimeMs);
                Intent retryIntent = new Intent(ACTION_REGISTRATION_CALLBACK_INTENT);
                retryIntent.putExtra(EXTRA_RETRY, true);
                PendingIntent retryPIntent = PendingIntent.getBroadcast(context,
                        0 /*requestCode*/, retryIntent, 0 /*flags*/);

                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + backoffTimeMs, retryPIntent);

                // Next retry should wait longer.
                backoffTimeMs *= 2;
                C2DMessaging.setBackoff(context, backoffTimeMs);
            }
        } else {
            try {
            	//Hold saving registration_id until it registered in common sense
                //C2DMessaging.setRegistrationId(context, registrationId);
                onRegistration(context, registrationId);
            } catch (Exception ex) {
                Log.e(TAG, "Registration error " + ex.getMessage());
            }
        }
    }
}
