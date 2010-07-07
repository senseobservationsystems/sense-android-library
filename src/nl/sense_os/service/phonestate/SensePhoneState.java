package nl.sense_os.service.phonestate;

import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import nl.sense_os.service.MsgHandler;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class SensePhoneState extends PhoneStateListener {
    private static final String TAG = "MyPhoneStateListener";    
    private MsgHandler msgHandler;

    public SensePhoneState(MsgHandler msgHandler) {
        super();        
        this.msgHandler = msgHandler;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        Log.d(TAG, "Call state changed.");
        
        Map<String, String> data = new HashMap<String, String>();
        String strState = "";
        if (state == TelephonyManager.CALL_STATE_IDLE) {
            strState = "idle";
        }
        if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            strState = "calling";
        }
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            strState = "ringing";
        }

        data.put("callState.state", strState);
        data.put("callState.incomingNumber", incomingNumber);
        this.msgHandler.sendPhoneState(data);
        super.onCallStateChanged(state, incomingNumber);
    }

    @Override
    public void onCellLocationChanged(CellLocation location) {
        Log.d(TAG, "Cell location changed.");
        // TODO: Catch listen cell location!
        // location.requestLocationUpdate();
    }

    @Override
    @SuppressWarnings("unused")
    public void onDataActivity(int direction) {
        Log.d(TAG, "Data activity.");
        
        Map<String, String> data = new HashMap<String, String>();
        String strDirection = "";
        if (direction == TelephonyManager.DATA_ACTIVITY_IN) {
            strDirection = "receiving_data";
        }
        if (direction == TelephonyManager.DATA_ACTIVITY_INOUT) {
            strDirection = "receiving_and_sending_data";
        }
        if (direction == TelephonyManager.DATA_ACTIVITY_NONE) {
            strDirection = "none";
        }
        if (direction == TelephonyManager.DATA_ACTIVITY_OUT) {
            strDirection = "sending_data";
            // data.put("dataActivity", strDirection); // Don't Send, will create a loop
            // msgHandler.sendUpdate(data); // Don't Send, will create a loop
        }
    }

    @Override
    public void onDataConnectionStateChanged(int state) {
        Log.d(TAG, "Data connection state changed.");
        
        Map<String, String> data = new HashMap<String, String>();
        String strState = "";
        if (state == TelephonyManager.DATA_CONNECTED) {
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
            }
            data.put("phoneServiceUrl", "http://" + ip + ":8080/console/settings/");
            data.put("phoneCallServiceUrl", "http://" + ip + ":8080");
            strState = "connected";
        }
        if (state == TelephonyManager.DATA_CONNECTING) {
            strState = "connecting";
        }
        if (state == TelephonyManager.DATA_DISCONNECTED) {
            strState = "disconnected";
        }
        if (state == TelephonyManager.DATA_SUSPENDED) {
            strState = "suspended";
        }
        data.put("dataConnection", strState);
        this.msgHandler.sendPhoneState(data);
    }

    @Override
    public void onMessageWaitingIndicatorChanged(boolean mwi) {
        Log.d(TAG, "Message waiting indicator changed.");
        
        Map<String, String> data = new HashMap<String, String>();
        String strState = "";
        if (mwi == true) {
            strState = "true";
        } else {
            strState = "false";
        }
        data.put("unreadMSG", strState);
        this.msgHandler.sendPhoneState(data);
    }

    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
        Log.d(TAG, "Service state changed.");
        
        Map<String, String> data = new HashMap<String, String>();
        String strState = "";
        if (serviceState.getState() == ServiceState.STATE_EMERGENCY_ONLY) {
            strState = "emergency_calls_only";
        }
        if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
            strState = "in_service";
        }
        if (serviceState.getState() == ServiceState.STATE_OUT_OF_SERVICE) {
            strState = "out_of_service";
        }
        if (serviceState.getState() == ServiceState.STATE_POWER_OFF) {
            strState = "power_off";
        }
        data.put("serviceState.state", strState);
        if (serviceState.getIsManualSelection()) {
            data.put("serviceState.manualSet", "true");
        } else {
            data.put("serviceState.manualSet", "false");
        }
        this.msgHandler.sendPhoneState(data);
    }
}
