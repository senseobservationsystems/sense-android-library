package nl.sense_os.datastorageengine;

import org.json.JSONObject;

public class SensorOptions {
    private JSONObject meta = null;
    private Boolean uploadEabled = null;
    private Boolean downloadEnabled = null;
    private Boolean persist= null;

    public SensorOptions(JSONObject meta, Boolean uploadEabled, Boolean downloadEnabled, Boolean persist) {
        this.meta = meta;
        this.uploadEabled = uploadEabled;
        this.downloadEnabled = downloadEnabled;
        this.persist = persist;
    }

    public JSONObject getMeta() {
        return meta;
    }

    public void setMeta(JSONObject meta) {
        this.meta = meta;
    }

    public Boolean getUploadEabled() {
        return uploadEabled;
    }

    public void setUploadEabled(Boolean uploadEabled) {
        this.uploadEabled = uploadEabled;
    }

    public Boolean getDownloadEnabled() {
        return downloadEnabled;
    }

    public void setDownloadEnabled(Boolean downloadEnabled) {
        this.downloadEnabled = downloadEnabled;
    }

    public Boolean getPersist() {
        return persist;
    }

    public void setPersist(Boolean persist) {
        this.persist = persist;
    }
}
