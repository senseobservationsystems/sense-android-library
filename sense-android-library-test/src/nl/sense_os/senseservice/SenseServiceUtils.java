package nl.sense_os.senseservice;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.TimeZone;

import nl.sense_os.datastorageengine.DataStorageEngine;
import nl.sense_os.datastorageengine.test.CSUtils;
import nl.sense_os.platform.SenseApplication;
import nl.sense_os.platform.SensePlatform;
import nl.sense_os.service.ISenseServiceCallback;
import nl.sense_os.service.constants.SensorData;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseDataProducer;
import nl.sense_os.service.subscription.DataConsumer;
import nl.sense_os.service.subscription.DataProducer;
import nl.sense_os.service.subscription.SubscriptionManager;
import nl.sense_os.util.json.EncryptionHelper;

/**
 * Created by ted@sense-os.nl on 11/30/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public class SenseServiceUtils {

    static final String uniqueID = ""+ System.currentTimeMillis();
    public static final String EMAIL = "spam+ce_"+uniqueID+"@sense-os.nl";
    public static final String PASSWORD = "87f95196987d8c3bf339e2a52be957f4";
    private static int loginResult = -1;
    private static SensePlatform mSensePlatform;

    /**
     * Returns a SensePlatform object with a bound SenseService
     * @return the SensePlatform
     */
    public static SensePlatform getSensePlatform(Context context) throws InterruptedException {
        if(mSensePlatform != null){
            return mSensePlatform;
        }
        final Object monitor = new Object();
        // wait for the SenseService to be connected
        SensePlatform sensePlatform = new SensePlatform(context, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                synchronized (monitor){
                    monitor.notifyAll();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        });
        synchronized (monitor){
            monitor.wait();
        }
        mSensePlatform = sensePlatform;
        return sensePlatform;
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
        CSUtils csUtils = new CSUtils(false);
        Map<String, String> newUser = csUtils.createCSAccount();
        SensePlatform sensePlatform = SenseServiceUtils.getSensePlatform(context);
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

    public static BaseDataProducer positionSensor =  new BaseDataProducer() {
        public void sendDummyData(){
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.POSITION, this);
            JSONObject json = new JSONObject();
            try {
                json.put("latitude", 53.209684d);
                json.put("longitude", 6.5452243d);
                json.put("accuracy", 100d);
                json.put("altitude", 1);
                json.put("speed", 5d);
                json.put("bearing", 5d);
                json.put("provider", "fused");

            } catch (JSONException e) {
                Log.e(TAG, "JSONException in onLocationChanged", e);
                return;
            }

            // use SNTP time
            long timestamp = SNTP.getInstance().getTime();
            notifySubscribers();
            SensorDataPoint dataPoint = new SensorDataPoint(json);
            dataPoint.sensorName = SensorData.SensorNames.POSITION;
            dataPoint.sensorDescription = SensorData.SensorNames.POSITION;
            dataPoint.timeStamp = timestamp;
            sendToSubscribers(dataPoint);
        }
    };

    public static BaseDataProducer noiseSensor =  new BaseDataProducer() {
        public void sendDummyData(){
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.NOISE, this);
            notifySubscribers();
            SensorDataPoint dataPoint = new SensorDataPoint(10);
            dataPoint.sensorName = SensorData.SensorNames.NOISE;
            dataPoint.sensorDescription = SensorData.SensorNames.NOISE;
            dataPoint.timeStamp = SNTP.getInstance().getNtpTime();
            sendToSubscribers(dataPoint);
        }
    };
    public static BaseDataProducer timeZoneSensor =  new BaseDataProducer() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.TIME_ZONE, this);
            try {
                this.notifySubscribers();
                TimeZone timeZone = TimeZone.getDefault();
                JSONObject timeZoneJson = new JSONObject();
                timeZoneJson.put("offset", timeZone.getOffset(SNTP.getInstance().getTime())/1000l);
                timeZoneJson.put("id", timeZone.getID());
                SensorDataPoint dataPoint = new SensorDataPoint(timeZoneJson);
                dataPoint.sensorName = SensorData.SensorNames.TIME_ZONE;
                dataPoint.sensorDescription = "Current time zone";
                dataPoint.timeStamp = SNTP.getInstance().getTime();
                //Log.d(TAG,"time zone subscribers: " + this.);
                this.sendToSubscribers(dataPoint);
            } catch (JSONException e) {
                Log.e(TAG, "Error creating time zone json object");
            }
        }
    };

    public static BaseDataProducer accelerometerSensor =  new BaseDataProducer() {
        public void sendDummyData(){
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.ACCELEROMETER, this);
            try {
                this.notifySubscribers();
                JSONObject json = new JSONObject();
                json.put("x-axis", 10);
                json.put("y-axis", 10);
                json.put("z-axis", 10);
                SensorDataPoint dataPoint = new SensorDataPoint(json);
                dataPoint.sensorName = SensorData.SensorNames.ACCELEROMETER;
                dataPoint.sensorDescription = SensorData.SensorNames.ACCELEROMETER;
                dataPoint.timeStamp = SNTP.getInstance().getTime();
                this.sendToSubscribers(dataPoint);
            }catch(Exception e){
                Log.e(TAG, "Error creating and sending accelerometerData");
            }

        }
    };

    public static BaseDataProducer batterySensor =  new BaseDataProducer() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.BATTERY, this);
            notifySubscribers();
            try {
                JSONObject json = new JSONObject();
                json.put("level", 10);
                json.put("status", "charging");
                SensorDataPoint dataPoint = new SensorDataPoint(json);
                dataPoint.sensorName = SensorData.SensorNames.BATTERY;
                dataPoint.sensorDescription = SensorData.SensorNames.BATTERY;
                dataPoint.timeStamp = SNTP.getInstance().getTime();
                sendToSubscribers(dataPoint);
            }catch(Exception e){
                Log.e(TAG, "Error sending battery data");
            }
        }
    };

    public static BaseDataProducer callSensor =  new BaseDataProducer() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.BATTERY, this);
            // TODO implement
        }
    };

    public static BaseDataProducer lightSensor =  new BaseDataProducer() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.BATTERY, this);
            // TODO implement
        }
    };

    public static BaseDataProducer proximitySensor =  new BaseDataProducer() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.BATTERY, this);
            // TODO implement
        }
    };

    public static BaseDataProducer screenSensor =  new BaseDataProducer() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.BATTERY, this);
            // TODO implement
        }
    };

    public static BaseDataProducer wifiScanSensor =  new BaseDataProducer() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.BATTERY, this);
            // TODO implement
        }
    };

    public static BaseDataProducer appInfo =  new BaseDataProducer() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.BATTERY, this);
            // TODO implement
        }
    };
}
