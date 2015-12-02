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
