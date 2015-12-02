# Sense Service

This is the main `SenseService` class to which application can bind. 

Activities or other components can not access the SenseService directly, but must do so through an nl.sense_os.service.SenseServiceStub instance, that can be gathered from the nl.sense_os.service.SenseService.SenseBinder instance referred in `onServiceConnected` callback of `ServiceConnection` defined when binding the service (see [Binding](#binding)). Through this `SenseServiceStub` instance, the application can then retrieve some functionality:
* Register user
* Login/Logout
* Toggle (start/stop) main sensing
* Toggle (start/stop) individual sensor
* Get/Set settings preferences

`SenseService` is implemented as a singleton, and it has an instance of nl.sense_os.service.ServiceStateHelper to keep its state. It also has an instance of nl.sense_os.service.subscription.SubscriptionManager to manage all available sensors in a producer-subscriber model (see [Data Subcription](documentation/subscription.md)).

`SenseService` should always be running as a foreground service, and should not be killed except by being stopped explicitly. There is also another component, nl.sense_os.service.AliveChecker (see [Alive Checker](#alive_checker)), that will check the service periodically and make sure it keeps running.

`SenseService` is implemented in nl.sense_os.service.SenseService.

# Binding {#binding}

`SenseService` is a service that others components (applications) can bind to. It will return an nl.sense_os.service.SenseService.SenseBinder object to the bound component through a derivative of android.content.ServiceConnection when connected. Through this binder, the component could get a `SenseServiceStub` object which provides an interface for `SenseService` functionality.

Here is an example from [SensePlatform](documentation/sense_platform.md) on how to bind to `SenseService`.

~~~java
final Intent serviceIntent = new Intent(getContext(), nl.sense_os.service.SenseService.class);
boolean bindResult = mContext.bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
~~~

And here is an example of onServiceConnected callback of the ServiceConnection class that will receive the binder when connected to the service (also from SensePlatform).

~~~java
@Override
public void onServiceConnected(ComponentName className, IBinder binder) {
    Log.v(TAG, "Bound to Sense Platform service...");

    mSenseService = ((SenseBinder) binder).getService();
}
~~~

Some of the functionality in `SenseServiceStub` provides a callback mechanism implementing an AIDL interface defined in nl.sense_os.service.ISenseServiceCallback. This interface provides several callback functions:
* `statusReport`, returns the current status of SenseService
* `onChangeLoginResult`, called when login status changes
* `onRegisterResult`, called when finished registering a user

This is an example of how to use the callback mechanism in the login case.

~~~java
mSenseService.changeLogin(username,	SenseApi.hashPassword(password), this.mServiceCallback);
~~~

And this is the definition `mServiceCallback` for that example.

~~~
private final ISenseServiceCallback mServiceCallback = new ISenseServiceCallback.Stub() {
	@Override
	public IBinder asBinder() {
		return null;
	}

	@Override
	public void onChangeLoginResult(final int result) throws RemoteException {

		// 0 if login completed successfully, -2 if login was forbidden,
		// and -1 for any other errors.

		if (result == -2) {
			onLoginFailure(LoginActivityFrag.this.ERROR_CREDENTIAL);

		} else if (result == -1) {
			onLoginFailure(LoginActivityFrag.this.ERROR_CREDENTIAL);

		} else {
			onLoginSuccess();
		}
	}
}
~~~

# Manage Main and Sensor Groups {#manage_main_sensor_groups}

`SenseService` provides several methods to toggle the main state as well as sensor group states (see [Sensors](documentation/sensors.md)). There is a toggle method for each of the sensor groups. Here is an example of how to toggle the states of several sensor groups and the main service.

~~~
// enable some specific sensor modules
senseService.togglePhoneState(true);
senseService.toggleAmbience(true);
senseService.toggleMotion(true);
senseService.toggleLocation(true);

// enable main service state
senseService.toggleMain(true);
~~~

# Background service {#background_service}

The `SenseService` will start as a service when nl.sense_os.service.SenseService.toggleMain method is called with true argument. It will also register itself as a foreground service so it doesn’t easily get killed by Android when the system is low on memory. It will also be started as a STICKY service so android knows to start it again if it’s accidentally been killed.

When the service starts, it will try to start available sensors based on preferences (see [Settings](documentation/settings.md)). It will also start scheduling for data transmission to CommonSense. Finally it will send a broadcast so any other component will be notified that the `SenseService` has started (see [Start Broadcast](#start_broadcast)).

Each sensor group will also be run in a different thread, and will keep existing while the service running.

# Start Broadcast {#start_broadcast}

`SenseService` will broadcast an *action_sense_service_broadcast* action everytime :
* The service started
* Sensor modules started/stop

The broadcast receiver then can check the service status through the nl.sense_os.service.ServiceStateHelper instance.

# AliveChecker {#alive_checker}

`SenseService` has an instance of nl.sense_os.service.AliveChecker to make sure that it keeps running when it should be. It will start checking periodically when `SenseService` starts sensing.

`AliveChecker` will check every 15 minutes if the phone is awake, and it will make sure to wake up every hour. It will make sure the `SenseService` keeps running by trying to start the service regardless of current state.

# Transmit Sensor Data to CommonSense {#transmit_data}

SenseService uses the nl.sense_os.service.scheduler.DataTransmitter instance to schedule sending of data in the DataStorageEngine to the back-end periodically. The DataTransmitter will register itself to the scheduler to run at a particular interval based on the nl.sense_os.service.constants.SensePrefs.Main.SyncRate setting in preferences. This DataTransmitter then will send a broadcast intent to the WakefulBroadcastReceiver `PeriodicDataSyncer` which will start the IntentService `PeriodicSyncService` to actually call the `DataStorageEngine.syncData` function to send the sensor data to the back-end.

These options are available for SyncRate settings:
* nl.sense_os.service.SensePrefs.Main.SyncRate.ECO_MODE (30 minute)
* nl.sense_os.service.SensePrefs.Main.SyncRate.RARELY (15 minute)
* nl.sense_os.service.SensePrefs.Main.SyncRate.NORMAL (5 minute)
* nl.sense_os.service.SensePrefs.Main.SyncRate.OFTEN (1 minute)
* nl.sense_os.service.SensePrefs.Main.SyncRate.REAL_TIME (depend on sample rate)