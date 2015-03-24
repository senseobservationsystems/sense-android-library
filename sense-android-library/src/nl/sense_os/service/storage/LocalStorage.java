/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.storage;

import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.debug.OutputUtils;
import nl.sense_os.service.provider.SNTP;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Storage for recent sensor data. The data is initially stored in the device's RAM memory. In case
 * the memory becomes too full, the data is offloaded into a persistent database in the flash
 * memory. This process is hidden to the end user, so you do not have to worry about which data is
 * where.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 * 
 * @see ParserUtils
 * @see DataPoint
 */
public class LocalStorage {

    /**
     * Minimum time to retain data points. If data is not sent to CommonSense, it will be retained
     * longer.
     */
  private static final int DEFAULT_RETENTION_HOURS = 24;

    /**
     * Default projection for rows of data points
     */
    private static final String[] DEFAULT_PROJECTION = new String[] { BaseColumns._ID,
            DataPoint.SENSOR_NAME, DataPoint.DISPLAY_NAME, DataPoint.SENSOR_DESCRIPTION,
            DataPoint.DATA_TYPE, DataPoint.VALUE, DataPoint.TIMESTAMP, DataPoint.DEVICE_UUID,
            DataPoint.TRANSMIT_STATE };

    private static final int LOCAL_VALUES_URI = 1;
    private static final int REMOTE_VALUES_URI = 2;

    private static final String TAG = "LocalStorage";

    private static final int DEFAULT_LIMIT = 100;

    private static LocalStorage instance;

    /**
     * @param context
     *            Context for lazy creating the LocalStorage.
     * @return Singleton instance of the LocalStorage
     */
    public static LocalStorage getInstance(Context context) {
        // Log.v(TAG, "Get local storage instance");
        if (null == instance) {
            instance = new LocalStorage(context.getApplicationContext());
        }
        return instance;
    }

    private final RemoteStorage commonSense;
    private final SQLiteStorage inMemory;
    private final SQLiteStorage persisted;

    private Context context;

    private LocalStorage(Context context) {
        Log.i(TAG, "Construct new local storage instance");
        this.context = context;
        persisted = new SQLiteStorage(context, true);
        inMemory = new SQLiteStorage(context, false);
        commonSense = new RemoteStorage(context);
    }

    public int delete(Uri uri, String where, String[] selectionArgs) {
        switch (matchUri(uri)) {
        case LOCAL_VALUES_URI:
            int nrDeleted = 0;
            nrDeleted += inMemory.delete(where, selectionArgs);
            nrDeleted += persisted.delete(where, selectionArgs);
            return nrDeleted;
        case REMOTE_VALUES_URI:
            throw new IllegalArgumentException("Cannot delete values from CommonSense!");
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    //TODO: make this method private before merge
    /**
     * Removes old data from the persistent storage.
     * 
     * @return The number of data points deleted
     */
    @SuppressLint( "NewApi" )
    public int deleteOldData() {
        Log.i(TAG, "Delete old data points from persistent storage");

        SharedPreferences prefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        // set max retention time
        int retentionHours = prefs.getInt( Main.Advanced.RETENTION_HOURS, DEFAULT_RETENTION_HOURS );
        long retentionLimit = SNTP.getInstance().getTime() - getMilliSencondsOfHours( retentionHours );
        // check preferences to see if the data needs to be sent to CommonSense
        boolean useCommonSense = prefs.getBoolean(Main.Advanced.USE_COMMONSENSE, true );
        boolean preserveLastDatapoints = prefs.getBoolean( Main.Advanced.PRESERVE_LAST_DATAPOINTS, false );
        
        
        String where = "";
        
        //TODO: This is probably not a optimized solution. This runs delete query per cumulative sensors.
        //      There is a query that should do the job in one go, but somehow subqueries do not return what it should.
        //      The documentation is available at https://docs.google.com/a/sense-os.nl/document/d/1WEobs3ZlX5qnlct0n3N6jnwURPjhcMOIQMvclJCVwdY/edit?usp=sharing
        if(preserveLastDatapoints){
             where = deleteOldDataForCumulativeSensors( prefs, retentionLimit, where );
        }
        
        if (useCommonSense) {
            // delete data older than maximum retention time if it had been transmitted
            where += DataPoint.TIMESTAMP + "<" + retentionLimit + " AND " + DataPoint.TRANSMIT_STATE
                    + "==1";
        } else {
            // not using CommonSense: delete all data older than maximum retention time
            where += DataPoint.TIMESTAMP + "<" + retentionLimit;
        }
        
        int deleted = persisted.delete(where, null);

        return deleted;
    }

    public String deleteOldDataForCumulativeSensors( SharedPreferences prefs,
        long retentionLimit, String where ) {
      List<String> list = getListOfPreservedSensors( prefs );

       Iterator<String> iter = list.iterator();
       
       if(list!=null && list.size()>0){
         where = DataPoint.SENSOR_NAME + " NOT IN (";
         
         while(iter.hasNext()){
             String sensor = iter.next();
             if(sensor != null){
               String wherePerSensor = DataPoint.SENSOR_NAME +"=='"+ sensor+ "' AND " + DataPoint.TRANSMIT_STATE +"==1 AND " + DataPoint.TIMESTAMP +"<(SELECT MAX("+DataPoint.TIMESTAMP +") FROM "
                               + DbHelper.TABLE + " WHERE " + DataPoint.TIMESTAMP + "<" + retentionLimit + " AND " + DataPoint.SENSOR_NAME + "=='" + sensor + "' GROUP BY " + DataPoint.SENSOR_NAME +")";
               persisted.delete(wherePerSensor, null);
               
               //exculde those sensors from normal delete query
               where += "'"+sensor+"'";
               where += (iter.hasNext())?",":"";
               //TODO: remove this
               Log.d("DBTest", sensor);
             }
         }
         where += ") AND ";
       }
      return where;
    }

    public List<String> getListOfPreservedSensors( SharedPreferences prefs ) {
      int size = prefs.getInt( SensePrefs.Main.Advanced.PRESERVED_SENSORS_SIZE, 0 );
       List<String> list= new ArrayList<String>();
       for(int i = 0; i < size ; i++){
         list.add(prefs.getString( SensePrefs.Main.Advanced.PRESERVED_SENSOR_PREFIX + i, null ));
       }
      return list;
    }
    


    public String getType(Uri uri) {
        int uriType = matchUri(uri);
        if (uriType == LOCAL_VALUES_URI || uriType == REMOTE_VALUES_URI) {
            return DataPoint.CONTENT_TYPE;
        } else {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    public Uri insert(Uri uri, ContentValues values) {

        // check the URI
        switch (matchUri(uri)) {
        case LOCAL_VALUES_URI:
            // implementation below
            break;
        case REMOTE_VALUES_URI:
            throw new IllegalArgumentException(
                    "Cannot insert into CommonSense through this ContentProvider");
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // insert in the in-memory database
        long rowId = 0;
        try {
            rowId = inMemory.insert(values);
        } catch (BufferOverflowException e) {
            // in-memory storage is full!
            //TODO: remove this before merge
            OutputUtils.appendLog( "BufferOverflowException!" );
          
            deleteOldData();
            persistRecentData();

            // try again
            rowId = inMemory.insert(values);
        }

        // notify any listeners (does this work properly?)
        Uri contentUri = Uri.parse("content://"
                + context.getString(R.string.local_storage_authority) + DataPoint.CONTENT_URI_PATH);
        Uri rowUri = ContentUris.withAppendedId(contentUri, rowId);
        context.getContentResolver().notifyChange(rowUri, null);

        return rowUri;
    }

    private int matchUri(Uri uri) {
        if (DataPoint.CONTENT_URI_PATH.equals(uri.getPath())) {
            return LOCAL_VALUES_URI;
        } else if (DataPoint.CONTENT_REMOTE_URI_PATH.equals(uri.getPath())) {
            return REMOTE_VALUES_URI;
        } else {
            return -1;
        }
    }

    public int persistRecentData() {
        Log.i(TAG, "Persist recent data points from in-memory storage");
        //TODO : remove this
        OutputUtils.appendLog( "Persist recent data points from in-memory storage");

        Cursor recentPoints = null;
        int nrRecentPoints = 0;
        try {
            SharedPreferences prefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS, Context.MODE_PRIVATE);
            int retentionHours = prefs.getInt( Main.Advanced.RETENTION_HOURS, DEFAULT_RETENTION_HOURS );
            long retentionLimit = SNTP.getInstance().getTime() - getMilliSencondsOfHours( retentionHours );

            // get unsent or very recent data from the memory
            String selectUnsent = DataPoint.TRANSMIT_STATE + "!=1" + " OR " + DataPoint.TIMESTAMP
                    + ">" + retentionLimit;
            recentPoints = inMemory.query(DEFAULT_PROJECTION, selectUnsent, null, null);
            nrRecentPoints = recentPoints.getCount();

            // bulk insert the new data
            persisted.bulkInsert(recentPoints);

            // remove all the in-memory data
            inMemory.delete(null, null);

        } finally {
            if (null != recentPoints) {
                recentPoints.close();
                recentPoints = null;
            }
        }
        return nrRecentPoints;
    }

    public Cursor query(Uri uri, String[] projection, String where, String[] selectionArgs,
            String sortOrder) {
        return query(uri, projection, where, selectionArgs, DEFAULT_LIMIT, sortOrder);
    }

    public Cursor query(Uri uri, String[] projection, String where, String[] selectionArgs,
            Integer limit, String sortOrder) {
        // Log.v(TAG, "Query data points in local storage");

        // check URI
        switch (matchUri(uri)) {
        case LOCAL_VALUES_URI:
            // implementation below
            break;
        case REMOTE_VALUES_URI:
            try {
                return commonSense.query(uri, projection, where, selectionArgs, limit, sortOrder);
            } catch (Exception e) {
                Log.e(TAG, "Failed to query the CommonSense data points", e);
                return null;
            }
        default:
            Log.e(TAG, "Unknown URI: " + uri);
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // use default projection if needed
        if (projection == null) {
            projection = DEFAULT_PROJECTION;
        }

        String limitStr = (limit != null)? "" + limit : null;
        // query both databases
        Cursor inMemoryCursor = inMemory.query(projection, where, selectionArgs, sortOrder, limitStr);
        Cursor persistedCursor = persisted.query(projection, where, selectionArgs, sortOrder, limitStr);

        if (inMemoryCursor.getCount() > 0) {
            if (persistedCursor.getCount() > 0) {
                // merge cursors
                if (sortOrder != null && sortOrder.toLowerCase(Locale.ENGLISH).contains("desc")) {
                    // assume that data from inMemoryCursor is newer than from persistedCursor
                    return new MergeCursor(new Cursor[] { inMemoryCursor, persistedCursor });
                } else {
                    // assume that data from persistedCursor is newer than from inMemoryCursor
                    return new MergeCursor(new Cursor[] { persistedCursor, inMemoryCursor });
                }
            } else {
                persistedCursor.close();
                return inMemoryCursor;
            }

        } else {
            inMemoryCursor.close();
            return persistedCursor;
        }
    }

    public int update(Uri uri, ContentValues newValues, String where, String[] selectionArgs) {

        // check URI
        switch (matchUri(uri)) {
        case LOCAL_VALUES_URI:
            // implementation below
            break;
        case REMOTE_VALUES_URI:
            throw new IllegalArgumentException("Cannot update data points in CommonSense");
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // persist parameter is used to initiate persisting the volatile data
        boolean persist = "true".equals(uri.getQueryParameter("persist"));

        int result = 0;
        if (!persist) {
            // Log.v(TAG, "Update data points in local storage");
            int updated = 0;
            updated += inMemory.update(newValues, where, selectionArgs);
            updated += persisted.update(newValues, where, selectionArgs);
            return updated;
        } else {
            deleteOldData();
            persistRecentData();
        }

        // notify content observers
        context.getContentResolver().notifyChange(uri, null);

        return result;
    }
    
    public static long getMilliSencondsOfHours(int hours){
        return 1000l* 60 * 60 * hours;
    }

}
