/*
Copyright (c) 2011, Sony Ericsson Mobile Communications AB

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB nor the names
  of its contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package nl.sense_os.service.external_sensors.smartwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * <p>
 * The extension receiver receives the extension intents and starts the extension service when it
 * arrives. Copied from the sample project in the SmartExtension SDK.
 * </p>
 * <p>
 * Nota bene: this receiver needs to be registered in the application AndroidManifest as follows:
 * 
 * <pre>
 * {@code
 * <receiver android:name="nl.sense_os.service.external_sensors.smartwatch.SmartWatchExtensionReceiver">
 *             <intent-filter>
 *                 <!-- Generic extension intents. -->
 *                 <action android:name="com.sonyericsson.extras.liveware.aef.registration.EXTENSION_REGISTER_REQUEST" />
 *                 <action android:name="com.sonyericsson.extras.liveware.aef.registration.ACCESSORY_CONNECTION"/>
 *                 <action android:name="android.intent.action.LOCALE_CHANGED" />
 * 
 *                 <!-- Notification intents -->
 *                 <action android:name="com.sonyericsson.extras.liveware.aef.notification.VIEW_EVENT_DETAIL"/>
 *                 <action android:name="com.sonyericsson.extras.liveware.aef.notification.REFRESH_REQUEST"/>
 * 
 *                 <!-- Widget intents -->
 *                 <action android:name="com.sonyericsson.extras.aef.widget.START_REFRESH_IMAGE_REQUEST"/>
 *                 <action android:name="com.sonyericsson.extras.aef.widget.STOP_REFRESH_IMAGE_REQUEST"/>
 *                 <action android:name="com.sonyericsson.extras.aef.widget.ONTOUCH"/>
 *                 <action android:name="com.sonyericsson.extras.liveware.extension.util.widget.scheduled.refresh"/>
 * 
 *                 <!-- Control intents -->
 *                 <action android:name="com.sonyericsson.extras.aef.control.START"/>
 *                 <action android:name="com.sonyericsson.extras.aef.control.STOP"/>
 *                 <action android:name="com.sonyericsson.extras.aef.control.PAUSE"/>
 *                 <action android:name="com.sonyericsson.extras.aef.control.RESUME"/>
 *                 <action android:name="com.sonyericsson.extras.aef.control.ERROR"/>
 *                 <action android:name="com.sonyericsson.extras.aef.control.KEY_EVENT"/>
 *                 <action android:name="com.sonyericsson.extras.aef.control.TOUCH_EVENT"/>
 *                 <action android:name="com.sonyericsson.extras.aef.control.SWIPE_EVENT"/>
 * 
 *             </intent-filter>
 *         </receiver>}
 * </pre>
 * 
 * </p>
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class SmartWatchExtensionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(SmartWatchExtensionService.LOG_TAG, "onReceive: " + intent.getAction());
        intent.setClass(context, SmartWatchExtensionService.class);
        context.startService(intent);
    }
}
