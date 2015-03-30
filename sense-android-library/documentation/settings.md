# Settings {#sense_settings}

Sense Library provide several settings to alter the library behaviour. Sense Library store these settings in three separate preferences files:
* Main preferences file,
contains the settings for the sensors and sample/sync rates

* Authentication preferences file,
contains all user-related stuff like login username, password, session, and cached sensor IDs

* Status Preferences file,
contain settings about which sensors are activated

All available settings keys for each of preferences file is defined in nl.sense_os.service.constants.SensePrefs class.

Application can access the setttings directly from preferences file, although we encourage developers to access settings through helper functions in SenseServiceStub (see [Access settings through SenseServiceStub](#access_settings)). Here is an example of accessing settings directly from preferences file.

~~~java
SharedPreferences mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, MODE_PRIVATE);
boolean useGps = mainPrefs.setBoolean(SensePrefs.Main.Advanced.DEV_MODE, true);

SharedPreferences authPrefs = getSharedPreferences(SensePrefs.AUTH_PREFS, MODE_PRIVATE);
String username = mainPrefs.getString(SensePrefs.Auth.LOGIN_USERNAME, "");
~~~

# Access settings through SenseServiceStub {#access_settings}
SenseServiceStub provide helper functions to access settings for Sense Library. There is a helper function for each of available data types, each with setter and getter function.

* Boolean : nl.sense_os.service.SenseServiceStub.getPrefBool / nl.sense_os.service.SenseServiceStub.setPrefBool
* Float   : nl.sense_os.service.SenseServiceStub.getPrefFloat / nl.sense_os.service.SenseServiceStub.setPrefFloat
* Integer : nl.sense_os.service.SenseServiceStub.getPrefInt / nl.sense_os.service.SenseServiceStub.setPrefInt
* Long    : nl.sense_os.service.SenseServiceStub.getPrefLong / nl.sense_os.service.SenseServiceStub.setPrefLong
* String  : nl.sense_os.service.SenseServiceStub.getPrefString / nl.sense_os.service.SenseServiceStub.setPrefString

Using helper functions from SenseServiceStub, there is no need to check and open corresponding preferences file for each available settings anymore. Here is an example of accessing settings using SenseServiceStub.

~~~
SenseServiceStub senseService = mApplication.getSenseService();

senseService.setPrefBool(Advanced.ENCRYPT_CREDENTIAL, true);
senseService.setPrefString(SensePrefs.Main.Advanced.ENCRYPT_CREDENTIAL_SALT, "some salt !@#$%XCBCV");
senseService.setPrefBool(SensePrefs.Main.Advanced.ENCRYPT_DATABASE, true);
senseService.setPrefString(SensePrefs.Main.Advanced.ENCRYPT_DATABASE_SALT, "some salt !@#$%XCBCV");

String username = senseService.getPrefString(SensePrefs.Auth.LOGIN_USERNAME, "");
~~~

# Sample and sync rate {#sample_sync_rate}
With the preferences nl.sense_os.service.constants.SensePrefs.Main.SAMPLE_RATE and nl.sense_os.service.constants.SensePrefs.Main.SYNC_RATE the sensor sample and data upload interval can be set respectively. By default the NORMAL sync and sample rates are selected, however for most applications it's recommended to use the BALANCED sample rate and lowest sync rate e.g. ECO_MODE.
 
The different sample rates:

* RARELY<br> 
every 15 minutes
* BALANCED<br>
every 3 minutes, 5 minutes for location sensors
* NORMAL<br>
every minute, 5 minuts for location sensors
* OFTEN<br>
every 10 seconds, 30 seconds for location sensors, 5 seconds for external and motion sensors

The different sync rates:

* ECO_MODE<br>
every 30 minutes
* RARELY<br>
every 15 minutes
* NORMAL<br>
every 5 minutes
* OFTEN<br>
every minute
* REAL_TIME<br>
immediately, local data buffering is disabled

Example for setting the sample and sync rate:  
~~~
        senseService.setPrefString(SensePrefs.Main.SAMPLE_RATE, SensePrefs.Main.SampleRate.BALANCED);
        senseService.setPrefString(SensePrefs.Main.SYNC_RATE, SensePrefs.Main.SyncRate.ECO_MODE);
~~~

# Settings list {#settings_list}

* nl.sense_os.service.constants.SensePrefs.Main
  * nl.sense_os.service.constants.SensePrefs.Main.Advanced
    * nl.sense_os.service.constants.SensePrefs.Main.Advanced.AGOSTINO
    * nl.sense_os.service.constants.SensePrefs.Main.Advanced.COMPRESS
    * nl.sense_os.service.constants.SensePrefs.Main.Advanced.DEV_MODE
    * nl.sense_os.service.constants.SensePrefs.Main.Advanced.ENCRYPT_CREDENTIAL
    * nl.sense_os.service.constants.SensePrefs.Main.Advanced.ENCRYPT_CREDENTIAL_SALT
    * nl.sense_os.service.constants.SensePrefs.Main.Advanced.ENCRYPT_DATABASE
    * nl.sense_os.service.constants.SensePrefs.Main.Advanced.ENCRYPT_DATABASE_SALT
    * nl.sense_os.service.constants.SensePrefs.Main.Advanced.LOCAL_STORAGE
    * nl.sense_os.service.constants.SensePrefs.Main.Advanced.LOCATION_FEEDBACK
    * nl.sense_os.service.constants.SensePrefs.Main.Advanced.MOBILE_INTERNET_ENERGY_SAVING_MODE
    * nl.sense_os.service.constants.SensePrefs.Main.Advanced.RETENTION_HOURS
    * nl.sense_os.service.constants.SensePrefs.Main.Advanced.USE_COMMONSENSE
    * nl.sense_os.service.constants.SensePrefs.Main.Advanced.WIFI_UPLOAD_ONLY
  * nl.sense_os.service.constants.SensePrefs.Main.Ambience
    * nl.sense_os.service.constants.SensePrefs.Main.Ambience.AUDIO_SPECTRUM
    * nl.sense_os.service.constants.SensePrefs.Main.Ambience.BURSTMODE
    * nl.sense_os.service.constants.SensePrefs.Main.Ambience.CAMERA_LIGHT
    * nl.sense_os.service.constants.SensePrefs.Main.Ambience.DONT_UPLOAD_BURSTS
    * nl.sense_os.service.constants.SensePrefs.Main.Ambience.HUMIDITY
    * nl.sense_os.service.constants.SensePrefs.Main.Ambience.LIGHT
    * nl.sense_os.service.constants.SensePrefs.Main.Ambience.MAGNETIC_FIELD
    * nl.sense_os.service.constants.SensePrefs.Main.Ambience.MIC
    * nl.sense_os.service.constants.SensePrefs.Main.Ambience.PRESSURE
    * nl.sense_os.service.constants.SensePrefs.Main.Ambience.RECORD_AUDIO
    * nl.sense_os.service.constants.SensePrefs.Main.Ambience.RECORD_TWO_SIDED_VOICE_CALL
    * nl.sense_os.service.constants.SensePrefs.Main.Ambience.TEMPERATURE
  * nl.sense_os.service.constants.SensePrefs.Main.DevProx
    * nl.sense_os.service.constants.SensePrefs.Main.DevProx.BLUETOOTH
    * nl.sense_os.service.constants.SensePrefs.Main.DevProx.NFC
    * nl.sense_os.service.constants.SensePrefs.Main.DevProx.WIFI
  * nl.sense_os.service.constants.SensePrefs.Main.External
    * nl.sense_os.service.constants.SensePrefs.Main.External.MyGlucoHealth
    * nl.sense_os.service.constants.SensePrefs.Main.External.OBD2Sensor
    * nl.sense_os.service.constants.SensePrefs.Main.External.TanitaScale
    * nl.sense_os.service.constants.SensePrefs.Main.External.ZephyrBioHarness
    * nl.sense_os.service.constants.SensePrefs.Main.External.ZephyrHxM
  * nl.sense_os.service.constants.SensePrefs.Main.Location
    * nl.sense_os.service.constants.SensePrefs.Main.Location.AUTO_GPS
    * nl.sense_os.service.constants.SensePrefs.Main.Location.FUSED_PROVIDER
    * nl.sense_os.service.constants.SensePrefs.Main.Location.FUSED_PROVIDER_PRIORITY
    * nl.sense_os.service.constants.SensePrefs.Main.Location.GPS
    * nl.sense_os.service.constants.SensePrefs.Main.Location.NETWORK
    * nl.sense_os.service.constants.SensePrefs.Main.Location.TIME_ZONE
  * nl.sense_os.service.constants.SensePrefs.Main.Motion
    * nl.sense_os.service.constants.SensePrefs.Main.Motion.ACCELEROMETER
    * nl.sense_os.service.constants.SensePrefs.Main.Motion.BURST_DURATION
    * nl.sense_os.service.constants.SensePrefs.Main.Motion.BURSTMODE
    * nl.sense_os.service.constants.SensePrefs.Main.Motion.DONT_UPLOAD_BURSTS
    * nl.sense_os.service.constants.SensePrefs.Main.Motion.EPIMODE
    * nl.sense_os.service.constants.SensePrefs.Main.Motion.FALL_DETECT
    * nl.sense_os.service.constants.SensePrefs.Main.Motion.FALL_DETECT_DEMO
    * nl.sense_os.service.constants.SensePrefs.Main.Motion.GYROSCOPE
    * nl.sense_os.service.constants.SensePrefs.Main.Motion.LINEAR_ACCELERATION
    * nl.sense_os.service.constants.SensePrefs.Main.Motion.MOTION_ENERGY
    * nl.sense_os.service.constants.SensePrefs.Main.Motion.ORIENTATION
    * nl.sense_os.service.constants.SensePrefs.Main.Motion.SCREENOFF_FIX
    * nl.sense_os.service.constants.SensePrefs.Main.Motion.UNREG
  * nl.sense_os.service.constants.SensePrefs.Main.PhoneState
    * nl.sense_os.service.constants.SensePrefs.Main.PhoneState.APP_INFO
    * nl.sense_os.service.constants.SensePrefs.Main.PhoneState.BATTERY
    * nl.sense_os.service.constants.SensePrefs.Main.PhoneState.CALL_STATE
    * nl.sense_os.service.constants.SensePrefs.Main.PhoneState.DATA_CONNECTION
    * nl.sense_os.service.constants.SensePrefs.Main.PhoneState.FOREGROUND_APP
    * nl.sense_os.service.constants.SensePrefs.Main.PhoneState.INSTALLED_APPS
    * nl.sense_os.service.constants.SensePrefs.Main.PhoneState.IP_ADDRESS
    * nl.sense_os.service.constants.SensePrefs.Main.PhoneState.PROXIMITY
    * nl.sense_os.service.constants.SensePrefs.Main.PhoneState.SCREEN_ACTIVITY
    * nl.sense_os.service.constants.SensePrefs.Main.PhoneState.SERVICE_STATE
    * nl.sense_os.service.constants.SensePrefs.Main.PhoneState.SIGNAL_STRENGTH
    * nl.sense_os.service.constants.SensePrefs.Main.PhoneState.UNREAD_MSG
  * nl.sense_os.service.constants.SensePrefs.Main.SampleRate
    * nl.sense_os.service.constants.SensePrefs.Main.SampleRate.RARELY
    * nl.sense_os.service.constants.SensePrefs.Main.SampleRate.BALANCED
    * nl.sense_os.service.constants.SensePrefs.Main.SampleRate.NORMAL
    * nl.sense_os.service.constants.SensePrefs.Main.SampleRate.OFTEN
  * nl.sense_os.service.constants.SensePrefs.Main.SyncRate
    * nl.sense_os.service.constants.SensePrefs.Main.SyncRate.RARELY
    * nl.sense_os.service.constants.SensePrefs.Main.SyncRate.ECO_MODE
    * nl.sense_os.service.constants.SensePrefs.Main.SyncRate.NORMAL
    * nl.sense_os.service.constants.SensePrefs.Main.SyncRate.OFTEN
    * nl.sense_os.service.constants.SensePrefs.Main.SyncRate.REAL_TIME
  * nl.sense_os.service.constants.SensePrefs.Main.Quiz
    * nl.sense_os.service.constants.SensePrefs.Main.Quiz.RATE
    * nl.sense_os.service.constants.SensePrefs.Main.Quiz.SILENT_MODE
    * nl.sense_os.service.constants.SensePrefs.Main.Quiz.SYNC
    * nl.sense_os.service.constants.SensePrefs.Main.Quiz.SYNC_TIME
  * nl.sense_os.service.constants.SensePrefs.Main.APPLICATION_KEY
  * nl.sense_os.service.constants.SensePrefs.Main.LAST_LOGGED_IN
  * nl.sense_os.service.constants.SensePrefs.Main.LAST_STATUS
  * nl.sense_os.service.constants.SensePrefs.Main.LAST_VERIFIED_SENSORS
  * nl.sense_os.service.constants.SensePrefs.Main.SAMPLE_RATE
  * nl.sense_os.service.constants.SensePrefs.Main.SYNC_RATE
* nl.sense_os.service.constants.SensePrefs.Auth
  * nl.sense_os.service.constants.SensePrefs.Auth.DEVICE_ID
  * nl.sense_os.service.constants.SensePrefs.Auth.DEVICE_ID_TIME
  * nl.sense_os.service.constants.SensePrefs.Auth.DEVICE_TYPE
  * nl.sense_os.service.constants.SensePrefs.Auth.GCM_REGISTRATION_ID
  * nl.sense_os.service.constants.SensePrefs.Auth.LOGIN_COOKIE
  * nl.sense_os.service.constants.SensePrefs.Auth.LOGIN_PASS
  * nl.sense_os.service.constants.SensePrefs.Auth.LOGIN_SESSION_ID
  * nl.sense_os.service.constants.SensePrefs.Auth.LOGIN_USERNAME
  * nl.sense_os.service.constants.SensePrefs.Auth.PHONE_IMEI
  * nl.sense_os.service.constants.SensePrefs.Auth.PHONE_TYPE
  * nl.sense_os.service.constants.SensePrefs.Auth.SENSOR_LIST
  * nl.sense_os.service.constants.SensePrefs.Auth.SENSOR_LIST_COMPLETE
  * nl.sense_os.service.constants.SensePrefs.Auth.SENSOR_LIST_COMPLETE_TIME
  * nl.sense_os.service.constants.SensePrefs.Auth.SENSOR_LIST_TIME
* nl.sense_os.service.constants.SensePrefs.Status
  * nl.sense_os.service.constants.SensePrefs.Status.AMBIENCE
  * nl.sense_os.service.constants.SensePrefs.Status.AUTOSTART
  * nl.sense_os.service.constants.SensePrefs.Status.DEV_PROX
  * nl.sense_os.service.constants.SensePrefs.Status.EXTERNAL
  * nl.sense_os.service.constants.SensePrefs.Status.LOCATION
  * nl.sense_os.service.constants.SensePrefs.Status.MAIN
  * nl.sense_os.service.constants.SensePrefs.Status.MOTION
  * nl.sense_os.service.constants.SensePrefs.Status.PAUSED_UNTIL_NEXT_CHARGE
  * nl.sense_os.service.constants.SensePrefs.Status.PHONESTATE
  * nl.sense_os.service.constants.SensePrefs.Status.POPQUIZ
