package nl.sense_os.senseservice.test;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import java.io.IOException;
import java.util.Map;

import nl.sense_os.datastorageengine.test.CSUtils;
import nl.sense_os.platform.SensePlatform;
import nl.sense_os.service.ISenseServiceCallback;
import nl.sense_os.service.constants.SensePrefs;

/**
 * Created by ted@sense-os.nl on 11/30/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public class SenseServiceUtils {

    private static int loginResult = -1;
    private static SensePlatform mSensePlatform;
    private static boolean mServiceConnected;

    /**
     * Returns a SensePlatform object with a bound SenseService
     * @return the SensePlatform
     */
    public static SensePlatform getSensePlatform(final Context context) throws InterruptedException {
        if(mSensePlatform != null){
            return mSensePlatform;
        }

        // Connect to the SenseService using the SensePlatform
        final Object monitor = new Object();
        mServiceConnected = false;
        mSensePlatform = new SensePlatform(context, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                synchronized (monitor) {
                    mServiceConnected = true;
                    monitor.notifyAll();
                }
            }
            @Override
            public void onServiceDisconnected(ComponentName componentName) {}
        });

        // wait for the SenseService to be connected
        synchronized (monitor){
            if(!mServiceConnected) {
                monitor.wait(60000);
            }
        }
        // check if the binder is alive, if not then something is wrong and an exception is thrown
        mSensePlatform.getService().isBinderAlive();
        return mSensePlatform;
    }

    /**
     * Creates a new random user account and logs in to the SenseService
     * This functions performs a wait on the current thread
     * @param context An Android context
     * @throws IOException
     * @throws InterruptedException
     * @throws RemoteException
     * @throws RuntimeException
     */
    public static void createAccountAndLoginService(Context context) throws IOException, InterruptedException, RemoteException, RuntimeException {
        // get the sensePlatform object
        SensePlatform sensePlatform = SenseServiceUtils.getSensePlatform(context);
        // set DEV mode
        sensePlatform.getService().setPrefBool(SensePrefs.Main.Advanced.DEV_MODE, true);
        // set the application key
        sensePlatform.getService().setPrefString(SensePrefs.Main.APPLICATION_KEY, CSUtils.APP_KEY);

        // Create a random CS account
        CSUtils csUtils = new CSUtils(false);
        Map<String, String> newUser = csUtils.createCSAccount();

        // Login with the new username and password in the SenseService
        final Object monitor = new Object();
        loginResult = -1;
        sensePlatform.login(newUser.get("username"), newUser.get("password"), new ISenseServiceCallback() {
            @Override
            public void statusReport(int status) throws RemoteException {}

            @Override
            public void onChangeLoginResult(int result) throws RemoteException {
                // got login result
                synchronized (monitor){
                    loginResult = result;
                    monitor.notifyAll();
                }
            }

            @Override
            public void onRegisterResult(int result) throws RemoteException {}

            @Override
            public IBinder asBinder() {return null;}
        });
        // Wait for the login result
        synchronized (monitor){
            monitor.wait();
        }
        // Only on successful login return normally
        if(loginResult != 0){
            throw new RuntimeException("Error creating new user and logging in");
        }
    }

    public static void logout(Context context) throws InterruptedException, RemoteException {
        getSensePlatform(context).logout();
    }

}
