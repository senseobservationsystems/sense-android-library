/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service.deviceprox;

import android.content.Context;
import android.content.SharedPreferences;

import nl.sense_os.service.Constants;

public class DeviceProximity {

    private Context context;
    private boolean isScanEnabled = false;
    private BluetoothDeviceProximity bluetoothDP;
    private WIFIDeviceProximity wifiDP;
    private boolean isBtEnabled;
    private boolean isWifiEnabled;
    private int scanInterval = 0;

    public DeviceProximity(Context context) {
        this.context = context;
        this.bluetoothDP = new BluetoothDeviceProximity(context);
        this.wifiDP = new WIFIDeviceProximity(context);
    }

    public int getScanInterval() {
        return scanInterval;
    }

    public void setScanInterval(int scanInterval) {
        this.scanInterval = scanInterval;
    }

    public boolean getScanEnabled() {
        return isScanEnabled;
    }

    public void startEnvironmentScanning(int interval) {
        this.scanInterval = interval;
        this.isScanEnabled = true;

        final SharedPreferences mainPrefs = this.context.getSharedPreferences(Constants.MAIN_PREFS,
                Context.MODE_WORLD_WRITEABLE);
        
        this.isBtEnabled = mainPrefs.getBoolean(Constants.PREF_PROXIMITY_BT, true);
        if (this.isBtEnabled) {
            this.bluetoothDP.startEnvironmentScanning(interval);
        }
        
        this.isWifiEnabled = mainPrefs.getBoolean(Constants.PREF_PROXIMITY_WIFI, true);
        if (this.isWifiEnabled) {
            this.wifiDP.startEnvironmentScanning(interval);
        }
    }

    public void stopEnvironmentScanning() {
        this.isScanEnabled = false;
        if (this.isBtEnabled)
            this.bluetoothDP.stopEnvironmentScanning();
        if (this.isWifiEnabled)
            this.wifiDP.stopEnvironmentScanning();
    }
}
