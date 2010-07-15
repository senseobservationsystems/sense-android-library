package nl.sense_os.service.phonestate;

import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import nl.sense_os.app.SenseSettings;
import nl.sense_os.service.MsgHandler;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class SensePhoneState extends PhoneStateListener {
    private static final String TAG = "MyPhoneStateListener";    
    private MsgHandler msgHandler;
    private TelephonyManager telman;

    public SensePhoneState(MsgHandler msgHandler, TelephonyManager telman) {
        super();        
        this.telman = telman;
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
        	data.put("incomingNumber", incomingNumber);
        }
        data.put("state", strState);

        this.msgHandler.sendSensorData("call state",data);
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
            strDirection = "receiving data";
        }
        if (direction == TelephonyManager.DATA_ACTIVITY_INOUT) {
            strDirection = "receiving and sending_data";
        }
        if (direction == TelephonyManager.DATA_ACTIVITY_NONE) {
            strDirection = "none";
        }
        if (direction == TelephonyManager.DATA_ACTIVITY_OUT) {
            strDirection = "sending data";
            // data.put("dataActivity", strDirection); // Don't Send, will create a loop
            // msgHandler.sendUpdate(data); // Don't Send, will create a loop
        }
    }

    @Override
    public void onDataConnectionStateChanged(int state) {
        Log.d(TAG, "Data connection state changed.");
                
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
            if(ip.length() > 1)
            	this.msgHandler.sendSensorData("ip address", ip, SenseSettings.SENSOR_DATA_TYPE_STRING);           
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
        this.msgHandler.sendSensorData("data connection", strState, SenseSettings.SENSOR_DATA_TYPE_STRING);
    }

    @Override
    public void onMessageWaitingIndicatorChanged(boolean mwi) {
        Log.d(TAG, "Message waiting indicator changed.");       
    
        String strState = "";
        if (mwi == true) {
            strState = "true";
        } else {
            strState = "false";
        }
        this.msgHandler.sendSensorData("unread msg", strState, SenseSettings.SENSOR_DATA_TYPE_BOOL);
    }

    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
        Log.d(TAG, "Service state changed.");
        
        Map<String, String> data = new HashMap<String, String>();
        String strState = "";
        if (serviceState.getState() == ServiceState.STATE_EMERGENCY_ONLY) {
            strState = "emergency calls only";
        }
        if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
            strState = "in service";
            data.put("phone number", ""+telman.getLine1Number());
        }
        if (serviceState.getState() == ServiceState.STATE_OUT_OF_SERVICE) {
            strState = "out of service";
        }
        if (serviceState.getState() == ServiceState.STATE_POWER_OFF) {
            strState = "power off";
        }
        data.put("state", strState);
        if (serviceState.getIsManualSelection()) {
            data.put("manualSet", "true");
        } else {
            data.put("manualSet", "false");
        }
        this.msgHandler.sendSensorData("service state", data);
    }
        
    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
    	Map<String, String> data = new HashMap<String, String>();
    	data.put("CDMA dBm",""+signalStrength.getCdmaDbm());
    	data.put("EVDO dBm", ""+signalStrength.getEvdoDbm());
    	data.put("GSM signal strength", ""+signalStrength.getGsmSignalStrength());
    	data.put("GSM bit error rate", ""+signalStrength.getGsmBitErrorRate());
    	this.msgHandler.sendSensorData("signal strength", data);
    	super.onSignalStrengthsChanged(signalStrength);
    }
}
