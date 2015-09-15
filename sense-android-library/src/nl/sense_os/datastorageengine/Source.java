package nl.sense_os.datastorageengine;


import org.json.JSONObject;

public class Source {

    private String id = null;
    private String name = null;
    private JSONObject meta = null;
    private String uuid = null; // device UUID or some other UUID
    private String csId = null;
    private boolean synced = false;

    public Source(String id, String name, JSONObject meta, String uuid, String csId, boolean synced) {
        this.id = id;
        this.name = name;
        this.meta = meta;
        this.uuid = uuid;
        this.csId = csId;
        this.synced = synced;
    }

    public Source (String id, String name, String uuid, String csId, boolean synced) {
        this.id = id;
        this.name = name;
        this.uuid = uuid;
        this.csId = csId;
        this.synced = synced;
    }

    public String getCsId() {
        return csId;
    }

    public void setCsId(String csId) {
        this.csId = csId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public JSONObject getMeta() {
        return meta;
    }

    public void setMeta(JSONObject meta) {
        this.meta = meta;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

}
