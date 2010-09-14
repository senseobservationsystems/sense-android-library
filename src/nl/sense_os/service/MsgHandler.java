package nl.sense_os.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import nl.sense_os.app.SenseSettings;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

public class MsgHandler extends IntentService {

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

    public static final String KEY_DATA_TYPE = "data_type";
    public static final String KEY_INTENT_TYPE = "intent_type";
    public static final String KEY_SENSOR_NAME = "sensor_name";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_VALUE = "value";
    private static final String TAG = "Sense MsgHandler";
    public static final int TYPE_NEW_MSG = 1;
    public static final int TYPE_SEND_MSG = 2;
    private HttpClient client;
    private SQLiteDatabase db;
    private DbHelper dbHelper;
    private boolean isDbOpen;

    public MsgHandler() {
        super("Sense MsgHandler");
        this.isDbOpen = false;
    }
    
    private void closeClient() {
        if (null != client) {
            client.getConnectionManager().shutdown();
        }
    }

    private void closeDb() {
        if (true == this.isDbOpen) {
            this.dbHelper.close();
        }
    }

    private void handleNewMsgIntent(Intent intent) {

        String name = intent.getStringExtra(KEY_SENSOR_NAME);
        String type = intent.getStringExtra(KEY_DATA_TYPE);
        long time = intent.getLongExtra(KEY_TIMESTAMP, System.currentTimeMillis());
        JSONObject json = new JSONObject();
        try {
            json.put("name", name);
            json.put("time", time / 1000D);
            json.put("type", type);
            json.put("device", "");
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

        openDb();

        // prepare content values for database entry
        ContentValues values = new ContentValues();
        values.put(DbHelper.COL_JSON, json.toString());
        values.put(DbHelper.COL_ACTIVE, false);

        long rowId = this.db.insert(DbHelper.TABLE_NAME, null, values);
        if (rowId >= 0) {
            Log.d(TAG, "New message in buffer: #" + rowId + ", sensor: " + name);
        } else {
            Log.e(TAG, "Error creating row in transmit buffer!");
        }
    }

    private void handleSendIntent(Intent intent) {

        // query the database
        openDb();
        String[] columns = { DbHelper.COL_ROWID, DbHelper.COL_JSON };
        String selection = DbHelper.COL_ACTIVE + "!=\"true\"";
        Cursor c = this.db.query(DbHelper.TABLE_NAME, columns, selection, null, null, null, null);
        final int max_entries = 20;
        
        try {
            if (c.getCount() > 0) {

                // create JSON object with the data
                JSONObject json = new JSONObject();
                c.moveToFirst();
                int count = 0;
                try {
                    JSONArray array = new JSONArray();

                    // fill array with JSON objects from the buffer database
                    while (false == c.isAfterLast() && count < max_entries) {
                        final String jsonString = c.getString(1);
                        array.put(new JSONObject(jsonString));
                        count++;
                        c.moveToNext();
                    }
                    json.put("data", array);
                } catch (JSONException e) {
                    Log.d(TAG, "JSONException creating sensor data");
                    return;
                }
//                Log.d(TAG, "Got data from database...\n" + json.toString());

                // send data to CommonSense
                final boolean success = sendData(json);

                // remove data from database
                if (success) {
                    count = 0;
                    c.moveToFirst();
                    while (false == c.isAfterLast() && count < max_entries) {
                        int id = c.getInt(c.getColumnIndex(DbHelper.COL_ROWID));
                        this.db.delete(DbHelper.TABLE_NAME, DbHelper.COL_ROWID + "=" + id, null);
                        count++;
                        c.moveToNext();
                    }
                }
            } else {
                // TODO smart transmission scaling
            }

        } finally {
            c.close();
        }
    }

    @Override
    public void onDestroy() {
        closeDb();
        closeClient();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        final int intentType = intent.getIntExtra(KEY_INTENT_TYPE, Integer.MIN_VALUE);
        switch (intentType) {
        case TYPE_NEW_MSG:
            handleNewMsgIntent(intent);
            break;
        case TYPE_SEND_MSG:
            handleSendIntent(intent);
            break;
        default:
            Log.e(TAG, "Unexpected intent received: " + intentType);
            return;
        }
    }
    
    private void openClient() {
        if (null == client) {
            client = new DefaultHttpClient();
            client.getConnectionManager().closeIdleConnections(15, TimeUnit.SECONDS);
        }
    }

    private void openDb() {
        if (false == this.isDbOpen) {
            this.dbHelper = new DbHelper(this);
            this.db = this.dbHelper.getWritableDatabase();
            this.isDbOpen = true;
        }
    }

    private boolean sendData(JSONObject json) {

        openClient();

        try {
            String url = SenseSettings.URL_SEND_SENSOR_DATA + "?data="
                    + URLEncoder.encode(json.toString(), HTTP.UTF_8);
            final SharedPreferences prefs = getSharedPreferences(SenseSettings.PRIVATE_PREFS,
                    android.content.Context.MODE_PRIVATE);
            String cookie = prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, "");

            // set up HTTP POST
            URI uri = new URI(url);
            HttpPost post = new HttpPost(uri);
            post.setHeader("Cookie", cookie);

            // execute POST
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            
            // parse response
            JSONObject response = new JSONObject(client.execute(post, responseHandler));
            
            if (response.getString("status").equals("ok")) {
                Log.d(TAG, "Sent sensor data OK! Response message:\n" + response.getString("msg"));
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
                    Log.e(TAG, "SQL error storing data in CommonSense: " + response.getString("msg"));
                    break;                    
                default:
                    Log.e(TAG, "Error sending sensor data: " + error);
                }
                return false;
            }

        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnsupportedEncodingException sending transmission", e);
            return false;
        } catch (URISyntaxException e) {
            Log.e(TAG, "URISyntaxException sending transmission", e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "IOException sending transmission: " + e.getMessage());
            return false;
        } catch (JSONException e) {
            Log.e(TAG, "JSONException parsing response from CommonSense " + e.getMessage());
            return false;
        }

        return true;
    }
}
