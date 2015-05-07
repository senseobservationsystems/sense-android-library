package nl.sense_os.service.storage;

import java.nio.BufferOverflowException;

import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main.Motion;
import nl.sense_os.service.constants.SensorData.DataPoint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteStatement;

/**
 * Class that manages a store for sensor data points in a persistent SQLite database. Helper class
 * for {@link LocalStorage}.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
class SQLiteStorage {

    /**
     * Maximum size of in-memory database, in bytes.
     */
    private static final long MAX_INMEMORY_SIZE = 10 * 1000 * 1024; // 10 MB

    /**
     * Maximum number of in-memory data points
     */
    private static final long MAX_INMEMORY_COUNT = 10000;

    /**
     * Limit for number of query results
     */
    public static final int QUERY_RESULTS_LIMIT = 10000;

    /**
     * Limit for number of query results in epi-mode (very low because epi mode data points can be
     * huge)
     */
    public static final int QUERY_RESULTS_LIMIT_EPI_MODE = 60;

    private Context context;
    private boolean persistent;
    private DbHelper dbHelper;
    private long rowCount;

    protected SQLiteStorage(Context context, boolean persistent) {
        this.context = context;
        this.persistent = persistent;
        dbHelper = new DbHelper(context, persistent);

        // set maximum size for in-memory database
        if (!persistent) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.setMaximumSize(MAX_INMEMORY_SIZE);
            db.close();
        }
    }

    /**
     * Efficiently inserts a collection of rows in the database
     * 
     * @param c
     *            Cursor with rows of data points
     * @return The number of data points that were inserted
     */
    public int bulkInsert(Cursor c) {

        // prepare SQL insert statement
        StringBuilder sb = new StringBuilder("INSERT INTO " + DbHelper.TABLE + " ");
        sb.append("(" + DataPoint.SENSOR_NAME);
        sb.append(", " + DataPoint.DISPLAY_NAME);
        sb.append(", " + DataPoint.SENSOR_DESCRIPTION);
        sb.append(", " + DataPoint.DATA_TYPE);
        sb.append(", " + DataPoint.TIMESTAMP);
        sb.append(", " + DataPoint.VALUE);
        sb.append(", " + DataPoint.DEVICE_UUID);
        sb.append(", " + DataPoint.TRANSMIT_STATE + ")");
        sb.append(" VALUES (?,?,?,?,?,?,?,?);");

        // get database
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int insertCount = 0;

        // do transaction
        try {
            db.beginTransaction();

            // execute an insert statement for each row
            c.moveToFirst();
            SQLiteStatement statement = db.compileStatement(sb.toString());
            while (!c.isAfterLast()) {
                statement.bindString(1, c.getString(c.getColumnIndex(DataPoint.SENSOR_NAME)));
                statement.bindString(2, c.getString(c.getColumnIndex(DataPoint.DISPLAY_NAME)));
                statement
                        .bindString(3, c.getString(c.getColumnIndex(DataPoint.SENSOR_DESCRIPTION)));
                statement.bindString(4, c.getString(c.getColumnIndex(DataPoint.DATA_TYPE)));
                statement.bindLong(5, c.getLong(c.getColumnIndex(DataPoint.TIMESTAMP)));
                statement.bindString(6, c.getString(c.getColumnIndex(DataPoint.VALUE)));
                statement.bindString(7, c.getString(c.getColumnIndex(DataPoint.DEVICE_UUID)));
                statement.bindLong(8, c.getInt(c.getColumnIndex(DataPoint.TRANSMIT_STATE)));
                statement.execute();

                insertCount++;

                c.moveToNext();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        db.close();

        // update the row count
        if (!persistent) {
            rowCount += insertCount;
        }

        return insertCount;
    }

    /**
     * Deletes rows from the database
     * 
     * @param where
     * @param selectionArgs
     * @return The number of rows affected
     */
    public int delete(String where, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete(DbHelper.TABLE, where, selectionArgs);
        db.close();

        // update the row count
        if (!persistent) {
            rowCount -= result;
        }

        return result;
    }

    /**
     * Inserts a row into the database.
     * 
     * @param values
     * @return
     */
    public long insert(ContentValues values) {

        if (!persistent && rowCount > MAX_INMEMORY_COUNT) {
            throw new BufferOverflowException();
        }

        // insert in database
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(DbHelper.TABLE, DataPoint.SENSOR_NAME, values);
        db.close();

        // update row count
        if (!persistent) {
            rowCount++;
        }

        return rowId;
    }

    /**
     * Query the database
     * 
     * @param projection
     * @param where
     * @param selectionArgs
     * @param orderBy
     *            How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY
     *            itself). Passing null will use the default sort order, which orders by descending
     *            timestamp.
     * @return Cursor with the result set
     */
    public Cursor query(String[] projection, String where, String[] selectionArgs, String orderBy) {

        // limit parameter depends on epi mode preference
        SharedPreferences pref = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                Context.MODE_PRIVATE);
        String limitStr = "" + QUERY_RESULTS_LIMIT;
        if (pref.getBoolean(Motion.EPIMODE, false)) {
            limitStr = "" + QUERY_RESULTS_LIMIT_EPI_MODE;
        }

        // set default ordering
        if (null == orderBy) {
            orderBy = DataPoint.TIMESTAMP + " DESC";
        }

        // do query
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DbHelper.TABLE, projection, where, selectionArgs, null, null,
                orderBy, limitStr);
        db.close();

        return cursor;
    }

    /**
     * Updates rows in the database
     * 
     * @param newValues
     * @param where
     * @param selectionArgs
     * @return the number of rows affected
     */
    public int update(ContentValues newValues, String where, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int result = db.update(DbHelper.TABLE, newValues, where, selectionArgs);
        db.close();
        return result;
    }
}
