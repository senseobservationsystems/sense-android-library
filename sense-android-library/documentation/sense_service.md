# Sense Service

This is main sense service class from which application can binded to. 

Activities or other component that binded to sense service can not access the SenseService directly, but through nl.sense_os.service.SenseServiceStub instance, that can be gathered from the nl.sense_os.service.SenseService.SenseBinder instance refered in onServiceConnected callback of ServiceConnection defined when binding the service (see Binding). Through this SenseServiceStub instance, application could then retrieve some functionality:
* Register user
* Login/Logout
* Toggle (start/stop) main sensing
* Toggle (start/stop) individual sensor
* Get/Set settings preferences

SenseService is implemented as singleton, and it has an instance of nl.sense_os.service.ServiceStateHelper to keep its state. It also has an instance of nl.sense_os.service.subscription.SubscriptionManager to manage all available sensor in a producer-subscriber model (see Data Subcription).

SenseService should always running as a foreground service, and should not being killed except being stop explicitly. There is also another component nl.sense_os.service.AliveChecker that will check the service periodically and make sure it keeps running.

SenseService is implemented in nl.sense_os.service.SenseService.

## 1. Binding

Sense Service is a service that others component (application) could bind to. It will return nl.sense_os.service.SenseService.SenseBinder object to bounded component through a derivative of android.content.ServiceConnection when connected. Through this binder, the component could get a nl.sense_os.service.SenseServiceStub object which provide interface for SenseService functionality.

Here an example from SensePlatform on how to bind to Sense Service.

    final Intent serviceIntent = new Intent(getContext(), nl.sense_os.service.SenseService.class);
    boolean bindResult = mContext.bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

And here is an example of onServiceConnected callback of ServiceConnection class that will receive the binder when connected to service (also from SensePlatform).

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.v(TAG, "Bound to Sense Platform service...");

            mSenseService = ((SenseBinder) binder).getService();
        }

Some of functionality in SenseServiceStub provide callback mechanism implementing AIDL interface defined in nl.sense_os.service.ISenseServiceCallback. This interface provide several callback function :
* statusReport, return the current status of SenseService
* onChangeLoginResult, called when login status is change
* onRegisterResult, called when finish registering a user

This is an example of how to use the callback mechanism in login case.

	mSenseService.changeLogin(username,	SenseApi.hashPassword(password), this.mServiceCallback);

And this is the definition mServiceCallback for such example.

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

## 2. Manage/Stopping (Main / Sensor groups)

SenseService provide several method to toggle the main state as well as sensor groups state (see Sensor Groups). There is a toggle method for each of the sensor groups. Here is an example of how to toggle the states of several sensor groups and the main service.

    // enable some specific sensor modules
    senseService.togglePhoneState(true);
    senseService.toggleAmbience(true);
    senseService.toggleMotion(true);
    senseService.toggleLocation(true);
    
    // enable main service state
    senseService.toggleMain(true);


## 3. Background service

The SenseService will start as a service when nl.sense_os.service.SenseService.toggleMain method is called with true argument, It will also registers itself as foreground service so it doesn’t easily killed by Android when the system low on memory. It will also being start as STICKY service so android know to start it again if it’s accidentally been killed.

When the service started, it will try to start available sensor based on preferences. It will also start scheduling for data transmission to commonSense. Finally it will send broadcast so any other component could be notified that the sense service already start (see Start Broadcast).

Each of sensor groups will also be run in different thread, and will keep exist while the service running.

## 4. Start Broadcast

Sense Service will broadcast *action_sense_service_broadcast* action everytime :
* The service started
* Sensor modules started/stop

The broadcast receiver then could check the service status through nl.sense_os.service.ServiceStateHelper instance.

## 5. Alive Checker

SenseService has an instance of AliveChecker to make sure that keep running when it should be. It will start checking periodically when SenseService starts sensing.

Alive Checker will check every 15 minute if the phone is awake, and it will make sure to wake up every 1 hour. It will make sure the SenseService keep running by trying to start the service regardless of current state.
