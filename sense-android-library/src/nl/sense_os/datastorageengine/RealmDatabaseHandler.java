package nl.sense_os.datastorageengine;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;


/**
 * RealmDatabaseHandler handles local storage of DataPoints, Sensors, and Sources.
 * It stores the data in a local Realm database.
 * It needs to be instantiated with an Android Context.
 *
 * Example usage:
 *
 *     DatabaseHandler databaseHandler = new RealmDatabaseHandler(getContext());
 *
 *     Sensor sensor = databaseHandler.getSensor(sourceId, sensorName);
 *
 *     sensor.insertDataPoint(1234, new Date().getTime());
 *
 *     long startDate = 1388534400000; // 2014-01-01
 *     long startDate = 1420070400000; // 2015-01-01
 *     List<DataPoint> data = sensor.getDataPoints(startDate, endDate, 1000, SORT_ORDER.ASC);
 *
 *     databaseHandler.close();
 *
 */
public class RealmDatabaseHandler implements DatabaseHandler {

    private Realm realm = null;

    public RealmDatabaseHandler(Context context) {
        realm = Realm.getInstance(context);
    }

    /**
     * Close the database connection.
     * @throws Exception
     */
    @Override
    protected void finalize() throws Exception {
        // close realm
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    /**
     * Close the DatabaseHandler. This will neatly close the database connection to Realm.
     * @throws Exception
     */
    public void close () throws Exception {
        finalize();
    }

    /**
     * Create a new source and store it in the local database
     */
    public Source createSource(String id, String name, JSONObject meta, String deviceId, String userId, String csId) {
        boolean synced = false;
        Source source = new RealmSource(realm, id, name, meta, deviceId, userId, csId, synced);

        RealmModelSource realmSource = RealmModelSource.fromSource(source);

        realm.beginTransaction();
        realm.copyToRealm(realmSource);
        realm.commitTransaction();

        return source;
    }

    /**
     * Returns a list of sources based on the specified criteria.
     * @param sourceName    Name of the source
     * @param deviceId      Device identifier
     * @return list of source objects that correspond to the specified criteria.
     */
    public List<Source> getSources (String sourceName, String deviceId) throws JSONException {
        // query results
        realm.beginTransaction();
        RealmResults<RealmModelSource> results = realm
                .where(RealmModelSource.class)
                .equalTo("name", sourceName)
                .equalTo("deviceId", deviceId)
                .findAll();
        realm.commitTransaction();

        // convert to Source
        List<Source> sources = new ArrayList<>();
        Iterator<RealmModelSource> iterator = results.iterator();
        while (iterator.hasNext()) {
            sources.add(RealmModelSource.toSource(realm, iterator.next()));
        }
        // TODO: figure out what is the most efficient way to loop over the results

        return sources;
    }
}
