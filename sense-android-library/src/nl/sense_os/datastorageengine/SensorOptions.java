package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Object with sensor options.
 * All options are optional and can be left null.
 * When setting sensor options via Sensor.setOptions(options) the options being null are ignored.
 */
public class SensorOptions implements Cloneable {
    private JSONObject mMeta = null;
    private Boolean mUploadEnabled = null;
    private Boolean mDownloadEnabled = null;
    private Boolean mPersistLocally = null;

    public SensorOptions() {};

    public SensorOptions(JSONObject meta, Boolean uploadEnabled, Boolean downloadEnabled, Boolean persistLocally) {
        this.mMeta = meta;
        this.mUploadEnabled = uploadEnabled;
        this.mDownloadEnabled = downloadEnabled;
        this.mPersistLocally = persistLocally;
    }

    public JSONObject getMeta() {
        return mMeta;
    }

    public void setMeta(JSONObject meta) {
        this.mMeta = meta;
    }

    public Boolean isUploadEnabled() {
        return mUploadEnabled;
    }

    public void setUploadEnabled(Boolean uploadEnabled) {
        this.mUploadEnabled = uploadEnabled;
    }

    public Boolean isDownloadEnabled() {
        return mDownloadEnabled;
    }

    public void setmDownloadEnabled(Boolean mDownloadEnabled) {
        this.mDownloadEnabled = mDownloadEnabled;
    }

    public Boolean isPersistLocally() {
        return mPersistLocally;
    }

    public void setPersistLocally(Boolean mPersistLocally) {
        this.mPersistLocally = mPersistLocally;
    }

    public SensorOptions clone () {
        return merge(this);
    };

    /**
     * Merge two or more options objects.
     * The fields in `options` which are `null` will be ignored.
     * @param options
     * @return Returns a cloned SensorOptions object having merge the options of the provided sensors
     */
    public static SensorOptions merge (SensorOptions ...options) {
        SensorOptions merged = new SensorOptions();

        for (SensorOptions o : options) {
            if (o.mMeta != null) {
                try {
                    // This should not throw an exception in practice as we're
                    // cloning an existing, valid JSONObject.
                    merged.mMeta = new JSONObject(o.mMeta.toString()); // create a clone of the JSON
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (o.mDownloadEnabled != null) {
                merged.mDownloadEnabled = o.mDownloadEnabled;
            }

            if (o.mUploadEnabled != null) {
                merged.mUploadEnabled = o.mUploadEnabled;
            }

            if (o.mPersistLocally != null) {
                merged.mPersistLocally = o.mPersistLocally;
            }
        }
        return merged;
    }
}
