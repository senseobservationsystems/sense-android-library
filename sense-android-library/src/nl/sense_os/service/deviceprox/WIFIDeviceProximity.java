/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.deviceprox;

import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.SensorDataPoint;
import nl.sense_os.service.subscription.BaseDataProducer;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Represents the WiFi scan sensor. Performs periodic scans of the WiFi network on a separate
 * thread.
 * 
 * @author Ted Schmidt <ted@sense-os.nl>
 */
public class WIFIDeviceProximity extends BaseDataProducer{

    /*
     * Wi-Fi Scan thread
     */
    private class WifiScanThread implements Runnable {

        private boolean wifiActiveFromTheStart = false;

        private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (!scanEnabled)
                    return;

                Log.i(TAG, "Scan complete");
                // get the results of the wifi scan
                List<ScanResult> results = wifi.getScanResults();
                if (results != null && results.size() > 0) {
                    // Log.v(TAG, "WiFi devices found: " + results.size());
                    try {

                        for (ScanResult result : results) {

                            JSONObject deviceJson = new JSONObject();
                            deviceJson.put("ssid", result.SSID);
                            deviceJson.put("bssid", result.BSSID);
                            deviceJson.put("frequency", result.frequency);
                            deviceJson.put("rssi", result.level);
                            deviceJson.put("capabilities", result.capabilities);

                            notifySubscribers();
                            SensorDataPoint dataPoint = new SensorDataPoint(deviceJson);
                            dataPoint.sensorName = SensorNames.WIFI_SCAN;
                            dataPoint.sensorDescription = SensorNames.WIFI_SCAN;
                            dataPoint.timeStamp = SNTP.getInstance().getTime();        
                            sendToSubscribers(dataPoint);
                            
                            // pass device data to the MsgHandler
                            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
                            i.putExtra(DataPoint.SENSOR_NAME, SensorNames.WIFI_SCAN);
                            i.putExtra(DataPoint.VALUE, deviceJson.toString());
                            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
                            i.putExtra(DataPoint.TIMESTAMP, dataPoint.timeStamp);
                            i.setClass(context, nl.sense_os.service.MsgHandler.class);
                            WIFIDeviceProximity.this.context.startService(i);
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException preparing wifi scan data");
                    } finally {
                        stop();
                        wifiScanThread = new WifiScanThread();
                        scanHandler.postDelayed(wifiScanThread, scanInterval);
                    }
                }
            }
        };

        public WifiScanThread() {
            try {
                wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            } catch (Exception e) {
                Log.e(TAG, "Exception preparing wifi scan thread:", e);
            }
        }

        @Override
        public void run() {
            if (scanEnabled) {
                if (!(wifiActiveFromTheStart = wifi.isWifiEnabled())) {
                    wifi.setWifiEnabled(true);
                    // Log.v(TAG, "WIFI enabled for network scan, waiting 1 sec");
                    int cnt = 0;
                    try {
                        while (!wifi.isWifiEnabled() && cnt++ < 31) {
                            // Log.v(TAG, "... waiting 1 sec");
                            Thread.sleep(1000); // evil but necessary
                            if (cnt % 10 == 0)
                                wifi.setWifiEnabled(true);
                        }
                    } catch (InterruptedException e) {
                        Log.e(TAG, "InterruptedException during sleep");
                    }
                }
                context.registerReceiver(wifiReceiver, new IntentFilter(
                        WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                Log.i(TAG, "Starting Wi-Fi scan");
                wifi.startScan();
            } else {
                stop();
            }
        }

        public void stop() {
            try {
                Log.i(TAG, "Stopping Wi-Fi network scan");
                context.unregisterReceiver(wifiReceiver);
                if (!wifiActiveFromTheStart)
                    wifi.setWifiEnabled(false);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

    }

    private static final String TAG = "WIFI DeviceProximity";
    private final Context context;
    private boolean scanEnabled = false;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private int scanInterval = 0;
    private WifiScanThread wifiScanThread = null;
    private WifiManager wifi = null;

    public WIFIDeviceProximity(Context context) {
        this.context = context;
    }

    public int getScanInterval() {
        return scanInterval;
    }

    public void setScanInterval(int scanInterval) {
        this.scanInterval = scanInterval;
    }

    public void startEnvironmentScanning(int interval) {
        scanInterval = interval;
        scanEnabled = true;
        Thread t = new Thread() {
            @Override
            public void run() {
                scanHandler.post(wifiScanThread = new WifiScanThread());
            }
        };
        this.scanHandler.post(t);
    }

    public void stopEnvironmentScanning() {
        scanEnabled = false;
        try {
            wifiScanThread.stop();
            scanHandler.removeCallbacks(wifiScanThread);

        } catch (Exception e) {
            Log.e(TAG, "Exception in stopping wifi scan thread:", e);
        }
    }
}
