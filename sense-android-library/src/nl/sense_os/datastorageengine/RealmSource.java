package nl.sense_os.datastorageengine;


import org.json.JSONObject;

public class RealmSource implements Source {

    private RealmDatabaseHandler databaseHandler = null;

    private String id = null;
    private String name = null;
    private JSONObject meta = null;
    private String uuid = null; // device UUID or some other UUID. TODO: rename to deviceId?
    private String csId = null;
    private boolean synced = false;

    protected RealmSource(RealmDatabaseHandler databaseHandler, String id, String name, JSONObject meta, String uuid, String csId, boolean synced) {
        this.databaseHandler = databaseHandler;

        this.id = id;
        this.name = name;
        this.meta = meta;
        this.uuid = uuid;
        this.csId = csId;
        this.synced = synced;
    }

    public String getCsId() {
        return csId;
    }

    public void setCsId(String csId) throws DatabaseHandlerException {
        this.csId = csId;

        databaseHandler.updateSource(this);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) throws DatabaseHandlerException {
        this.uuid = uuid;

        databaseHandler.updateSource(this);
    }

    public JSONObject getMeta() {
        return meta;
    }

    public void setMeta(JSONObject meta) throws DatabaseHandlerException {
        this.meta = meta;

        databaseHandler.updateSource(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws DatabaseHandlerException {
        this.name = name;

        databaseHandler.updateSource(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) throws DatabaseHandlerException {
        this.id = id;

        databaseHandler.updateSource(this);
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) throws DatabaseHandlerException {
        this.synced = synced;

        databaseHandler.updateSource(this);
    }

}
