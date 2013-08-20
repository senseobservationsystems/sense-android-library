package nl.sense_os.service.storage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Auth;
import nl.sense_os.service.constants.SenseUrls;
import nl.sense_os.service.constants.SensorData.DataPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

/**
 * Class that manages the (read-only) store for sensor data points in the remote CommonSense
 * storage. Helper class for {@link LocalStorage}.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
class RemoteStorage {

    private static final String TAG = "RemoteStorage";
    private Context context;

    public RemoteStorage(Context context) {
        this.context = context;
    }

    public Cursor query(Uri uri, String[] projection, String where, String[] selectionArgs,
            int limit, String sortOrder) throws JSONException, URISyntaxException, IOException {
        // Log.v(TAG, "Query data points in CommonSense");

        // try to parse the selection criteria
        List<String> sensorNames = ParserUtils.getSelectedSensors(new HashSet<String>(), where,
                selectionArgs);
        long[] timeRangeSelect = ParserUtils.getSelectedTimeRange(where, selectionArgs);
        String deviceUuid = ParserUtils.getSelectedDeviceUuid(where, selectionArgs);

        if (sensorNames.size() != 1) {
            throw new IllegalArgumentException("Incorrect number of sensors in query: "
                    + sensorNames.size());
        }

        // check if the requested sensor is in the list
        String id = SenseApi.getSensorId(context, sensorNames.get(0), null, null, deviceUuid);

        if (null == id) {
            throw new IllegalArgumentException("Cannot find sensor ID");
        }

        // convert sort order to commonsense format
        if (null != sortOrder && sortOrder.length() > 0) {
            if (sortOrder.toLowerCase(Locale.ENGLISH).contains("desc")) {
                sortOrder = "desc";
            } else {
                sortOrder = "asc";
            }
        } else {
            sortOrder = "desc";
        }

        // get the data for the sensor
        String url = SenseUrls.SENSOR_DATA.replace("%1", id) + "?start_date="
                + timeRangeSelect[0] / 1000d + "&end_date=" + timeRangeSelect[1] / 1000d;
        url += "&per_page=" + limit;
        url += sortOrder != null ? "&sort=" + sortOrder : "";

        String cookie = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE)
                .getString(Auth.LOGIN_COOKIE, null);
        Map<String, String> response = SenseApi.request(context, url, null, cookie);

        // parse response
        JSONArray data;
        if (response.get(SenseApi.RESPONSE_CODE).equals("200")) {
            String content = response.get(SenseApi.RESPONSE_CONTENT);
            JSONObject json = new JSONObject(content);
            data = json.getJSONArray("data");
        } else {
            Log.w(TAG, "Error retrieving sensor data: " + response.get(SenseApi.RESPONSE_CODE));
            return null;
        }

        // fill the result Cursor with sensor data
        MatrixCursor result = new MatrixCursor(projection, data.length());
        for (int i = 0; i < data.length(); i++) {
            Object[] row = new Object[projection.length];
            JSONObject jsonDataPoint = data.getJSONObject(i);
            for (int j = 0; j < projection.length; j++) {
                if (projection[j].equals(DataPoint.VALUE)) {
                    row[j] = jsonDataPoint.getString("value");
                } else if (projection[j].equals(DataPoint.TIMESTAMP)) {
                    double rawDate = jsonDataPoint.getDouble("date");
                    row[j] = Math.round(rawDate * 1000d);
                } else if (projection[j].equals(DataPoint.SENSOR_NAME)) {
                    row[j] = sensorNames.get(0);
                }
            }
            result.addRow(row);
        }

        return result;
    }
}
