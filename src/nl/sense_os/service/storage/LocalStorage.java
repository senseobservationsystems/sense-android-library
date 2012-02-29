/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.storage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.provider.SNTP;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Storage for recent sensor data. The data is stored in the devices RAM memory, so this
 * implementation is more energy efficient than storing everything in flash. This does mean that
 * parsing the selection queries is quite a challenge. Only a very limited set of queries will work:
 * <ul>
 * <li>sensor_name = 'foo'</li>
 * <li>sensor_name != 'foo'</li>
 * <li>timestamp = foo</li>
 * <li>timestamp != foo</li>
 * <li>timestamp > foo</li>
 * <li>timestamp >= foo</li>
 * <li>timestamp < foo</li>
 * <li>timestamp <= foo</li>
 * <li>combinations of a sensor_name and a timestamp selection</li>
 * </ul>
 * 
 * @see ParserUtils
 * @see DataPoint
 */
public class LocalStorage {

    /**
     * Inner class that handles the creation of the SQLite3 database with the desired tables and
     * columns.
     */
    private static class DbHelper extends SQLiteOpenHelper {

        protected static final String DATABASE_NAME = "persitent_storage.sqlite3";
        protected static final int DATABASE_VERSION = 3;

        DbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            final StringBuilder sb = new StringBuilder("CREATE TABLE " + TABLE_PERSISTENT + "(");
            sb.append(BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT");
            sb.append(", " + DataPoint.SENSOR_NAME + " STRING");
            sb.append(", " + DataPoint.DISPLAY_NAME + " STRING");
            sb.append(", " + DataPoint.SENSOR_DESCRIPTION + " STRING");
            sb.append(", " + DataPoint.DATA_TYPE + " STRING");
            sb.append(", " + DataPoint.TIMESTAMP + " INTEGER");
            sb.append(", " + DataPoint.VALUE + " STRING");
            sb.append(", " + DataPoint.DEVICE_UUID + " STRING");
            sb.append(", " + DataPoint.TRANSMIT_STATE + " INTEGER");
            sb.append(");");
            db.execSQL(sb.toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVers, int newVers) {
            Log.w(TAG, "Upgrading '" + DATABASE_NAME + "' database from version " + oldVers
                    + " to " + newVers + ", which will destroy all old data");

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERSISTENT);
            onCreate(db);
        }
    }

    private static final String TAG = "Sense LocalStorage";

    // public static final String AUTHORITY = "nl.sense_os.service.provider.LocalStorage";
    private static final String TABLE_PERSISTENT = "persisted_values";
    private static final int VOLATILE_VALUES_URI = 1;
    private static final int PERSISTED_VALUES_URI = 2;
    private static final int REMOTE_VALUES_URI = 3;
    private static final int MAX_VOLATILE_VALUES = 100;
    public static final int QUERY_RESULTS_LIMIT = 10000;
    public static final int QUERY_RESULTS_LIMIT_EPI_MODE = 60;

    private static long count = 0;

    private static LocalStorage instance;

    private Context context;
    private DbHelper dbHelper;

    private final static Map<String, ContentValues[]> storage = new HashMap<String, ContentValues[]>();
    private final static Map<String, Integer> pointers = new HashMap<String, Integer>();

    /**
     * @param context
     *            Context for lazy creating the LocalStorage.
     * @return Singleton instance of the LocalStorage
     */
    public static LocalStorage getInstance(Context context) {
        // Log.v(TAG, "Get local storage instance...");
        if (null == instance) {
            instance = new LocalStorage(context.getApplicationContext());
        }
        return instance;
    }

    private LocalStorage(Context context) {
        // Log.v(TAG, "Construct new local storage instance...");
        this.context = context;
        dbHelper = new DbHelper(context);
    }

    public int delete(Uri uri, String where, String[] selectionArgs) {
        switch (matchUri(uri)) {
        case VOLATILE_VALUES_URI:
            throw new IllegalArgumentException("Cannot delete recent data points!");
        case PERSISTED_VALUES_URI:
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int result = db.delete(TABLE_PERSISTENT, where, selectionArgs);
            return result;
        case REMOTE_VALUES_URI:
            throw new IllegalArgumentException("Cannot delete values from CommonSense!");
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /**
     * @param dataPoint
     * @return Key that uniquely represents the sensor that produced the data point.
     */
    private String getKey(ContentValues dataPoint) {
        String name = dataPoint.getAsString(DataPoint.SENSOR_NAME);
        String descr = dataPoint.getAsString(DataPoint.SENSOR_DESCRIPTION);
        return name.equals(descr) ? name : name + " (" + descr + ")";
    }

    public String getType(Uri uri) {
        // Log.v(TAG, "Get content type...");
        int uriType = matchUri(uri);
        if (uriType == VOLATILE_VALUES_URI || uriType == PERSISTED_VALUES_URI
                || uriType == REMOTE_VALUES_URI) {
            return DataPoint.CONTENT_TYPE;
        } else {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    public Uri insert(Uri uri, ContentValues values) {

        // check URI
        switch (matchUri(uri)) {
        case VOLATILE_VALUES_URI:
            break;
        case PERSISTED_VALUES_URI:
            throw new IllegalArgumentException(
                    "Cannot insert directly into persistent data point database");
        case REMOTE_VALUES_URI:
            throw new IllegalArgumentException(
                    "Cannot insert into CommonSense through this ContentProvider");
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // add a unique ID
        values.put(DataPoint._ID, count);
        count++;

        // get currently stored values from the storage map
        String key = getKey(values);
        ContentValues[] storedValues = storage.get(key);
        Integer index = pointers.get(key);
        if (null == storedValues) {
            storedValues = new ContentValues[MAX_VOLATILE_VALUES];
            index = 0;
        }

        // check for buffer overflows
        if (index >= MAX_VOLATILE_VALUES) {
            // Log.d(TAG, "Buffer overflow! More than " + MAX_VOLATILE_VALUES + " points for '" +
            // key + "'. Send to persistent storage...");

            // find out how many values have to be put in the persistant storage
            int persistFrom = -1;
            for (int i = 0; i < storedValues.length; i++) {
                if (storedValues[i].getAsInteger(DataPoint.TRANSMIT_STATE) == 0) {
                    // found the first data point that was not transmitted yet
                    persistFrom = i;
                    break;
                }
            }

            // persist the data that was not sent yet
            if (-1 != persistFrom) {
                ContentValues[] unsent = new ContentValues[MAX_VOLATILE_VALUES - persistFrom];
                System.arraycopy(storedValues, persistFrom, unsent, 0, unsent.length);
                persist(unsent);
            }

            // reset the array and index
            storedValues = new ContentValues[MAX_VOLATILE_VALUES];
            index = 0;
        }

        // add the new data point
        // Log.v(TAG, "Inserting '" + key + "' value in local storage...");
        storedValues[index] = values;
        index++;
        storage.put(key, storedValues);
        pointers.put(key, index);

        // notify any listeners (does this work properly?)
        Uri contentUri = Uri.parse("content://"
                + context.getString(R.string.local_storage_authority) + DataPoint.CONTENT_URI_PATH);
        Uri rowUri = ContentUris.withAppendedId(contentUri, count - 1);
        context.getContentResolver().notifyChange(rowUri, null);

        return rowUri;
    }

    private int matchUri(Uri uri) {
        if (DataPoint.CONTENT_URI_PATH.equals(uri.getPath())) {
            return VOLATILE_VALUES_URI;
        } else if (DataPoint.CONTENT_PERSISTED_URI_PATH.equals(uri.getPath())) {
            return PERSISTED_VALUES_URI;
        } else if (DataPoint.CONTENT_REMOTE_URI_PATH.equals(uri.getPath())) {
            return REMOTE_VALUES_URI;
        } else {
            return -1;
        }
    }

    private void persist(ContentValues[] storedValues) {
        // Log.v(TAG, "Persist " + storedValues.length + " data points");
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // do not store data points that are more than 24 hours old
            String where = DataPoint.TIMESTAMP + "<"
                    + (SNTP.getInstance().getTime() - 1000l * 60 * 60 * 24);
            int deleted = db.delete(TABLE_PERSISTENT, where, null);
            if (deleted > 0) {
                // Log.v(TAG, "Deleted " + deleted + " old data points from persistent storage");
            }

            // insert new points to persistent storage
            for (ContentValues dataPoint : storedValues) {
                // strip the _ID column to dodge SQL exceptions
                dataPoint.remove(DataPoint._ID);

                db.insert(TABLE_PERSISTENT, DataPoint.SENSOR_NAME, dataPoint);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception persisting recent sensor values to database", e);
        }
    }

    public Cursor query(Uri uri, String[] projection, String where, String[] selectionArgs,
            String sortOrder) {
        // Log.v(TAG, "Query local storage...");

        // default projection
        if (projection == null) {
            projection = new String[] { BaseColumns._ID, DataPoint.SENSOR_NAME,
                    DataPoint.DISPLAY_NAME, DataPoint.SENSOR_DESCRIPTION, DataPoint.DATA_TYPE,
                    DataPoint.VALUE, DataPoint.TIMESTAMP, DataPoint.DEVICE_UUID,
                    DataPoint.TRANSMIT_STATE };
        }

        // query based on URI
        switch (matchUri(uri)) {
        case VOLATILE_VALUES_URI:
            try {
                return queryVolatile(uri, projection, where, selectionArgs, sortOrder);
            } catch (Exception e) {
                Log.e(TAG, "Failed to query the recent data points", e);
                return null;
            }
        case PERSISTED_VALUES_URI:
            try {
                return queryPersistent(uri, projection, where, selectionArgs, sortOrder);
            } catch (Exception e) {
                Log.e(TAG, "Failed to query the persisted data points", e);
                return null;
            }
        case REMOTE_VALUES_URI:
            // try {
            Log.w(TAG, "Querying remote data points is not supported!");
            // return queryRemote(uri, projection, where, selectionArgs, sortOrder);
            // } catch (Exception e) {
            // Log.e(TAG, "Failed to query the CommonSense data points", e);
            return null;
            // }
        default:
            Log.e(TAG, "Unknown URI: " + uri);
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    private Cursor queryPersistent(Uri uri, String[] projection, String where,
            String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        SharedPreferences pref = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        String limitStr = "" + QUERY_RESULTS_LIMIT;

        if (pref.getBoolean(SensePrefs.Main.Motion.EPIMODE, false))
            limitStr = "" + QUERY_RESULTS_LIMIT_EPI_MODE;

        Cursor persistentResult = db.query(TABLE_PERSISTENT, projection, where, selectionArgs,
                null, null, sortOrder, limitStr);

        return persistentResult;
    }

    /*
     * private Cursor queryRemote(Uri uri, String[] projection, String where, String[]
     * selectionArgs, String sortOrder) throws JSONException, URISyntaxException { // Log.v(TAG,
     * "Query remote data points");
     * 
     * // try to parse the selection criteria List<String> sensorNames =
     * ParserUtils.getSelectedSensors(storage.keySet(), where, selectionArgs); long[]
     * timeRangeSelect = ParserUtils.getSelectedTimeRange(where, selectionArgs);
     * 
     * if (sensorNames.size() != 1) { throw new
     * IllegalArgumentException("Can only query CommonSense for a single sensor"); }
     * 
     * JSONArray sensors = SenseApi.getAllSensors(context);
     * 
     * if (sensors == null) { Log.w(TAG, "Cannot access sensors from CommonSense"); return null; }
     * 
     * // check if the requested sensor is in the list String id = null; for (int i = 0; i <
     * sensors.length(); i++) { JSONObject sensor = sensors.getJSONObject(i); if
     * (sensor.getString("name").equalsIgnoreCase(sensorNames.get(0))) { // found the right sensor
     * if (null != id) { Log.w(TAG, "Multiple sensors with the same name"); } id =
     * sensor.getString("id"); } }
     * 
     * // get the data for the sensor URI remoteUri = new URI(SenseUrls.SENSOR_DATA.replace("<id>",
     * id) + "?start_date=" + timeRangeSelect[0] / 1000d + "&end_date=" + timeRangeSelect[1] /
     * 1000d); String cookie = context.getSharedPreferences(SensePrefs.AUTH_PREFS,
     * Context.MODE_PRIVATE) .getString(Auth.LOGIN_COOKIE, null); JSONObject response =
     * SenseApi.getJsonObject(context, remoteUri, cookie); JSONArray data =
     * response.getJSONArray("data");
     * 
     * // fill the result Cursor with sensor data MatrixCursor result = new MatrixCursor(projection,
     * data.length()); for (int i = 0; i < data.length(); i++) { Object[] row = new
     * Object[projection.length]; JSONObject jsonDataPoint = data.getJSONObject(i); for (int j = 0;
     * j < projection.length; j++) { if (projection[j].equals(DataPoint.VALUE)) { row[j] =
     * jsonDataPoint.getString("value"); } else if (projection[j].equals(DataPoint.TIMESTAMP)) {
     * double rawDate = jsonDataPoint.getDouble("date"); row[j] = Math.round(rawDate * 1000d); }
     * else if (projection[j].equals(DataPoint.SENSOR_NAME)) { row[j] = sensorNames.get(0); } }
     * result.addRow(row); }
     * 
     * return result; }
     */

    private Cursor queryVolatile(Uri uri, String[] projection, String where,
            String[] selectionArgs, String sortOrder) {

        // do selection
        ContentValues[] selection = select(where, selectionArgs);

        // create new cursor with the query result
        MatrixCursor result = new MatrixCursor(projection);
        Object[] row = null;
        for (ContentValues dataPoint : selection) {
            row = new Object[projection.length];
            for (int i = 0; i < projection.length; i++) {
                row[i] = dataPoint.get(projection[i]);
            }
            result.addRow(row);
        }

        // Log.d(TAG, "query result: " + result.getCount() + " data points.");

        return result;
    }

    private synchronized ContentValues[] select(String where, String[] selectionArgs) {

        // try to parse the selection criteria
        List<String> sensorNames = ParserUtils.getSelectedSensors(storage.keySet(), where,
                selectionArgs);
        long[] timeRangeSelect = ParserUtils.getSelectedTimeRange(where, selectionArgs);
        int transmitStateSelect = ParserUtils.getSelectedTransmitState(where, selectionArgs);

        ContentValues[] selection = new ContentValues[50 * MAX_VOLATILE_VALUES], dataPoints;
        ContentValues dataPoint;
        long timestamp = 0;
        int count = 0, max = 0, transmitState = 0;
        for (String name : sensorNames) {
            dataPoints = storage.get(name);
            if (null != dataPoints) {
                max = pointers.get(name);
                for (int i = 0; i < max; i++) {
                    dataPoint = dataPoints[i];
                    timestamp = dataPoint.getAsLong(DataPoint.TIMESTAMP);
                    if (timestamp >= timeRangeSelect[0] && timestamp <= timeRangeSelect[1]) {
                        transmitState = dataPoint.getAsInteger(DataPoint.TRANSMIT_STATE);
                        if (transmitStateSelect == -1 || transmitState == transmitStateSelect) {
                            selection[count] = dataPoint;
                            count++;
                        } else {
                            // Log.v(TAG, "Transmit state doesn't match: " + transmitState);
                        }
                    } else {
                        // Log.d(TAG, "Outside time range: " + timestamp);
                    }
                }
            } else {
                // Log.d(TAG, "Could not find values for the selected sensor: '" + name + "'");
            }
        }

        // copy selection to new array with proper length
        ContentValues[] result = new ContentValues[count];
        System.arraycopy(selection, 0, result, 0, count);

        return result;
    }

    public int update(Uri uri, ContentValues newValues, String where, String[] selectionArgs) {
        // Log.v(TAG, "Update local storage...");

        // check URI
        switch (matchUri(uri)) {
        case VOLATILE_VALUES_URI:
            break;
        case PERSISTED_VALUES_URI:
            throw new IllegalArgumentException("Cannot update the persisted data points");
        case REMOTE_VALUES_URI:
            throw new IllegalArgumentException("Cannot update data points in CommonSense");
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // persist parameter is used to initiate persisting the volatile data
        boolean persist = "true".equals(uri.getQueryParameter("persist"));

        // select the correct data points to update
        ContentValues[] selection = select(where, selectionArgs);

        // do the update
        int result = 0;
        if (!persist) {
            for (ContentValues dataPoint : selection) {
                dataPoint.putAll(newValues);
                result++;
            }
        } else {
            persist(selection);
            storage.clear();
        }

        // notify content observers
        context.getContentResolver().notifyChange(uri, null);

        return result;
    }
}
