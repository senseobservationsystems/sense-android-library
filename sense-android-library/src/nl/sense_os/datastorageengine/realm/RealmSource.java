package nl.sense_os.datastorageengine.realm;

import org.json.JSONException;
import org.json.JSONObject;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmSource extends RealmObject {

    @PrimaryKey
    private String id = null;

    private String name = null;
    private String meta = null; // stringified JSON object
    private String uuid = null; // device UUID or some other UUID
    private String csId = null;

    public RealmSource () {}

    public RealmSource(String id, String name, JSONObject meta, String uuid, String csId) {
        this.id = id;
        this.name = name;
        this.meta = meta != null ? meta.toString() : null;
        this.uuid = uuid;
        this.csId = csId;
    }

    public RealmSource(String id, String name, String uuid, String csId) {
        this.id = id;
        this.name = name;
        this.uuid = uuid;
        this.csId = csId;
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
    public static JSONObject getMeta (RealmSource realmSource) throws JSONException {
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
}
