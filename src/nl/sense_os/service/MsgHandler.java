/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import nl.sense_os.app.SenseSettings;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MsgHandler extends Service {

    /**
     * Inner class that handles the creation of the SQLite3 database with the desired tables and
     * columns.
     * 
     * To view the Sqlite3 database in a terminal: $ adb shell # sqlite3
     * /data/data/nl.sense_os.dji/databases/data.sqlite3 sqlite> .headers ON sqlite> select * from
     * testTbl;
     */
    private static class DbHelper extends SQLiteOpenHelper {

        protected static final String COL_ACTIVE = "active";
        protected static final String COL_JSON = "json";
        protected static final String COL_ROWID = "_id";
        protected static final String DATABASE_NAME = "tx_buffer.sqlite3";
        protected static final int DATABASE_VERSION = 3;
        protected static final String TABLE_NAME = "sensor_data";

        DbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            final StringBuilder sb = new StringBuilder("CREATE TABLE " + TABLE_NAME + "(");
            sb.append(COL_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT");
            sb.append(", " + COL_JSON + " STRING");
            sb.append(", " + COL_ACTIVE + " INTEGER");
            sb.append(");");
            db.execSQL(sb.toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVers, int newVers) {
            Log.w(TAG, "Upgrading database from version " + oldVers + " to " + newVers
                    + ", which will destroy all old data");

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    private class SendMessageThread implements Runnable {
        String cookie;
        String url;

        public SendMessageThread(String _url, String Cookie) {
            url = _url;
            cookie = Cookie;
        }

        public void run() {
            HttpClient client = new DefaultHttpClient();
            client.getConnectionManager().closeIdleConnections(2, TimeUnit.SECONDS);
            try {
                if (nrOfSendMessageThreads < MAX_NR_OF_SEND_MSG_THREADS) {
                    
                    ++nrOfSendMessageThreads;
                    
                    // prepare POST
                    URI uri = new URI(url);
                    HttpPost post = new HttpPost(uri);
                    post.setHeader("Cookie", cookie);                     
                    
                    // perform POST
                    ResponseHandler<String> responseHandler = new BasicResponseHandler();
                    String response = client.execute(post, responseHandler);
                    handleResponse(response);
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException in SendMessageThread: " + e.getMessage());
            } catch (URISyntaxException e) {
                Log.e(TAG, "URISyntaxException in SendMessageThread: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, "JSONException in SendMessageThread: " + e.getMessage());
            } finally {
                --nrOfSendMessageThreads;
                client.getConnectionManager().shutdown();
            }
        }
    }
    
    public static final String ACTION_NEW_MSG = "nl.sense_os.app.MsgHandler.NEW_MSG";
    public static final String ACTION_SEND_DATA = "nl.sense_os.app.MsgHandler.SEND_DATA";
    public static final String KEY_DATA_TYPE = "data_type";
    public static final String KEY_SENSOR_DEVICE = "sensor_device";
    public static final String KEY_SENSOR_NAME = "sensor_name";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_VALUE = "value";
    private static final int MAX_BUFFER = 1024;
    private static final int MAX_NR_OF_SEND_MSG_THREADS = 50;
    private static final int MAX_POST_DATA = 100;
    private static final String TAG = "Sense MsgHandler";
    private JSONObject[] buffer;
    private int bufferCount;
    private SQLiteDatabase db;
    private DbHelper dbHelper;
    private boolean isDbOpen;
    private int nrOfSendMessageThreads = 0;

    /**
     * Buffers data in the memory
     * 
     * @param json
     *            the data to buffer
     */
    private void bufferData(JSONObject json) {

        // check if there is room in the buffer
        if (this.bufferCount >= MAX_BUFFER) {
            // empty buffer into database
            Log.d(TAG, "Buffer overflow! Emptying buffer to database");
            emptyBufferToDb();
        }

        // put data in buffer
        this.buffer[this.bufferCount] = json;
        this.bufferCount++;
    }

    private void closeDb() {
        if (true == this.isDbOpen) {
            this.dbHelper.close();
            this.isDbOpen = false;
        }
    }

    /**
     * Puts data from the buffer in the flash database for long-term storage
     */
    private void emptyBufferToDb() {
        Log.d(TAG, "Emptying buffer to database...");

        openDb();
        try {
            for (int i = 0; i < this.bufferCount; i++) {
                ContentValues values = new ContentValues();
                values.put(DbHelper.COL_JSON, this.buffer[i].toString());
                values.put(DbHelper.COL_ACTIVE, false);
                this.db.insert(DbHelper.TABLE_NAME, null, values);
            }

            // reset buffer
            this.bufferCount = 0;
            this.buffer = new JSONObject[MAX_BUFFER];
        } finally {
            closeDb();
        }
    }
    
    private void handleResponse(String responseString) throws JSONException {
        JSONObject response = new JSONObject(responseString);

        if (response.getString("status").equals("ok")) {
            Log.d(TAG,"Sent sensor data OK! Response message: " + response.getString("msg"));
        } else {
            int error = response.getInt("faultcode");

            switch (error) {
            // TODO error handling
            case 1:
                Log.e(TAG, "Error storing data in CommonSense, re-login");
                break;
            case 2:
                Log.e(TAG, "Error storing data in CommonSense, sensor data incomplete!");
                break;
            case 3:
                Log.e(TAG,
                        "SQL error storing data in CommonSense: "
                                + response.getString("msg"));
                break;
            default:
                Log.e(TAG, "Error sending sensor data: " + error);
            }
            return;
        }
    }

    /**
     * Handles an incoming Intent that started the service by checking if it wants to store a new
     * message or if it wants to send data to CommonSense.
     */
    private void handleIntent(Intent intent, int flags, int startId) {

        final String action = intent.getAction();

        if (action.equals(ACTION_NEW_MSG)) {
            handleNewMsgIntent(intent);
        } else if (action.equals(ACTION_SEND_DATA)) {
            handleSendIntent(intent);
        } else {
            Log.e(TAG, "Unexpected intent action: " + action);
        }
    }

    private void handleNewMsgIntent(Intent intent) {
        // Log.d(TAG, "handleNewMsgIntent");

        String name = intent.getStringExtra(KEY_SENSOR_NAME);
        String type = intent.getStringExtra(KEY_DATA_TYPE);
        long time = intent.getLongExtra(KEY_TIMESTAMP, System.currentTimeMillis());
        String device = intent.getStringExtra(KEY_SENSOR_DEVICE);
        JSONObject json = new JSONObject();
        try {
            json.put("name", name);
            json.put("time", time / 1000D);
            json.put("type", type);
            if (null != device) {
                json.put("device", device);
            }
            if (type.equals(SenseSettings.SENSOR_DATA_TYPE_BOOL)) {
                json.put("val", intent.getBooleanExtra(KEY_VALUE, false));
            } else if (type.equals(SenseSettings.SENSOR_DATA_TYPE_FLOAT)) {
                json.put("val", intent.getDoubleExtra(KEY_VALUE, Double.MIN_VALUE));
            } else if (type.equals(SenseSettings.SENSOR_DATA_TYPE_INT)) {
                json.put("val", intent.getIntExtra(KEY_VALUE, Integer.MIN_VALUE));
            } else if (type.equals(SenseSettings.SENSOR_DATA_TYPE_JSON)) {
                json.put("val", new JSONObject(intent.getStringExtra(KEY_VALUE)));
            } else if (type.equals(SenseSettings.SENSOR_DATA_TYPE_STRING)) {
                json.put("val", intent.getStringExtra(KEY_VALUE));
            } else {
                Log.e(TAG, "Unexpected data type: " + type);
                return;
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException before putting message in buffer database", e);
            return;
        }

        // check if there is connectivity
        if (isOnline()) {

            // check if the app is in real-time sending mode
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (prefs.getString(SenseSettings.PREF_SYNC_RATE, "0").equals("-2")) {
                // real time mode, send immediately
                String value = "";
                if (type.equals(SenseSettings.SENSOR_DATA_TYPE_BOOL)) {
                    value += intent.getBooleanExtra(KEY_VALUE, false);
                } else if (type.equals(SenseSettings.SENSOR_DATA_TYPE_FLOAT)) {
                    value += intent.getDoubleExtra(KEY_VALUE, Double.MIN_VALUE);
                } else if (type.equals(SenseSettings.SENSOR_DATA_TYPE_INT)) {
                    value += intent.getIntExtra(KEY_VALUE, Integer.MIN_VALUE);
                } else if (type.equals(SenseSettings.SENSOR_DATA_TYPE_JSON)) {
                    try {
                        value += new JSONObject(intent.getStringExtra(KEY_VALUE)).toString();
                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException creating object to POST", e);
                        return;
                    }
                } else if (type.equals(SenseSettings.SENSOR_DATA_TYPE_STRING)) {
                    value += intent.getStringExtra(KEY_VALUE);
                }
                
                Log.d(TAG, "Send real-time");
                oldSendSensorData(name, value, type, device);
            } else {
                // normal mode, buffer to memory
                bufferData(json);
            }
        } else {
            // buffer data if there is no connectivity
            bufferData(json);
        }
    }

    private void handleSendIntent(Intent intent) {
        Log.d(TAG, "handleSendIntent");

        if (isOnline()) {

            sendDataFromDb();
            sendDataFromBuffer();

        }
    }

    private boolean isOnline() {
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        final NetworkInfo info = cm.getActiveNetworkInfo();
        return ((null != info) && (info.isConnected()));
    }
    
    public void oldSendSensorData(String sensorName, String sensorValue, String dataType,
            String deviceType) {
        
        // double check device type
        deviceType = deviceType != null ? deviceType : "";
        
        String url = SenseSettings.URL_SEND_SENSOR_VALUE + "?sensorName="
                + URLEncoder.encode(sensorName) + "&sensorValue=" + URLEncoder.encode(sensorValue)
                + "&sensorDataType=" + URLEncoder.encode(dataType) + "&sensorDeviceType="
                + URLEncoder.encode(deviceType);
        final SharedPreferences prefs = getSharedPreferences(SenseSettings.PRIVATE_PREFS,
                android.content.Context.MODE_PRIVATE);
        String cookie = prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, "");
        new Thread(new SendMessageThread(url, cookie)).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // you cannot bind to this service
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        this.buffer = new JSONObject[MAX_BUFFER];
        this.bufferCount = 0;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        emptyBufferToDb();
    }

    /**
     * Deprecated method for starting the service, used in 1.6 and older.
     */
    @Override
    public void onStart(Intent intent, int startid) {
        handleIntent(intent, 0, startid);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent, flags, startId);

        // this service is not sticky, it will get an intent to restart it if necessary
        return START_NOT_STICKY;
    }

    private void openDb() {
        if (false == this.isDbOpen) {
            this.dbHelper = new DbHelper(this);
            this.db = this.dbHelper.getWritableDatabase();
            this.isDbOpen = true;
        }
    }

    private boolean sendData(final JSONObject data) {

        new Thread() {
            public void run() {
                HttpClient client = new DefaultHttpClient();
                client.getConnectionManager().closeIdleConnections(1, TimeUnit.SECONDS);

                try {
                    String url = SenseSettings.URL_SEND_BATCH_DATA;
                    final SharedPreferences prefs = getSharedPreferences(
                            SenseSettings.PRIVATE_PREFS, android.content.Context.MODE_PRIVATE);
                    String cookie = prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, "");

                    // set up HTTP POST
                    URI uri = new URI(url);
                    HttpPost post = new HttpPost(uri);
                    post.setHeader("Cookie", cookie);
                    HttpParams params = new BasicHttpParams().setIntParameter(
                            HttpConnectionParams.CONNECTION_TIMEOUT, 0);
                    post.setParams(params);
                    List<NameValuePair> postForm = new ArrayList<NameValuePair>();
                    postForm.add(new BasicNameValuePair("data", data.toString()));
                    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postForm, "UTF-8");
                    post.setEntity(entity);

                    // execute POST
                    ResponseHandler<String> responseHandler = new BasicResponseHandler();
                    String response = client.execute(post, responseHandler);
                    handleResponse(response);                    

                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "UnsupportedEncodingException sending transmission", e);
                    return;
                } catch (URISyntaxException e) {
                    Log.e(TAG, "URISyntaxException sending transmission", e);
                    return;
                } catch (IOException e) {
                    Log.e(TAG, "IOException sending transmission: " + e.getMessage());
                    return;
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException parsing response from CommonSense: " + e.getMessage());
                    return;
                } finally {
                    client.getConnectionManager().shutdown();
                }
            }
        }.run();

        return true;
    }

    private boolean sendDataFromBuffer() {

        if (this.bufferCount > 0) {

            // create JSON object with the data
            JSONObject json = new JSONObject();
            int sentCount = 0;

            try {
                JSONArray array = new JSONArray();

                // fill array with JSON objects from the buffer database
                for (int i = 0; (i < MAX_POST_DATA) && (i < this.bufferCount); i++) {
                    final String jsonString = this.buffer[i].toString();
                    array.put(new JSONObject(jsonString));
                    sentCount++;
                }
                json.put("data", array);
            } catch (JSONException e) {
                Log.d(TAG, "JSONException creating sensor data to post");
                return false;
            }

            boolean flagMoreData = (sentCount == MAX_POST_DATA);

            // send data to CommonSense
            final boolean success = sendData(json);

            Log.d(TAG, "sent " + sentCount + " sensor values from buffer: " + success);

            // remove data from buffer
            JSONObject[] temp = new JSONObject[MAX_BUFFER];
            System.arraycopy(this.buffer, sentCount, temp, 0, MAX_BUFFER - sentCount);
            this.buffer = temp;
            this.bufferCount = this.bufferCount - sentCount;

            // immediately schedule a new send task if there is still stuff in the database
            if (flagMoreData) {
                Log.d(TAG, "Immediate reschedule of new send task to clean up buffer");
                Intent task = new Intent(ACTION_SEND_DATA);
                startService(task);
            }
        } else {
            // TODO smart transmission scaling
        }

        return true;
    }

    private boolean sendDataFromDb() {
        // query the database
        openDb();
        String[] cols = { DbHelper.COL_ROWID, DbHelper.COL_JSON };
        String sel = DbHelper.COL_ACTIVE + "!=?";
        String[] selArgs = { "true" };
        Cursor c = this.db.query(DbHelper.TABLE_NAME, cols, sel, selArgs, null, null, null, ""
                + MAX_POST_DATA);

        try {
            if (c.getCount() > 0) {

                // create JSON object with the data
                JSONObject json = new JSONObject();
                c.moveToFirst();
                int sentCount = 0;

                try {
                    JSONArray array = new JSONArray();

                    // fill array with JSON objects from the buffer database
                    while (false == c.isAfterLast()) {
                        final String jsonString = c.getString(1);
                        array.put(new JSONObject(jsonString));
                        sentCount++;
                        c.moveToNext();
                    }
                    json.put("data", array);
                } catch (JSONException e) {
                    Log.d(TAG, "JSONException creating sensor data to post");
                    return false;
                }

                boolean flagMoreData = (sentCount == MAX_POST_DATA);

                // send data to CommonSense
                final boolean success = sendData(json);

                Log.d(TAG, "sent " + sentCount + " sensor values from database: " + success);

                // remove data from database
                sentCount = 0;
                c.moveToFirst();
                while (false == c.isAfterLast()) {
                    int id = c.getInt(c.getColumnIndex(DbHelper.COL_ROWID));
                    String where = DbHelper.COL_ROWID + "=?";
                    String[] whereArgs = { "" + id };
                    this.db.delete(DbHelper.TABLE_NAME, where, whereArgs);
                    sentCount++;
                    c.moveToNext();
                }

                // immediately schedule a new send task if there is still stuff in the database
                if (flagMoreData) {
                    Log.d(TAG, "Immediate reschedule of new send task to clean up DB");
                    Intent task = new Intent(ACTION_SEND_DATA);
                    startService(task);
                }
            } else {
                // TODO smart transmission scaling
            }

        } finally {
            c.close();
            closeDb();
        }
        return true;
    }
}
