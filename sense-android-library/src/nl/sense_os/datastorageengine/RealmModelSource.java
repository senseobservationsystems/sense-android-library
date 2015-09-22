package nl.sense_os.datastorageengine;

import org.json.JSONException;
import org.json.JSONObject;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

public class RealmModelSource extends RealmObject {

    @PrimaryKey
    private String id = null;

    @Index
    private String name = null;
    private String meta = null; // stringified JSON object

    @Index
    private String uuid = null; // device UUID or some other UUID
    private String userId = null;
    private String csId = null;

    private boolean synced = false;

    public RealmModelSource() {}

    public RealmModelSource(String id, String name, JSONObject meta, String uuid, String userId, String csId, boolean synced) {
        this.id = id;
        this.name = name;
        this.meta = meta != null ? meta.toString() : null;
        this.uuid = uuid;
        this.userId = userId;
        this.csId = csId;
        this.synced = synced;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    /**
     * Returns the meta information of a RealmSource source as JSONObject
     * @param realmSource
     * @return Returns JSONObject with meta information
     */
    public static JSONObject getMeta (RealmModelSource realmSource) throws JSONException {
        String meta = realmSource.getMeta();
        return meta != null ? new JSONObject(meta) : null;
    }

    /**
     * Returns meta information as a stringified JSON object. Deserialize the string like:
     *
     *     RealmSource.getMeta(source);
     *
     * @return stringified JSON object
     */
    public String getMeta() {
        return meta;
    }

    /**
     * Set meta information for this source
     * @param meta Must contain a stringified JSON object.
     */
    public void setMeta(String meta) {
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

    /**
     * Convert a RealmSource into a Source
     * @param databaseHandler
     * @param realmSource
     * @return Returns a Source
     */
    public static Source toSource (RealmDatabaseHandler databaseHandler, RealmModelSource realmSource) throws JSONException {
        String meta = realmSource.getMeta();
        return new RealmSource(
                databaseHandler,
                realmSource.getId(),
                realmSource.getName(),
                meta != null ? new JSONObject(meta) : null,
                realmSource.getUuid(),
                realmSource.getUserId(),
                realmSource.getCsId(),
                realmSource.isSynced()
        );
    }

    /**
     * Create a RealmSource from a Source
     * @param source  A Source
     * @return Returns a RealmSource
     */
    public static RealmModelSource fromSource (Source source) {
        return new RealmModelSource(
                source.getId(),
                source.getName(),
                source.getMeta(),
                source.getUuid(),
                source.getUserId(),
                source.getCsId(),
                source.isSynced()
        );
    }

}
