package nl.sense_os.senseservice;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.TimeZone;

import nl.sense_os.service.constants.SensorData;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseDataProducer;
import nl.sense_os.service.subscription.SubscriptionManager;

/**
 * Created by ted@sense-os.nl on 12/1/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public class SensorUtils {
    private static SensorUtils mSensorUtils;
    public abstract class DummySensor extends  BaseDataProducer{
        public abstract void sendDummyData();
    }
    private SensorUtils(){}
    
    public static synchronized SensorUtils getInstance(){
        if(mSensorUtils == null){
            mSensorUtils = new SensorUtils();
        }
        return mSensorUtils;
    }

    public DummySensor positionSensor =  new DummySensor() {
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

    public  DummySensor noiseSensor =  new DummySensor() {
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
    public  DummySensor timeZoneSensor =  new DummySensor() {
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

    public  DummySensor accelerometerSensor =  new DummySensor() {
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

    public  DummySensor batterySensor =  new DummySensor() {
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

    public  DummySensor callSensor =  new DummySensor() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.CALL, this);
            try{
                JSONObject json = new JSONObject();
                json.put("state", "dialing");
                json.put("outgoingNumber", "+316123456778");
                SensorDataPoint dataPoint = new SensorDataPoint(json);
                dataPoint.sensorName = SensorData.SensorNames.CALL;
                dataPoint.sensorDescription = SensorData.SensorNames.CALL;
                dataPoint.timeStamp = SNTP.getInstance().getTime();
                sendToSubscribers(dataPoint);
            }catch(Exception e){
                Log.e(TAG, "Error sending call data");
            }
        }
    };

    public  DummySensor lightSensor =  new DummySensor() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.LIGHT, this);
            try{
                SensorDataPoint dataPoint = new SensorDataPoint(10);
                dataPoint.sensorName = SensorData.SensorNames.LIGHT;
                dataPoint.sensorDescription = SensorData.SensorNames.LIGHT;
                dataPoint.timeStamp = SNTP.getInstance().getTime();
                sendToSubscribers(dataPoint);
            }catch(Exception e){
                Log.e(TAG, "Error sending light data");
            }
        }
    };

    public  DummySensor proximitySensor =  new DummySensor() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.PROXIMITY, this);
            try{
                SensorDataPoint dataPoint = new SensorDataPoint(1);
                dataPoint.sensorName = SensorData.SensorNames.PROXIMITY;
                dataPoint.sensorDescription = SensorData.SensorNames.PROXIMITY;
                dataPoint.timeStamp = SNTP.getInstance().getTime();
                sendToSubscribers(dataPoint);
            }catch(Exception e){
                Log.e(TAG, "Error sending proximity data");
            }
        }
    };

    public  DummySensor screenSensor =  new DummySensor() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.SCREEN, this);
            try{
                SensorDataPoint dataPoint = new SensorDataPoint("on");
                dataPoint.sensorName = SensorData.SensorNames.SCREEN;
                dataPoint.sensorDescription = SensorData.SensorNames.SCREEN;
                dataPoint.timeStamp = SNTP.getInstance().getTime();
                sendToSubscribers(dataPoint);
            }catch(Exception e){
                Log.e(TAG, "Error sending screen data");
            }
        }
    };

    public  DummySensor wifiScanSensor =  new DummySensor() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.WIFI_SCAN, this);
            try{
                JSONObject deviceJson = new JSONObject();
                deviceJson.put("ssid", "Internet AP");
                deviceJson.put("bssid", "02:A3:B4:91:4F");
                deviceJson.put("frequency", 266);
                deviceJson.put("rssi", -68);
                deviceJson.put("capabilities", "WPS");

                notifySubscribers();
                SensorDataPoint dataPoint = new SensorDataPoint(deviceJson);
                dataPoint.sensorName = SensorData.SensorNames.WIFI_SCAN;
                dataPoint.sensorDescription = SensorData.SensorNames.WIFI_SCAN;
                dataPoint.timeStamp = SNTP.getInstance().getTime();
                sendToSubscribers(dataPoint);
            }catch(Exception e){
                Log.e(TAG, "Error sending WIFI scan  data");
            }
        }
    };

    public  DummySensor appInfo =  new DummySensor() {
        public void sendDummyData() {
            // register this produces
            SubscriptionManager.getInstance().registerProducer(SensorData.SensorNames.APP_INFO, this);
            try{
                JSONObject json = new JSONObject();
                json.put("sense_library_version", "v1.33.7");
                json.put("app_name", "sense unit test");
                json.put("app_build", "42");
                SensorDataPoint dataPoint = new SensorDataPoint(json);
                dataPoint.sensorName = SensorData.SensorNames.APP_INFO;
                dataPoint.sensorDescription = SensorData.SensorNames.APP_INFO;
                dataPoint.timeStamp = SNTP.getInstance().getTime();
                sendToSubscribers(dataPoint);
            }catch(Exception e){
                Log.e(TAG, "Error sending app Info");
            }
        }
    };
}
