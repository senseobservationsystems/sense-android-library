package nl.sense_os.datastorageengine.realm.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import nl.sense_os.datastorageengine.DataPoint;

/**
 * The class RealmDataPoint contains a serializable form of DataPoint, suited for storing
 * a DataPoint in Realm.
 *
 * Data values are stored a stringified JSON.
 *
 * The class contains two static helper functions to convert from and to DataPoint:
 *     RealmDataPoint.fromDataPoint()
 *     RealmDataPoint.toDataPoint()
 */
public class RealmModelDataPoint extends RealmObject {
    @PrimaryKey
    private String id = null; // Compound key purely used to identify the data point in Realm

    @Index
    private long sensorId = -1;

    private String value = null; // Stringified JSON containing the value

    @Index
    private long date = -1;

    private boolean existsInRemote = false;

    public RealmModelDataPoint() {}

    public RealmModelDataPoint(long sensorId, String value, long date, boolean existsInRemote) {
        this.id = RealmModelDataPoint.getCompoundKey(sensorId, date);
        this.sensorId = sensorId;
        this.value = value;
        this.date = date;
        this.existsInRemote = existsInRemote;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getSensorId() {
        return sensorId;
    }

    public void setSensorId(long sensorId) {
        this.sensorId = sensorId;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public boolean getExistsInRemote() {
        return existsInRemote;
    }

    public void setExistsInRemote(boolean existsInRemote) {
        this.existsInRemote = existsInRemote;
    }

    /**
     * Create a unique id for a DataPoint, consisting of the sensorId and the date.
     * @param sensorId
     * @param date
     * @return Returns a string "<sensorId>:<date>"
     */
    public static String getCompoundKey (long sensorId, long date) {
        return sensorId + ":" + date;
    }

    /**
     * Convert a RealmDataPoint into a DataPoint
     * @param realmDataPoint
     * @return Returns a DataPoint
     */
    public static DataPoint toDataPoint (RealmModelDataPoint realmDataPoint) throws JSONException {
        return new DataPoint(
                realmDataPoint.getSensorId(),
                parseValue(realmDataPoint.getValue()),
                realmDataPoint.getDate(),
                realmDataPoint.getExistsInRemote());
    }

    /**
     * Create a RealmDataPoint from a DataPoint
     * @param dataPoint  A DataPoint
     * @return Returns a RealmDataPoint
     */
    public static RealmModelDataPoint fromDataPoint (DataPoint dataPoint) {
        return new RealmModelDataPoint(
                dataPoint.getSensorId(),
                stringifyValue(dataPoint.getValue()),
                dataPoint.getTime(),
                dataPoint.existsInCS());
    }

    /**
     * Quote a string and escape quotes.
     * @param str
     * @return Quoted and escaped string.
     */
    protected static String quote(String str) {
        return JSONObject.quote(str);
    };

    /**
     * Unquote a quoted string, and unescape escaped characters.
     * @param quotedStr
     * @return
     */
    protected static String unquote(String quotedStr) throws JSONException {
        return String.valueOf(parseValue(quotedStr));
    };

    /**
     * Stringify a value, like a String, float, int, etc.
     * @param obj
     * @return
     */
    public static String stringifyValue(Object obj) {
        if (obj == null) {
            return null;
        }
        else if (obj instanceof String) {
            return quote((String)obj);
        }
        else {
            return String.valueOf(obj);
        }
    };

    /**
     * Parse a stringified value (String, float, int, ...)
     * @param str
     * @return
     */
    public static Object parseValue(String str) throws JSONException {
        return new JSONTokener(str).nextValue();
    };

}
