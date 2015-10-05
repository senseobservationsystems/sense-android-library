package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Object with sensor options.
 * All options are optional and can be left null.
 * When setting sensor options via Sensor.setOptions(options) the options being null are ignored.
 */
public class SensorOptions implements Cloneable {
    private JSONObject meta = null;
    private Boolean uploadEnabled = null;
    private Boolean downloadEnabled = null;
    private Boolean persistLocally = null;

    public SensorOptions() {};

    public SensorOptions(JSONObject meta, Boolean uploadEnabled, Boolean downloadEnabled, Boolean persistLocally) {
        this.meta = meta;
        this.uploadEnabled = uploadEnabled;
        this.downloadEnabled = downloadEnabled;
        this.persistLocally = persistLocally;
    }

    public JSONObject getMeta() {
        return meta;
    }

    public void setMeta(JSONObject meta) {
        this.meta = meta;
    }

    public Boolean isUploadEnabled() {
        return uploadEnabled;
    }

    public void setUploadEnabled(Boolean uploadEnabled) {
        this.uploadEnabled = uploadEnabled;
    }

    public Boolean isDownloadEnabled() {
        return downloadEnabled;
    }

    public void setDownloadEnabled(Boolean downloadEnabled) {
        this.downloadEnabled = downloadEnabled;
    }

    public Boolean isPersistLocally() {
        return persistLocally;
    }

    public void setPersistLocally(Boolean persistLocally) {
        this.persistLocally = persistLocally;
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
            if (o.meta != null) {
                try {
                    // This should not throw an exception in practice as we're
                    // cloning an existing, valid JSONObject.
                    merged.meta = new JSONObject(o.meta.toString()); // create a clone of the JSON
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (o.downloadEnabled != null) {
                merged.downloadEnabled = o.downloadEnabled;
            }

            if (o.uploadEnabled != null) {
                merged.uploadEnabled = o.uploadEnabled;
            }

            if (o.persistLocally != null) {
                merged.persistLocally = o.persistLocally;
            }
        }

        return merged;
    }
}
