/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.provider;

import java.util.HashMap;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensorData.BufferedData;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.storage.LocalStorage;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * ContentProvider that encapsulates recent sensor data. The data is persistently stored on the
 * devices flash memory, so this implementation is not very energy efficient. This implementation is
 * only kept as reference.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 * 
 * @see LocalStorage
 * @see DataPoint
 */
public class LocalStorageSql extends ContentProvider {

    /**
     * Inner class that helps managing the SQLite3 database.
     */
    private static class DbHelper extends SQLiteOpenHelper {

        DbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            final StringBuilder sb = new StringBuilder("CREATE TABLE " + VALUES_TABLE_NAME + "(");
            sb.append(BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT");
            sb.append(", " + DataPoint.SENSOR_NAME + " STRING");
            sb.append(", " + DataPoint.SENSOR_DESCRIPTION + " STRING");
            sb.append(", " + DataPoint.DATA_TYPE + " STRING");
            sb.append(", " + DataPoint.VALUE + " STRING");
            sb.append(", " + DataPoint.TIMESTAMP + " INTEGER");
            sb.append(");");
            db.execSQL(sb.toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVers, int newVers) {
            Log.w(TAG, "Upgrading database from version " + oldVers + " to " + newVers
                    + ", which will destroy all old data");

            db.execSQL("DROP TABLE IF EXISTS " + VALUES_TABLE_NAME);
            onCreate(db);
        }
    }

    private static final String TAG = "Sense LocalStorage";

    public static final String AUTHORITY = "nl.sense_os.service.provider.LocalStorage";
    private static final String DATABASE_NAME = "local_storage.sqlite3";
    private static final String VALUES_TABLE_NAME = "recent_values";
    private static final int DATABASE_VERSION = 1;
    private static final int VALUES_URI = 1;
    private static final long RETENTION_TIME = 1000 * 60 * 15; // 15 minutes

    private DbHelper dbHelper;

    private static HashMap<String, String> projectionMap;
    private static UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, VALUES_TABLE_NAME, VALUES_URI);

        projectionMap = new HashMap<String, String>();
        projectionMap.put(DataPoint.SENSOR_NAME, DataPoint.SENSOR_NAME);
        projectionMap.put(DataPoint.SENSOR_DESCRIPTION, DataPoint.SENSOR_DESCRIPTION);
        projectionMap.put(DataPoint.DATA_TYPE, DataPoint.DATA_TYPE);
        projectionMap.put(DataPoint.TIMESTAMP, DataPoint.TIMESTAMP);
        projectionMap.put(DataPoint.VALUE, DataPoint.VALUE);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Log.v(TAG, "Delete row(s) in local storage...");

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)) {
        case VALUES_URI:
            count = db.delete(VALUES_TABLE_NAME, selection, selectionArgs);
            // Log.v(TAG, count + " rows deleted");
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        db.close();

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        Log.v(TAG, "Get content type...");

        switch (uriMatcher.match(uri)) {
        case VALUES_URI:
            return BufferedData.CONTENT_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // Log.v(TAG, "Insert row in local storage...");

        if (uriMatcher.match(uri) != VALUES_URI) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // remove older data points by this sensor
        String removeWhere = DataPoint.SENSOR_NAME + "=?" + " AND " + DataPoint.TIMESTAMP + "<?";
        String[] removeArgs = new String[] { values.getAsString(DataPoint.SENSOR_NAME),
                "" + (SNTP.getInstance().getTime() - RETENTION_TIME) };
        delete(uri, removeWhere, removeArgs);

        long rowId = db.insert(VALUES_TABLE_NAME, BufferedData.ACTIVE, values);

        db.close();

        if (rowId > 0) {
            Uri contentUri = Uri.parse("content://"
                    + getContext().getString(R.string.local_storage_authority)
                    + DataPoint.CONTENT_URI_PATH);
            Uri rowUri = ContentUris.withAppendedId(contentUri, rowId);
            getContext().getContentResolver().notifyChange(rowUri, null);
            return rowUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        Log.v(TAG, "Create local storage...");
        dbHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Log.v(TAG, "Query local storage...");

        if (uriMatcher.match(uri) != VALUES_URI) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(VALUES_TABLE_NAME);
        qb.setProjectionMap(projectionMap);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        db.close();

        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Log.v(TAG, "Update row(s) in local storage...");

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)) {
        case VALUES_URI:
            count = db.update(VALUES_TABLE_NAME, values, selection, selectionArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        db.close();

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
