/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.storage.LocalStorage;

/**
 * ContentProvider that encapsulates recent sensor data. The data is stored in the devices RAM
 * memory, so this implementation is more energy efficient than storing everything in flash. This
 * does mean that parsing the selection queries is quite a challenge. Only a very limited set of
 * queries will work:
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
 * @see LocalStorage
 * @see DataPoint
 */
public class SensorDataProvider extends ContentProvider {

    private static final String TAG = "SensorDataProvider";

    @Override
    public int delete(Uri uri, String where, String[] selectionArgs) {
        return LocalStorage.getInstance(getContext()).delete(uri, where, selectionArgs);
    }

    @Override
    public String getType(Uri uri) {
        return LocalStorage.getInstance(getContext()).getType(uri);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return LocalStorage.getInstance(getContext()).insert(uri, values);
    }

    @Override
    public boolean onCreate() {
        Log.v(TAG, "Create sensor data provider...");
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String where, String[] selectionArgs,
            String sortOrder) {
        return LocalStorage.getInstance(getContext()).query(uri, projection, where, selectionArgs,
                sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues newValues, String where, String[] selectionArgs) {
        return LocalStorage.getInstance(getContext()).update(uri, newValues, where, selectionArgs);
    }
}
