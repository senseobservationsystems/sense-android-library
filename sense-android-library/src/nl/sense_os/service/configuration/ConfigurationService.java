/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.configuration;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SensePrefs.Main.Ambience;
import nl.sense_os.service.constants.SensePrefs.Main.DevProx;
import nl.sense_os.service.constants.SensePrefs.Main.Location;
import nl.sense_os.service.constants.SensePrefs.Main.Motion;
import nl.sense_os.service.constants.SensePrefs.Main.PhoneState;
import nl.sense_os.service.constants.SensePrefs.Status;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * IntentService to handle changes in the configuration. When the service is started, it will look
 * into the requirement and set the preferences accordingly.<br/>
 * <br/>
 * Example of the requirement:
 * 
 * <pre>
 *    {
 *      "position":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000,
 *        "automatic_gps": true
 *      },
 *      "traveled distance 1h":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "Location":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "service state":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "battery sensor":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "data connection":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "connection type":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "call state":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "unread msg":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "signal strength":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "proximity":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "screen activity":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "ip address":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "bluetooth_discovery":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "bluetooth neighbours count":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "wifi scan":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "audio_spectrum":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "light":{
 *        "sampling_rate":60000,
 *        "sync_rate":60000
 *      },
 *      "camera_light":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "noise_sensor":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "loudness":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "pressure":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "orientation":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "linear acceleration":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "accelerometer":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "motion_energy":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "attachToMyrianode":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "Availability":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "Reachability":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "Activity":{
 *        "sampling_rate":1800000,
 *        "sync_rate":1800000
 *      },
 *      "ambient_temperature":{
 *        "sampling_rate":30000,
 *        "sync_rate":60000
 *      }
 *    }
 * </pre>
 *
 * @author Ahmy Yulrizka <ahmy@sense-os.nl>
 */
public class ConfigurationService extends IntentService {

    private Integer samplingRate = null;
    private Integer syncRate = null;
    private SharedPreferences statusPrefs;
    private SharedPreferences mainPrefs;

    public ConfigurationService() {
        super("ConfigurationService");
    }

    /**
     * Handles accelerometer requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable unregister between sample sensing</li>
     * <li>enable screenoff_fix</li>
     * <li>turn on Motion sensing</li>
     * </ol>
     * 
     * @param req
     *            accelerometer requirement
     */
    private void handleAccelerometerReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(Motion.UNREG, true).putBoolean(Motion.SCREENOFF_FIX, true)
                    .commit();

            statusPrefs.edit().putBoolean(Status.MOTION, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles Activity requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable unregister between sample sensing</li>
     * <li>enable screenoff_fix</li>
     * <li>turn on Motion sensing</li>
     * </ol>
     * 
     * @param req
     *            Activity requirement
     */
    private void handleActivityReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(Motion.UNREG, true).putBoolean(Motion.SCREENOFF_FIX, true)
                    .commit();

            statusPrefs.edit().putBoolean(Status.MOTION, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles attachToMyrianode requirements.
     * <ol>
     * <li>enable attachToMyrianode preference</li>
     * </ol>
     * 
     * @param req
     *            attachTo requirement
     */
    private void handleAttachToMyrianodeReq(JSONObject req) {
        mainPrefs.edit().putBoolean(Advanced.LOCATION_FEEDBACK, true).commit();
    }

    /**
     * Handles audio spectrum requirements.
     * <ol>
     * <li>update sampling_rate if necessary
     * <li>enable mic sensing</li>
     * <li>enable audio_specturm sensing</li>
     * <li>turn on Ambience sensing</li>
     * </ol>
     * 
     * @param req
     *            audio_spectrum sensor requirement
     */
    private void handleAudioSpectrumReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(Ambience.AUDIO_SPECTRUM, true)
                    .putBoolean(Ambience.MIC, true).commit();

            statusPrefs.edit().putBoolean(Status.AMBIENCE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles Availability requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable GPS provider</li>
     * <li>enable Network Provider - enable Automatic GPS</li>
     * <li>turn on Location sensing</li>
     * </ol>
     * 
     * @param req
     *            Availability sensor requirement
     */
    private void handleAvailabilityReq(JSONObject req) {
        handleLocationReq(req);
    }

    /**
     * Handles Battery Sensor requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable Battery State sensing</li>
     * <li>turn on Phone State sensing</li>
     * </ol>
     * 
     * @param req
     *            Battery sensor requirement
     */
    private void handleBatterySensorReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(PhoneState.BATTERY, true).commit();
            statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    /**
     * Handles App Info Sensor requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable Battery State sensing</li>
     * <li>turn on Phone State sensing</li>
     * </ol>
     * 
     * @param req
     *            App info sensor requirement
     */
    private void handleAppInfoSensorReq(JSONObject req) {
        try {
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(PhoneState.APP_INFO, true).commit();
            statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles Bluetooth Discovery requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable bluetooth sensing</li>
     * <li>turn on Device Proximity sensing</li>
     * </ol>
     * 
     * @param req
     *            Bluetooth discovery sensor requirement
     */
    private void handleBluetoothDiscoveryReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(DevProx.BLUETOOTH, true).commit();
            statusPrefs.edit().putBoolean(Status.DEV_PROX, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles bluetooth neighbours count requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable bluetooth sensing</li>
     * <li>turn on Device Proximity sensing</li>
     * </ol>
     * 
     * @param req
     *            bluetooth neighbours count sensor requirement
     */
    private void handleBluetoothNeighboursCountReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(DevProx.BLUETOOTH, true).commit();
            statusPrefs.edit().putBoolean(Status.DEV_PROX, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles Call State requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable call state sensing</li>
     * <li>turn on Phone State sensing</li>
     * </ol>
     * 
     * @param req
     *            Call state sensor requirement
     */
    private void handleCallStateReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(PhoneState.CALL_STATE, true).commit();
            statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles camera light requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable camera light sensing</li>
     * <li>turn on Ambience sensing</li>
     * </ol>
     * 
     * @param req
     *            camera_light sensor requirement
     */
    private void handleCameraLightReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(Ambience.CAMERA_LIGHT, true).commit();

            statusPrefs.edit().putBoolean(Status.AMBIENCE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles Connection Type requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable data connection sensing</li>
     * <li>turn on Phone State sensing</li>
     * </ol>
     * 
     * @param req
     *            Connection type sensor requirement
     */
    private void handleConnectionTypeReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(PhoneState.DATA_CONNECTION, true).commit();
            statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles Data Connection requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable data connection sensing</li>
     * <li>turn on Phone State sensing</li>
     * </ol>
     * 
     * @param req
     *            Data Connection sensor requirement
     */
    private void handleDataConnectionReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(PhoneState.DATA_CONNECTION, true).commit();
            statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles IP address requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable IP address sensing</li>
     * <li>turn on Phone State sensing</li>
     * </ol>
     * 
     * @param req
     *            IP Address sensor requirement
     */
    private void handleIPAddressReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(PhoneState.IP_ADDRESS, true).commit();
            statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles light requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable light sensing</li>
     * <li>turn on Ambience sensing</li>
     * </ol>
     * 
     * @param req
     *            light sensor requirement
     */
    private void handleLightReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(Ambience.LIGHT, true).commit();

            statusPrefs.edit().putBoolean(Status.AMBIENCE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles linear acceleration requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable unregister between sample sensing</li>
     * <li>enable screenoff_fix</li>
     * <li>turn on Motion sensing</li>
     * </ol>
     * 
     * @param req
     *            linear acceleration requirement
     */
    private void handleLinearAccelerationReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(Motion.UNREG, true).putBoolean(Motion.SCREENOFF_FIX, true)
                    .commit();

            statusPrefs.edit().putBoolean(Status.MOTION, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles 1h requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable GPS provider</li>
     * <li>enable Network Provider - enable Automatic GPS</li>
     * <li>turn on Location sensing</li>
     * </ol>
     * 
     * @param req
     *            Location sensor requirement
     */
    private void handleLocationReq(JSONObject req) {
        handlePositionReq(req);
    }

    /**
     * Handles loudness sensor requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable Mic sensing</li>
     * <li>turn on Ambience sensing</li>
     * </ol>
     * 
     * @param req
     *            loudness requirement
     */
    private void handleLoudnessReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(Ambience.MIC, true).commit();
            statusPrefs.edit().putBoolean(Status.AMBIENCE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles motion energy requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable unregister between sample sensing</li>
     * <li>enable motion energy sensor</li>
     * <li>enable screenoff_fix</li>
     * <li>turn on Motion sensing</li>
     * </ol>
     * 
     * @param req
     *            accelerometer requirement
     */
    private void handleMotionEnergyReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(Motion.UNREG, true).putBoolean(Motion.SCREENOFF_FIX, true)
                    .commit();

            statusPrefs.edit().putBoolean(Status.MOTION, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles noise sensor requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable Mic sensing</li>
     * <li>turn on Ambience sensing</li>
     * </ol>
     * 
     * @param req
     *            noise_sensor requirement
     */
    private void handleNoiseSensorReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(Ambience.MIC, true).commit();

            statusPrefs.edit().putBoolean(Status.AMBIENCE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles orientation sensor requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable unregister between sample sensing</li>
     * <li>enable screenoff_fix</li>
     * <li>turn on Motion sensing</li>
     * </ol>
     * 
     * @param req
     *            orientation requirement
     */
    private void handleOrientationReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(Motion.UNREG, true).putBoolean(Motion.SCREENOFF_FIX, true)
                    .commit();

            statusPrefs.edit().putBoolean(Status.MOTION, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles position requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable GPS provider</li>
     * <li>enable Network Provider - enable Automatic GPS</li>
     * <li>turn on Location sensing</li>
     * </ol>
     * 
     * @param req
     *            Position sensor requirement
     */
    private void handlePositionReq(JSONObject req) {
        // TODO: select GPS / Network base on accuracy criteria ?
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(Location.GPS, true).putBoolean(Location.NETWORK, true)
                    .putBoolean(Location.AUTO_GPS, true).commit();

            statusPrefs.edit().putBoolean(Status.LOCATION, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles pressure sensor requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable pressure sensing</li>
     * <li>turn on Ambience sensing</li>
     * </ol>
     * 
     * @param req
     *            pressure requirement
     */
    private void handlePressureReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(Ambience.PRESSURE, true).commit();
            statusPrefs.edit().putBoolean(Status.AMBIENCE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles Proximity requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable proximity sensing</li>
     * <li>turn on Phone State sensing</li>
     * </ol>
     * 
     * @param req
     *            Proximity sensor requirement
     */
    private void handleProximityReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(PhoneState.PROXIMITY, true).commit();
            statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles Reachability requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable service state sensing</li>
     * <li>enable call state sensing</li>
     * <li>turn on Phone State sensing</li>
     * </ol>
     * 
     * @param req
     *            Reachability sensor requirement
     */
    private void handleReachabilityReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(PhoneState.SERVICE_STATE, true)
                    .putBoolean(PhoneState.CALL_STATE, true).commit();

            statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles Screen Activity requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable screen activity sensing</li>
     * <li>turn on Phone State sensing</li>
     * </ol>
     * 
     * @param req
     *            Screen activity sensor requirement
     */
    private void handleScreenActiviyReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(PhoneState.SCREEN_ACTIVITY, true).commit();
            statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles Service State requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable service state sensing</li>
     * <li>turn on Phone State sensing</li>
     * </ol>
     * 
     * @param req
     *            ServiceState sensor requirement
     */
    private void handleServiceStateReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(PhoneState.SERVICE_STATE, true).commit();
            statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles Signal Strength requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable signal strength sensing</li>
     * <li>turn on Phone State sensing</li>
     * </ol>
     * 
     * @param req
     *            Signal Strength sensor requirement
     */
    private void handleSignalStrengthReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(PhoneState.SIGNAL_STRENGTH, true).commit();
            statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles travel distance 1h requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable GPS provider</li>
     * <li>enable Network Provider - enable Automatic GPS</li>
     * <li>turn on Location sensing</li>
     * </ol>
     * 
     * @param req
     *            travel distance 1h sensor requirement
     */
    private void handleTraveledDistance1HReq(JSONObject req) {
        handlePositionReq(req);
    }

    /**
     * Handles Call State requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable call state sensing</li>
     * <li>turn on Phone State sensing</li>
     * </ol>
     * 
     * @param req
     *            Unread message sensor requirement
     */
    private void handleUnreadMsgReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(PhoneState.UNREAD_MSG, true).commit();
            statusPrefs.edit().putBoolean(Status.PHONESTATE, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles wifi scan requirements.
     * <ol>
     * <li>update sampling_rate if necessary</li>
     * <li>enable wifi sensing</li>
     * <li>turn on Device Proximity sensing</li>
     * </ol>
     * 
     * @param req
     *            wifi scan sensor requirement
     */
    private void handleWifiScanReq(JSONObject req) {
        try {
            if (req.has("sampling_rate"))
                updateSamplingRate(req.getInt("sampling_rate"));
            if (req.has("sync_rate"))
                updateSyncRate(req.getInt("sync_rate"));

            mainPrefs.edit().putBoolean(DevProx.WIFI, true).commit();
            statusPrefs.edit().putBoolean(Status.DEV_PROX, true).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        statusPrefs = getSharedPreferences(SensePrefs.STATUS_PREFS, Context.MODE_PRIVATE);
        mainPrefs = getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);

        try {
            String reqStr = intent.getStringExtra(RequirementReceiver.EXTRA_REQUIREMENTS);
            JSONObject requirements = new JSONObject(reqStr).getJSONObject("requirements");

            if (requirements.length() == 0)
                return;

            samplingRate = null;
            syncRate = null;

            // process position
            if (requirements.has("traveled distance 1h"))
                handleTraveledDistance1HReq(requirements.getJSONObject("traveled distance 1h"));
            if (requirements.has("Location"))
                handleLocationReq(requirements.getJSONObject("Location"));
            if (requirements.has("position"))
                handlePositionReq(requirements.getJSONObject("position"));
            if (requirements.has("Availability"))
                handleAvailabilityReq(requirements.getJSONObject("Availability"));

            // Phone state sensing
            if (requirements.has("service state"))
                handleServiceStateReq(requirements.getJSONObject("service state"));
            if (requirements.has("battery sensor"))
                handleBatterySensorReq(requirements.getJSONObject("battery sensor"));
            if (requirements.has("app info sensor"))
            	handleAppInfoSensorReq(requirements.getJSONObject("app info sensor"));
            if (requirements.has("data connection"))
                handleDataConnectionReq(requirements.getJSONObject("data connection"));
            if (requirements.has("connection type"))
                handleConnectionTypeReq(requirements.getJSONObject("connection type"));
            if (requirements.has("call state"))
                handleCallStateReq(requirements.getJSONObject("call state"));
            if (requirements.has("unread msg"))
                handleUnreadMsgReq(requirements.getJSONObject("unread msg"));
            if (requirements.has("signal strength"))
                handleSignalStrengthReq(requirements.getJSONObject("signal strength"));
            if (requirements.has("proximity"))
                handleProximityReq(requirements.getJSONObject("proximity"));
            if (requirements.has("screen activity"))
                handleScreenActiviyReq(requirements.getJSONObject("screen activity"));
            if (requirements.has("ip address"))
                handleIPAddressReq(requirements.getJSONObject("ip address"));
            if (requirements.has("Reachability"))
                handleReachabilityReq(requirements.getJSONObject("Reachability"));

            // device proximity sensing
            if (requirements.has("bluetooth_discovery"))
                handleBluetoothDiscoveryReq(requirements.getJSONObject("bluetooth_discovery"));
            if (requirements.has("bluetooth neighbours count"))
                handleBluetoothNeighboursCountReq(requirements
                        .getJSONObject("bluetooth neighbours count"));
            if (requirements.has("wifi scan"))
                handleWifiScanReq(requirements.getJSONObject("wifi scan"));

            // Ambience sensing
            if (requirements.has("audio_spectrum"))
                handleAudioSpectrumReq(requirements.getJSONObject("audio_spectrum"));
            if (requirements.has("light"))
                handleLightReq(requirements.getJSONObject("light"));
            if (requirements.has("camera_light"))
                handleCameraLightReq(requirements.getJSONObject("camera_light"));
            if (requirements.has("noise_sensor"))
                handleNoiseSensorReq(requirements.getJSONObject("noise_sensor"));
            if (requirements.has("loudness"))
                handleLoudnessReq(requirements.getJSONObject("loudness"));
            if (requirements.has("pressure"))
                handlePressureReq(requirements.getJSONObject("pressure"));

            // Motion Sensing
            if (requirements.has("orientation"))
                handleOrientationReq(requirements.getJSONObject("orientation"));
            if (requirements.has("linear acceleration"))
                handleLinearAccelerationReq(requirements.getJSONObject("linear acceleration"));
            if (requirements.has("accelerometer"))
                handleAccelerometerReq(requirements.getJSONObject("accelerometer"));
            if (requirements.has("motion_energy"))
                handleMotionEnergyReq(requirements.getJSONObject("motion_energy"));
            if (requirements.has("Activity"))
                handleActivityReq(requirements.getJSONObject("Activity"));

            // Location Feedback (myrianode)
            if (requirements.has("attachToMyrianode"))
                handleAttachToMyrianodeReq(requirements.getJSONObject("attachToMyrianode"));

            // update sampling rate if necessary
            if (samplingRate != null) {
                Integer preference_rate = null;

                if (samplingRate < 10) // realtime (< 10 sec)
                    preference_rate = -2;
                else if (samplingRate >= 10 && samplingRate < 60) // often (10-60) sec
                    preference_rate = -1;
                else if (samplingRate >= 60 && samplingRate < 900) // normal (1-15 min)
                    preference_rate = 0;
                else if (samplingRate >= 900) // rarely (> 15 min)
                    preference_rate = 1;

                if (preference_rate != null) {
                    String oldValue = mainPrefs.getString(Main.SAMPLE_RATE, SensePrefs.Main.SampleRate.NORMAL);

                    // only update if its have higher rate
                    if (preference_rate < Integer.parseInt(oldValue)) {
                        mainPrefs.edit().putString(Main.SAMPLE_RATE, preference_rate.toString())
                                .commit();
                    }
                }
            }

            // update common sense sync rate if necessary
            if (syncRate != null) {
                Integer preference_rate = null;

                if (syncRate < 60) // realtime (< 1 min)
                    preference_rate = -2;
                else if (syncRate >= 60 && syncRate < 300) // often (1)
                    preference_rate = -1;
                else if (syncRate >= 300 && syncRate < 900) // normal (5-29 min)
                    preference_rate = 0;
                else if (syncRate >= 900) // Eco-mode (30 min)
                    preference_rate = 1;

                if (preference_rate != null) {
                    String oldValue = mainPrefs.getString(Main.SYNC_RATE, "0");

                    // only update if its have higher rate
                    if (preference_rate < Integer.parseInt(oldValue)) {
                        mainPrefs.edit().putString(Main.SYNC_RATE, preference_rate.toString())
                                .commit();
                    }
                }
            }

            // apply change by starting the service
            startService(new Intent(getString(R.string.action_sense_service)));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates sampling rate
     * 
     * @param newRate
     *            new sampling rate
     */
    private void updateSamplingRate(int newRate) {
        if ((this.samplingRate == null) || (newRate > 0 && newRate < this.samplingRate)) {
            this.samplingRate = newRate;
        }
    }

    /**
     * Updates sync rate to common sense
     * 
     * @param newRate
     *            new sync rate
     */
    private void updateSyncRate(int newRate) {
        if ((this.syncRate == null) || (newRate > 0 && newRate < this.syncRate)) {
            this.syncRate = newRate;
        }
    }
}
