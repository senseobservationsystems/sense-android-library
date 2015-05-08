package nl.sense_os.service.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.BaseColumns;
import android.telephony.TelephonyManager;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Advanced;
import nl.sense_os.service.constants.SensorData.DataPoint;

/**
 * Helper class that assist in creating, opening and managing the SQLite3 database for data points.
 */
public class DbHelper extends SQLiteOpenHelper {

    /**
     * Name of the database on the disk
     */
    private static final String DATABASE_NAME = "persitent_storage.sqlite3";

    /**
     * Version of the database. Increment this when the database structure is changed.
     */
    private static final int DATABASE_VERSION = 4;

    /**
     * Name of the table with the data points.
     */
    static final String TABLE = "persisted_values";

    private static SharedPreferences sMainPrefs;

    private static final String DEFAULT_PASSPHRASE_SALT = "tUI@IBhf3J6o^G*&dno3yH!yC*E5#3qy";

    private static String passphrase = "";

    private static final String TAG = "DbHelper";

    private Context mContext;

    /**
     * Constructor. The database is not actually created or opened until one of
     * {@link #getWritableDatabase()} or {@link #getReadableDatabase()} is called.
     * 
     * @param context
     *            to use to open or create the database
     * @param persistent
     *            true for a persistent database, false for an in-memory database
     */
    public DbHelper(Context context, boolean persistent) {
        // if the database name is null, it will be created in-memory
        super(context, persistent ? DATABASE_NAME : null, null, DATABASE_VERSION);
        this.mContext = context;
        if (null == sMainPrefs) {
            sMainPrefs = mContext.getSharedPreferences(SensePrefs.MAIN_PREFS,
                    Context.MODE_PRIVATE);
        }
        updateEncryption();
        // add a listener to update the encryption settings when it changes
        sMainPrefs.registerOnSharedPreferenceChangeListener(encryptionChanged);
        SQLiteDatabase.loadLibs(context);
    }

    public void updateEncryption()
    {
        boolean encrypt = sMainPrefs.getBoolean(Advanced.ENCRYPT_DATABASE, false);
        if (encrypt) {
            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            String imei = telephonyManager.getDeviceId();
            setPassphrase(imei);
        }
    }

    /**
     * Monitor changes in the database encryption settings
     */
    SharedPreferences.OnSharedPreferenceChangeListener encryptionChanged = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            if(key.equals(Advanced.ENCRYPT_DATABASE) || key.equals(Advanced.ENCRYPT_DATABASE_SALT))
                updateEncryption();
        }
    };

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            final StringBuilder sb = new StringBuilder("CREATE TABLE " + DbHelper.TABLE + "(");
            sb.append(BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT");
            sb.append(", " + DataPoint.SENSOR_NAME + " TEXT");
            sb.append(", " + DataPoint.DISPLAY_NAME + " TEXT");
            sb.append(", " + DataPoint.SENSOR_DESCRIPTION + " TEXT");
            sb.append(", " + DataPoint.DATA_TYPE + " TEXT");
            sb.append(", " + DataPoint.TIMESTAMP + " INTEGER");
            sb.append(", " + DataPoint.VALUE + " TEXT");
            sb.append(", " + DataPoint.DEVICE_UUID + " TEXT");
            sb.append(", " + DataPoint.TRANSMIT_STATE + " INTEGER");
            sb.append(");");
            db.execSQL(sb.toString());
        } catch (SQLiteException e) {
            Log.w(TAG, "Error creating database. Maybe the table is already there", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVers, int newVers) {
        Log.w(TAG, "Upgrading '" + DATABASE_NAME + "' database from version " + oldVers
                + " to " + newVers + ", which will destroy all old data");

        db.execSQL("DROP TABLE IF EXISTS " + DbHelper.TABLE);
        onCreate(db);
    }

    public SQLiteDatabase getWritableDatabase(){
        try {
            return getWritableDatabase(passphrase);
        } catch (SQLiteException e) {
            File plain = this.mContext.getDatabasePath(DATABASE_NAME);
            File encrypted = this.mContext.getDatabasePath("tmp.db");

            // try to open the database without password
            SQLiteDatabase migrate_db = SQLiteDatabase.openOrCreateDatabase(plain, "", null);
            migrate_db.rawExecSQL(String.format("ATTACH DATABASE '%s' AS encrypted KEY '%s'", encrypted.getAbsolutePath(), passphrase));
            migrate_db.rawExecSQL("SELECT sqlcipher_export('encrypted');");
            migrate_db.execSQL("DETACH DATABASE encrypted;");
            migrate_db.close();

            // rename the encrypted file name back
            encrypted.renameTo(new File(plain.getAbsolutePath()));

            return getWritableDatabase(passphrase);
        }
    }

    public SQLiteDatabase getReadableDatabase(){
      return getReadableDatabase(passphrase);
    }

  private void setPassphrase(String imei) {

    String salt = sMainPrefs.getString(Advanced.ENCRYPT_DATABASE_SALT, DEFAULT_PASSPHRASE_SALT);

    MessageDigest md = null;
    try {
      md = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e1) {
      Log.e(TAG, "Error initializing SHA-1 message digest");
    }

    byte[] result_byte = md.digest((imei+salt).getBytes());

    passphrase = new String(result_byte);
  }

}

