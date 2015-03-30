# SensePlatform {#sense_platform}
The SensePlatform class provides an abstraction to simplify the interaction with the SenseService, user management and sensor data access. It's a proxy class which by instantiating binds (and starts if needed) the SenseService. You can then use the high level methods of this class, and/or get the service object to work directly with the SenseService.  

When a SensePlatform object has been instantiated it takes some time for the SenseService to be bound. You should therefore make sure that you have a bounded SenseService before using it, by i.e. calling the function `getSenseService().isBinderAlive()`. An Exception will be thrown if this is not the case, which implies you have to wait some more. Checking the status and waiting should not be done on the main/UI thread or it will block the bind process.  

# User management {#user_management}
The SensePlatform provides user management functions which interact with CommonSense. It enables you to create a user with an 		ISenseServiceCallback callback function which notifies you on the status of this action. If the create user call was successful then the SenseService will automatically be logged in. It will than remain to be logged in through a session-id until the logout function is called.  
## Encrypted credentials {#encrypted_credentials}
The user credentials will be stored in a shared preferences file so that the SenseService can login automatically when the session-id expires. 

The encryption for the shared authentication preferences file can be turned on or off via:<br>
~~~
senseService.setPrefBool(SensePrefs.Main.Advanced.ENCRYPT_CREDENTIAL, true);  
~~~
And an app specific salt key kan be provided via:<br>
~~~
senseService.setPrefString(SensePrefs.Main.Advanced.ENCRYPT_CREDENTIAL_SALT, "1tD#V4#%6BT!@#$%XCBCV");
~~~

# Sensor data {#sensor_data}
The SensePlatform provides simplified implementations for storing and accessing sensor data.

## Sensor data storage {#sensor_data_storage}
The function `addData` implements the intent broadcast method to send a sensor data point as desribed in the [Data Storage](documentation/storage.md). It also implements a call to the SensorRegistrator class to automatically create this sensor if it has not been created yet. Because sending a sensor data point via an intent broadcast when this sensor does not exist in CommonSense will result in the data not being uploaded. _Mind that this function could access the network when real-time sync is enabled and should therefore not be called from the main/ui thread._

## Sensor data access {#sensor_data_retrieval}
Sensor data from the local and remote storage can be retrieved via the SensePlatform using the `getLocalData` and `getData` functions respectively. The SensePlatform implements multiple versions of these functions with different arguments to specify the time boundaries, retrieval size and if it should be data from a sensor which is connected to this device.

## Flusing data {#flusing_Data}
In order to force an upload of the data in the local storage to CommonSense, for instance when a user decides to logout, the `flushData()` or `flushDataAndBlock()` functions can be used. In which case the later only returns when it's finished with uploading all the data.

