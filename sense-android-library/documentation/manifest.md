# AndroidManifest {#androidmanifest}
The Android manifest defines the permissions, services and intent handlers used by the Sense Android library. 

# Permissions {#permissions}
The Sense Android library defines a lot of permissions because the library should be capable of accessing many different resources for  the wide variety of sensor output streams. It is not recommended to leave any of these permission out of your application's manifest, but if you want to do so then please make sure that all the required sensors produce valid output.  

**The AndroidManifest.xml permissions:**

~~~
	< !-- REQUEST GENERAL PERMISSIONS FOR MAIN APP FUNCTIONALITY -->
	<uses-permission android:name="android.permission.INTERNET" />
	<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
	<uses-permission android:name="android.permission.VIBRATE" />
	<uses-permission android:name="android.permission.WAKE_LOCK" />
	<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
	<uses-permission android:name="android.permission.GET_TASKS" />
	< !-- REQUEST ACCESS TO NETWORKING FEATURES FOR NETWORK SCAN SENSORS -->
	<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
	<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
	<uses-permission android:name="android.permission.BLUETOOTH" />
	<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
	<uses-permission android:name="android.permission.NFC" />
	< !-- REQUEST ACCESS TO LOCATION SENSORS -->
	<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
	<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />  
	< !-- REQUEST ACCESS TO AUDIO AND VIDEO FOR AMBIENCE SENSORS -->
	<uses-permission android:name="android.permission.RECORD_AUDIO" />
	<uses-permission android:name="android.permission.CAMERA" />
	< !-- REQUEST ACCESS TO GENERAL PHONE STATE INFORMATION FOR PHONE STATE SENSORS -->
	<uses-permission android:name="android.permission.PROCESS_OUTGOING_CALLS" />
	<uses-permission android:name="android.permission.READ_PHONE_STATE" />    
	< !-- REQUEST PERMISSION TO RECEIVE PUSHED (GCM) MESSAGES -->
	<uses-permission android:name="nl.sense_os.demo.permission.GCM_MESSAGE" />
	<uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
	< !-- DEFINE OUR OWN GCM PERMISSION -->
	<permission android:name="nl.sense_os.demo.permission.GCM_MESSAGE" android:protectionLevel="signature" />

~~~

# Features {#features}
The library defines together with the permissions also some features which is may require to work properly. Setting a feature as required makes the application not show up as compatible in the Play Store for devices which doesn't have this. Make sure to only set the features to required which the application can't do without.

**The AndroidManifest.xml features:**  

~~~

	< !-- DEFINE REQUIRED FEATURES FOR AMBIENCE SENSORS --> 
	<uses-feature android:name="android.hardware.microphone" android:required="true" />
	< !-- IMPLIED BY PERMISSION CAMERA, BUT NOT REQUIRED -->
	<uses-feature android:name="android.hardware.camera" android:required="false" />
	< !-- DEFINE REQUIRED FEATURES FOR PHONE STATE SENSORS -->
	<uses-feature android:name="android.hardware.touchscreen" android:required="true" />
	< !-- IMPLIED BY PERMISSION PROCESS OUTGOING_CALLS, BUT NOT REQUIRED -->
	<uses-feature android:name="android.hardware.telephony" android:required="false" />    
	< !-- DEFINE REQUIRED FEATURES FOR NETWORK SCAN SENSORS -->
	<uses-feature android:name="android.hardware.wifi" android:required="true" />
	<uses-feature android:name="android.hardware.bluetooth" android:required="true" />
	< !-- IMPLIED BY PERMISSION NFC, BUT NOT REQUIRED -->
	<uses-feature android:name="android.hardware.nfc" android:required="false" />    

~~~

# Services {#services}
The Sense Android library defines several services to for instance, run in the background, receive new sensor data and synchronize with the back-end. In order to reduce conflicts with other applications when using the Sense Android library on the same device custom intent strings should be defined in `res/values/strings.xml`. For a complete example of an AndroidManifest.xml file please have a look at the sample application.<br>

An example of a strings.xml file:<br>
~~~
<resources>
    <string name="app_name">Sense Demo</string>

    < !-- OVERRIDE INTENT ACTIONS FOR BUNDLED SENSE PLATFORM (ALSO CHANGE IN MANIFEST!!) -->
    <string name="action_sense_service">nl.sense_os.demo.SENSE_SERVICE</string>
    <string name="action_sense_new_data">nl.sense_os.demo.NEW_DATA</string>
    <string name="action_sense_send_data">nl.sense_os.demo.SEND_DATA</string>
    <string name="action_sense_alive_check_alarm">nl.sense_os.demo.CHECK_ALIVE</string>
    <string name="action_sense_new_requirements">nl.sense_os.demo.NEW_REQUIREMENTS</string>
    <string name="action_widget_update">nl.sense_os.demo.UPDATE_APP_WIDGET</string>
    <string name="local_storage_authority">nl.sense_os.demo.LocalStorage</string>
   
    < !-- NOTIFICATION RESOURCES -->
    <string name="stat_notify_action">nl.sense_os.demo.NOTIFICATION</string>
    <string name="stat_notify_title">Sense Demo</string>
    <string name="stat_notify_content_off_loggedin">Measurement stopped, logged in as \'%s\'</string>
    <string name="stat_notify_content_off_loggedout">Measurement stopped, no connection with server </string>
    <string name="stat_notify_content_on_loggedin">"Measurement active, logged in as \'%s\'"</string>
    <string name="stat_notify_content_on_loggedout">Measurement active, no connection with server</string>
</resources>
~~~

**The AndroidManifest.xml services:**  

~~~
		< !-- MAIN SERVICE THAT MANAGES SENSING -->
        <service
            android:name="nl.sense_os.service.SenseService"
            android:exported="false" >
            <intent-filter>

                <!-- MAKE SURE YOU USE UNIQUE ACTIONS FOR YOUR OWN APP (SEE res/strings.xml) -->
                <action android:name="nl.sense_os.demo.SENSE_SERVICE" />
            </intent-filter>
        </service>
        < !-- SERVICE TO BUFFER AND SEND DATA TO COMMONSENSE -->
        <service
            android:name="nl.sense_os.service.MsgHandler"
            android:exported="false" >
            <intent-filter>

                <!-- MAKE SURE YOU USE UNIQUE ACTIONS FOR YOUR OWN APP (SEE res/strings.xml) -->
                <action android:name="nl.sense_os.demo.NEW_DATA" />
                <action android:name="nl.sense_os.demo.SEND_DATA" />
            </intent-filter>
        </service>
        < !-- SERVICE THAT CHECKS SENSOR REGISTRATION WITH COMMONSENSE -->
        <service android:name="nl.sense_os.service.commonsense.DefaultSensorRegistrationService" />
        < !-- SERVICE THAT HANDLES GCM MESSAGES FROM COMMONSENSE -->
        < !-- CAN BE REMOVED IF NOT USED -->
        <service android:name="nl.sense_os.service.push.GCMReceiver" />
        < !-- SERVICE THAT UPDATES THE SENSOR CONFIGURATION WHEN REQUIREMENTS CHANGE -->
        < !-- CAN BE REMOVED IF NOT USED -->
        <service android:name="nl.sense_os.service.configuration.ConfigurationService" />
        < !-- DUMMY SERVICE TO HANDLE CHANGES IN THE SENSE STATE (DOES NOTHING BY DEFAULT) -->
        < !-- REPLACE THIS SERVICE IF YOU WANT TO IMPLEMENT YOUR OWN APP WIDGET -->
        <service
            android:name="nl.sense_os.service.appwidget.DummyAppWidgetService"
            android:exported="false" >
            <intent-filter>

                <!-- MAKE SURE YOU USE UNIQUE ACTIONS FOR YOUR OWN APP (SEE res/strings.xml) -->
                <action android:name="nl.sense_os.demo.UPDATE_APP_WIDGET" />
            </intent-filter>
        </service>
~~~
**The AndroidManifest.xml receivers:**  
~~~
        < ! -- BROADCAST RECEIVER THAT WAKES UP THE APP AGAIN WHEN THE PHONE REBOOTS -->
        <receiver
            android:name="nl.sense_os.service.BootRx"
            android:exported="false" >
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />

                <category android:name="android.intent.category.HOME" />
            </intent-filter>
        </receiver>
        < !-- BROADCAST RECEIVER THAT MAKES SURE THE SENSE SERVICE IS RESTARTED IF IT IS KILLED -->
        <receiver
            android:name="nl.sense_os.service.AliveChecker"
            android:exported="false" >
            <intent-filter>

                <!-- MAKE SURE YOU USE UNIQUE ACTIONS FOR YOUR OWN APP (SEE res/strings.xml) -->
                <action android:name="nl.sense_os.demo.CHECK_ALIVE" />
            </intent-filter>
        </receiver>
        < !-- BROADCAST RECEIVER FOR CHANGES IN THE NETWORK STATE -->
        <receiver
            android:name="nl.sense_os.service.NetworkMonitor"
            android:exported="false" >
            <intent-filter>
                <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
            </intent-filter>
        </receiver>
        < !-- BROADCAST RECEIVER FOR SCHEDULED SAMPLE EXECUTION ALARMS -->
        <receiver android:name="nl.sense_os.service.scheduler.ExecutionAlarmReceiver" />
        < !-- BROADCAST RECEIVER FOR GCM MESSAGES -->
        < !-- CAN BE REMOVED IF NOT USED -->
        <receiver
            android:name="nl.sense_os.service.push.GCMBroadcastReceiver"
            android:permission="com.google.android.c2dm.permission.SEND" >
            <intent-filter>
                <action android:name="com.google.android.c2dm.intent.RECEIVE" />
                <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
                <!-- MAKE SURE YOU USE UNIQUE ACTIONS FOR YOUR OWN APP (SEE res/strings.xml) -->
                <category android:name="nl.sense_os.demo" />
            </intent-filter>
        </receiver>
        < !-- BROADCAST RECEIVER FOR PUSHED REQUIREMENT CHANGES -->
        < !-- CAN BE REMOVED IF NOT USED -->
        <receiver
            android:name="nl.sense_os.service.configuration.RequirementReceiver"
            android:exported="false" >
            <intent-filter>

                < !-- MAKE SURE YOU USE UNIQUE ACTIONS FOR YOUR OWN APP (SEE res/strings.xml) -->
                <action android:name="nl.sense_os.demo.NEW_REQUIREMENTS" />
            </intent-filter>
        </receiver>
 ~~~

# Implicit SenseService Binding {#implicitbinding}
To make the binding process to the SenseService implicit and easier for your application, you can define your application as a SenseApplication in the AndroidManifest. By retrieving the context of your application an automatic bind to the SenseService will be initiated and a SensePlatform object can be obtained. Because the bind process takes place on the background make sure the SensePlatform object has a bound SenseService before using it.

**Retrieve an implicit bound SenseService**:
~~~
SenseApplication mApplication = (SenseApplication) context.getApplicationContext();
SensePlatform sensePlatform = mApplication.getSensePlatform();
try{
	sensePlatform.checkSenseService();
}catch(IllegalStateException ise){
	...
};
~~~

**Define as SenseApplication in the AndroidManifest.xml:**  
~~~
	< application 
	...
	android:name="nl.sense_os.platform.SenseApplication"
	...
	>
~~~

# Google Play Service {#googleplayservices}
For getting an accurate location with out consuming too much battery the library uses the Fused Location Provider from the Google Play Services. When you your application uses location based data you should implement a check whether the phone has the Google Play Services installed, and notify the user to install it if necessary.

~~~
< !-- GOOGLE PLAY SERVICES USED FOR THE FUSED LOCATION PROVIDER -->
<meta-data android:name="com.google.android.gms.version" android:value="@integer/google_play_services_version" />
~~~
