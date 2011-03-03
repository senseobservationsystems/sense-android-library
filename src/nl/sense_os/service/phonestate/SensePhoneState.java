/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service.phonestate;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import nl.sense_os.service.Constants;
import nl.sense_os.service.MsgHandler;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SensePhoneState extends PhoneStateListener {
    private static final String NAME_CALL = "call state";
    private static final String NAME_DATA = "data connection";
    private static final String NAME_IP = "ip address";
    private static final String NAME_SERVICE = "service state";
    private static final String NAME_SIGNAL = "signal strength";
    private static final String NAME_UNREAD = "unread msg";
    private static final String TAG = "Sense PhoneStateListener";
    private Context context;

    public SensePhoneState(Context context) {
        super();
        this.context = context;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {

        JSONObject json = new JSONObject();
        try {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE :
                    json.put("state", "idle");
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK :
                    json.put("state", "calling");
                    break;
                case TelephonyManager.CALL_STATE_RINGING :
                    json.put("state", "ringing");
                    json.put("incomingNumber", incomingNumber);
                    break;
                default :
                    Log.e(TAG, "Unexpected call state: " + state);
                    return;
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException in onCallChanged", e);
            return;
        }

        // pass message to the MsgHandler
        Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
        i.putExtra(MsgHandler.KEY_SENSOR_NAME, NAME_CALL);
        i.putExtra(MsgHandler.KEY_VALUE, json.toString());
        i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_JSON);
        i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
        this.context.startService(i);
    }

    @Override
    public void onCellLocationChanged(CellLocation location) {

        // TODO: Catch listen cell location!
        // location.requestLocationUpdate();
    }

    @Override
    public void onDataActivity(int direction) {
        // not used to prevent a loop
        /*
         * JSONObject json = new JSONObject(); try { switch (direction) { case
         * TelephonyManager.DATA_ACTIVITY_IN: json.put("dataActivity", "receiving data"); break;
         * case TelephonyManager.DATA_ACTIVITY_INOUT: json.put("dataActivity",
         * "receiving and sending_data"); break; case TelephonyManager.DATA_ACTIVITY_NONE:
         * json.put("dataActivity", "none"); break; case TelephonyManager.DATA_ACTIVITY_OUT:
         * json.put("dataActivity", "sending_data"); break; default: Log.e(TAG,
         * "Unexpected data activity direction: " + direction); return; } } catch (JSONException e)
         * { Log.e(TAG, "JSONException in onDataActivity", e); return; }
         * 
         * // pass message to the MsgHandler Intent i = new Intent(this.context, MsgHandler.class);
         * i.putExtra("name", "dataActivity"); i.putExtra("msg", json.toString());
         * i.putExtra("type", Constants.SENSOR_DATA_TYPE_JSON); this.context.startService(i);
         */
    }

    @Override
    public void onDataConnectionStateChanged(int state) {

        String strState = "";
        switch (state) {
            case TelephonyManager.DATA_CONNECTED :
                // send the URL on which the phone can be reached
                String ip = "";
                try {
                    Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                    while (nis.hasMoreElements()) {
                        NetworkInterface ni = nis.nextElement();
                        Enumeration<InetAddress> iis = ni.getInetAddresses();
                        while (iis.hasMoreElements()) {
                            InetAddress ia = iis.nextElement();
                            if (ni.getDisplayName().equalsIgnoreCase("rmnet0")) {
                                ip = ia.getHostAddress();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting my own IP:", e);
                    return;
                }
                if (ip.length() > 1) {
                    // pass message to the MsgHandler
                    Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
                    i.putExtra(MsgHandler.KEY_SENSOR_NAME, NAME_IP);
                    i.putExtra(MsgHandler.KEY_VALUE, ip);
                    i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_STRING);
                    i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
                    this.context.startService(i);
                }
                strState = "connected";
                break;
            case TelephonyManager.DATA_CONNECTING :
                strState = "connecting";
                break;
            case TelephonyManager.DATA_DISCONNECTED :
                strState = "disconnected";
                break;
            case TelephonyManager.DATA_SUSPENDED :
                strState = "suspended";
                break;
            default :
                Log.e(TAG, "Unexpected data connection state: " + state);
                return;
        }

        // pass message to the MsgHandler
        Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
        i.putExtra(MsgHandler.KEY_SENSOR_NAME, NAME_DATA);
        i.putExtra(MsgHandler.KEY_VALUE, strState);
        i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_STRING);
        i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
        this.context.startService(i);
    }

    @Override
    public void onMessageWaitingIndicatorChanged(boolean unreadMsgs) {

        // pass message to the MsgHandler
        Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
        i.putExtra(MsgHandler.KEY_SENSOR_NAME, NAME_UNREAD);
        i.putExtra(MsgHandler.KEY_VALUE, unreadMsgs);
        i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_BOOL);
        i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
        this.context.startService(i);
    }

    @Override
    public void onServiceStateChanged(ServiceState serviceState) {

        JSONObject json = new JSONObject();
        try {
            switch (serviceState.getState()) {
                case ServiceState.STATE_EMERGENCY_ONLY :
                    json.put("state", "emergency calls only");
                    break;
                case ServiceState.STATE_IN_SERVICE :
                    json.put("state", "in service");
                    String number = ((TelephonyManager) context
                            .getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
                    json.put("phone number", number);
                    break;
                case ServiceState.STATE_OUT_OF_SERVICE :
                    json.put("state", "out of service");
                    break;
                case ServiceState.STATE_POWER_OFF :
                    json.put("state", "power off");
                    break;
            }

            json.put("manualSet", serviceState.getIsManualSelection() ? true : false);

        } catch (JSONException e) {
            Log.e(TAG, "JSONException in onServiceStateChanged", e);
            return;
        }

        // pass message to the MsgHandler
        Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
        i.putExtra(MsgHandler.KEY_SENSOR_NAME, NAME_SERVICE);
        i.putExtra(MsgHandler.KEY_VALUE, json.toString());
        i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_JSON);
        i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
        this.context.startService(i);
    }

    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {

        JSONObject json = new JSONObject();
        try {
            json.put("CDMA dBm", signalStrength.getCdmaDbm());
            json.put("EVDO dBm", signalStrength.getEvdoDbm());
            json.put("GSM signal strength", signalStrength.getGsmSignalStrength());
            json.put("GSM bit error rate", signalStrength.getGsmBitErrorRate());
        } catch (JSONException e) {
            Log.e(TAG, "JSONException in onSignalStrengthsChanged", e);
            return;
        }

        // pass message to the MsgHandler
        Intent i = new Intent(MsgHandler.ACTION_NEW_MSG);
        i.putExtra(MsgHandler.KEY_SENSOR_NAME, NAME_SIGNAL);
        i.putExtra(MsgHandler.KEY_VALUE, json.toString());
        i.putExtra(MsgHandler.KEY_DATA_TYPE, Constants.SENSOR_DATA_TYPE_JSON);
        i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
        this.context.startService(i);
    }

}
