package nl.sense_os.datastorageengine;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.exceptions.RealmException;
import nl.sense_os.service.shared.SensorDataPoint;

public class RealmSource implements Source {

    private Realm realm = null;

    private String id = null;
    private String name = null;
    private JSONObject meta = null;
    private String deviceId = null; // device UUID or some other UUID. TODO: rename to deviceId?
    private String userId = null;
    private String csId = null;
    private boolean synced = false;

    protected RealmSource(Realm realm, String id, String name, JSONObject meta, String deviceId, String userId, String csId, boolean synced) {
        this.realm = realm;

        this.id = id;
        this.name = name;
        this.meta = meta;
        this.deviceId = deviceId;
        this.userId = userId;
        this.csId = csId;
        this.synced = synced;
    }

    public String getUserId() {
        return userId;
    }

    public String getCsId() {
        return csId;
    }

    public void setCsId(String csId) throws DatabaseHandlerException {
        this.csId = csId;
        this.synced = false; // mark as dirty

        saveChanges();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) throws DatabaseHandlerException {
        this.deviceId = deviceId;
        this.synced = false; // mark as dirty

        saveChanges();
    }

    public JSONObject getMeta() {
        return meta;
    }

    public void setMeta(JSONObject meta) throws DatabaseHandlerException {
        this.meta = meta;
        this.synced = false; // mark as dirty

        saveChanges();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws DatabaseHandlerException {
        this.name = name;
        this.synced = false; // mark as dirty

        saveChanges();
    }

    public String getId() {
        return id;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) throws DatabaseHandlerException {
        this.synced = synced;

        saveChanges();
    }

    @Override
    public Sensor createSensor(String name, SensorDataPoint.DataType dataType, SensorOptions options) throws DatabaseHandlerException {
        String id = UUID.randomUUID().toString();
        String csId = null;  // must be filled out by the database syncer
        boolean synced = false;
        Sensor sensor = new RealmSensor(realm, id, name, userId, this.id, dataType, csId, options, synced);

        RealmModelSensor realmSensor = RealmModelSensor.fromSensor(sensor);

        realm.beginTransaction();
        try {
            realm.copyToRealm(realmSensor);
        }
        catch (RealmException err) {
            if (err.toString().indexOf("Primary key constraint broken") != -1) {
                throw new DatabaseHandlerException("Cannot create sensor. A sensor with id " + id + " already exists.");
            }
            else {
                throw err;
            }
        }
        realm.commitTransaction();

        return sensor;
    }

    @Override
    public Sensor getSensor(String name) throws JSONException, DatabaseHandlerException {
        realm.beginTransaction();

        RealmModelSensor realmSensor = realm
                .where(RealmModelSensor.class)
                .equalTo("sourceId", this.id)
                .equalTo("name", name)
                .findFirst();

        realm.commitTransaction();

        if (realmSensor == null) {
            throw new DatabaseHandlerException("Sensor not found. Sensor with name " + name + " does not exist.");
        }

        return RealmModelSensor.toSensor(realm, realmSensor);
    }

    @Override
    public List<Sensor> getSensors() throws JSONException {
        // query results
        realm.beginTransaction();
        RealmResults<RealmModelSensor> results = realm
                .where(RealmModelSensor.class)
                .equalTo("sourceId", this.id)
                .findAll();
        realm.commitTransaction();

        // convert to Sensor
        List<Sensor> sensors = new ArrayList<>();
        Iterator<RealmModelSensor> iterator = results.iterator();
        while (iterator.hasNext()) {
            sensors.add(RealmModelSensor.toSensor(realm, iterator.next()));
        }
        // TODO: figure out what is the most efficient way to loop over the results

        return sensors;
    }

    /**
     * Update Source in database with the info of the given Source object.
     * Throws an exception if it fails to updated.
     */
    protected void saveChanges() throws DatabaseHandlerException {
        realm.beginTransaction();

        // get the existing source (just to throw an error if it doesn't exist)
        RealmModelSource realmSource = realm
                .where(RealmModelSource.class)
                .equalTo("id", this.id)
                .findFirst();

        if (realmSource == null) {
            throw new DatabaseHandlerException("Cannot update source: source doesn't yet exist.");
        }

        // update the source
        realm.copyToRealmOrUpdate(RealmModelSource.fromSource(this));

        realm.commitTransaction();
    }
}
