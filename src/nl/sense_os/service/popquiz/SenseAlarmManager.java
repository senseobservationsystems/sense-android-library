/*
 * ***********************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 * **
 * ************************************************************************************************
 * *********
 */
package nl.sense_os.service.popquiz;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import nl.sense_os.service.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SenseAlarmManager {
    /**
     * Inner class that handles the creation of the SQLite3 database with the desired tables and
     * columns.
     * 
     * To view the Sqlite3 database in a terminal: $ adb shell # sqlite3
     * /data/data/nl.sense_os.dji/databases/data.sqlite3 sqlite> .headers ON sqlite> select * from
     * testTbl;
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            final StringBuilder sbEntry = new StringBuilder("CREATE TABLE " + TABLE_ENTRY + "(");
            sbEntry.append(COL_ROW_ID + " integer primary key autoincrement");
            sbEntry.append(", " + COL_ENTRY_TIME + " integer");
            sbEntry.append(", " + COL_QUIZ_ID + " integer");
            sbEntry.append(", " + COL_QSTN_ID + " integer");
            sbEntry.append(", " + COL_ANSW_ID + " integer");
            sbEntry.append(");");
            db.execSQL(sbEntry.toString());

            final StringBuilder sbQuiz = new StringBuilder("CREATE TABLE " + TABLE_QUIZ + "(");
            sbQuiz.append(COL_ROW_ID + " integer primary key autoincrement");
            sbQuiz.append(", " + COL_QUIZ_ID + " integer");
            sbQuiz.append(", " + COL_QUIZ_VAL + " String");
            sbQuiz.append(", " + COL_QSTN_ID + " integer");
            sbQuiz.append(", " + COL_QSTN_VAL + " String");
            sbQuiz.append(", " + COL_ANSW_ID + " integer");
            sbQuiz.append(", " + COL_ANSW_VAL + " String");
            sbQuiz.append(");");
            db.execSQL(sbQuiz.toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVers, int newVers) {
            Log.w(TAG, "Upgrading database from version " + oldVers + " to " + newVers
                    + ", which will destroy all old data");

            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENTRY);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUIZ);
            db.execSQL("DROP TABLE IF EXISTS " + "questions");
            db.execSQL("DROP TABLE IF EXISTS " + "answers");
            db.execSQL("DROP TABLE IF EXISTS " + "Alarms");
            onCreate(db);
        }
    }

    public static final String COL_ANSW_ID = "answer_id";
    public static final String COL_ANSW_VAL = "answer_val";
    public static final String COL_ENTRY_TIME = "entry_time";
    public static final String COL_QSTN_ID = "question_id";
    public static final String COL_QSTN_VAL = "question_val";
    public static final String COL_QUIZ_ID = "quiz_id";
    public static final String COL_QUIZ_VAL = "quiz_val";
    public static final String COL_ROW_ID = "_id";
    public static final String DATABASE_NAME = "data.sqlite3";
    public static final int DATABASE_VERSION = 6;
    public static final String KEY_ENTRY_ID = "nl.sense_os.service.EntryId";
    public static final String KEY_QUIZ_ID = "nl.sense_os.service.QuizId";
    public static final int NOTIFICATION_QUIZ_ID = 1;
    public static final String PREFS_LOCATION = "prefs_location";
    public static final String TABLE_ENTRY = "entries";
    public static final String TABLE_QUIZ = "quizes";
    private static final String TAG = "SenseAlarmMgr";
    private final Context ctx;
    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private boolean dbOpened;

    /**
     * Constructor for AlertHandler object.
     * 
     * @param context
     *            Application context, used to create intents and open the database.
     */
    public SenseAlarmManager(Context context) {

        this.ctx = context;
        this.dbOpened = false;
    }

    public void cancelEntry() {

        // safely open the database
        boolean closeMeWhenDone = false;
        if (false == this.dbOpened) {
            closeMeWhenDone = true;
            openDb();
        }

        // delete upcoming alarms from the database
        final long[] upcoming = readUpcoming();
        for (final long l : upcoming) {
            delete(l);
        }

        // only close the database if it was opened by this method
        if (true == closeMeWhenDone) {
            closeDb();
        }
    }

    public void cancelSyncAlarm() {
        final Intent popQuestionIntent = new Intent("nl.sense_os.service.AlarmPopQuestionUpdate");
        final PendingIntent popQuestionPI = PendingIntent.getBroadcast(this.ctx, 0,
                popQuestionIntent, 0);
        final AlarmManager alarmMgr = (AlarmManager) this.ctx
                .getSystemService(Context.ALARM_SERVICE);
        alarmMgr.cancel(popQuestionPI);
    }

    /**
     * Closes the database.
     */
    private void closeDb() {
        this.dbHelper.close();
        this.dbOpened = false;
    }

    /**
     * Creates new quiz entry in the database and sets the alarm to display it.
     * 
     * @param entryTime
     *            time stamp of the new alarm
     * @param quiz
     *            id of the quiz to display
     * @return <code>true</code> if created successfully
     */
    public boolean createEntry(long entryTime, int quiz) {

        // safely open the database
        boolean closeMeWhenDone = false;
        if (false == this.dbOpened) {
            closeMeWhenDone = true;
            openDb();
        }

        // Check if entryTime makes sense or whether we should get the time ourselves
        if (entryTime == 0) {
            entryTime = getNextQuarterHour();
        }

        // Delete any for identical quiz alarms that are already inserted for this time slot
        final String select = COL_ENTRY_TIME + "=" + entryTime + " AND " + COL_QUIZ_ID + "=" + quiz;
        final int del = this.db.delete(TABLE_ENTRY, select, null);
        if (del > 0) {
            Log.w(TAG, "This alarm was already present in the DB. " + del + " entries deleted.");
        }

        // insert an entry for every question of the quiz in the database
        String qstnSel = COL_QUIZ_ID + "=" + quiz;
        Cursor c = this.db.query(true, TABLE_QUIZ, new String[] { COL_QSTN_ID }, qstnSel, null,
                null, null, null, null);
        c.moveToFirst();
        long result = -1;
        while (false == c.isAfterLast()) {
            final ContentValues saveValues = new ContentValues();
            saveValues.put(COL_ENTRY_TIME, entryTime);
            saveValues.put(COL_QUIZ_ID, quiz);
            saveValues.put(COL_QSTN_ID, c.getInt(c.getColumnIndex(COL_QSTN_ID)));
            saveValues.put(COL_ANSW_ID, -1);

            // only insert if the alert is not in the database yet
            result = this.db.insert(TABLE_ENTRY, null, saveValues);

            c.moveToNext();
        }
        c.close();

        // set alarm for next quiz
        if (result > 0) {
            Log.d(TAG, "New alarm set for quiz nr. " + quiz + " on "
                    + new SimpleDateFormat("HH:mm").format(new Date(entryTime)));

            Intent alarmIntent = new Intent("nl.sense_os.service.AlarmPeriodic");
            alarmIntent.putExtra(KEY_ENTRY_ID, entryTime);
            alarmIntent.putExtra(KEY_QUIZ_ID, quiz);
            PendingIntent alarmOp = PendingIntent.getBroadcast(this.ctx, (int) entryTime,
                    alarmIntent, 0);
            AlarmManager mgr = (AlarmManager) this.ctx.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC_WAKEUP, entryTime, alarmOp);
        } else {
            Log.w(TAG, "Problem inserting new entry in the database");
        }

        // only close the database if it was opened by this method
        if (true == closeMeWhenDone) {
            closeDb();
        }

        return result > 0;
    }

    /**
     * Set alarm to synchronize the quiz data with CommonSense. Last sync time is fetched from the
     * preferences.
     */
    public void createSyncAlarm() {

        // get time of last refresh from preferences
        SharedPreferences mainPrefs = this.ctx.getSharedPreferences(Constants.MAIN_PREFS,
                Context.MODE_WORLD_WRITEABLE);
        long lastUpdate = mainPrefs.getLong(Constants.PREF_QUIZ_SYNC_TIME, 0);

        // next update time 8 hours from last one
        final long nextUpdate = lastUpdate + 8 * 60 * 60 * 1000;

        // set alarm to go off for next update
        final Intent refreshIntent = new Intent("nl.sense_os.service.AlarmPopQuestionUpdate");
        final PendingIntent refreshPI = PendingIntent.getBroadcast(this.ctx, 0, refreshIntent, 0);
        final AlarmManager mgr = (AlarmManager) this.ctx.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC_WAKEUP, nextUpdate, refreshPI);
    }

    /**
     * Stores a pop quiz with questions and answers in the database.
     */
    public void createQuiz(Quiz quiz) {

        // safely open the database
        boolean closeMeWhenDone = false;
        if (false == this.dbOpened) {
            closeMeWhenDone = true;
            openDb();
        }

        // delete any quiz with the same id
        this.db.delete(TABLE_QUIZ, COL_QUIZ_ID + "=" + quiz.id, null);

        // insert each question and its answers in the database
        for (final Question qstn : quiz.questions) {
            for (final Answer answ : qstn.answers) {
                final ContentValues values = new ContentValues();
                values.put(COL_QUIZ_ID, quiz.id);
                values.put(COL_QUIZ_VAL, quiz.description);
                values.put(COL_QSTN_ID, qstn.id);
                values.put(COL_QSTN_VAL, qstn.value);
                values.put(COL_ANSW_ID, answ.id);
                values.put(COL_ANSW_VAL, answ.value);
                this.db.insert(TABLE_QUIZ, null, values);
            }
        }

        // only close the database if it was opened by this method
        if (true == closeMeWhenDone) {
            closeDb();
        }
    }

    /**
     * Delete the entry with the given time from the specified table.
     * 
     * @param time
     *            ID of the row to delete. pass 0 to delete all rows.
     * @return true if deleted, false otherwise
     */
    public boolean delete(long time) {

        // safely open the database
        boolean closeMeWhenDone = false;
        if (false == this.dbOpened) {
            closeMeWhenDone = true;
            openDb();
        }

        Cursor c = null;
        if (0 == time) {
            // row ID starts at 1, if 0 is passed we delete all entries
            c = this.db.query(TABLE_ENTRY, null, null, null, null, null, null);
        } else {
            // regular behaviour: only read a single request id
            c = this.db.query(TABLE_ENTRY, null, COL_ENTRY_TIME + "=" + time, null, null, null,
                    null);
        }

        // delete the selected rows one by one
        c.moveToFirst();
        int count = 0;
        while (false == c.isAfterLast()) {
            final int rowId = c.getInt(c.getColumnIndex(COL_ROW_ID));
            final long entryId = c.getInt(c.getColumnIndex(COL_ENTRY_TIME));
            int deleted = this.db.delete(TABLE_ENTRY, COL_ROW_ID + "=" + rowId, null);
            Log.d(TAG, "deleting quiz entry " + rowId + ". Result: " + deleted);

            // remove associated alarm from system alarm manager
            if (deleted > 0) {
                final Intent alarmIntent = new Intent("nl.sense_os.service.AlarmPeriodic");
                final PendingIntent alarmOp = PendingIntent.getBroadcast(this.ctx, (int) entryId,
                        alarmIntent, 0);
                final AlarmManager mgr = (AlarmManager) this.ctx
                        .getSystemService(Context.ALARM_SERVICE);
                mgr.cancel(alarmOp);
            }

            count += deleted;
            c.moveToNext();
        }
        c.close();

        // only close the database if it was opened by this method
        if (true == closeMeWhenDone) {
            closeDb();
        }

        return count > 0;
    }

    /**
     * @return the next nearest quiz time, expressed in milliseconds, e.g. 9:00, when it is 8:56.
     */
    private long getNextQuarterHour() {
        final SharedPreferences mainPrefs = ctx.getSharedPreferences(Constants.MAIN_PREFS,
                Context.MODE_WORLD_WRITEABLE);
        final int rate = Integer.parseInt(mainPrefs.getString(Constants.PREF_QUIZ_RATE, "0"));
        long period = 0;
        switch (rate) {
        case -1:
            // often (5 mins)
            period = 5 * 60 * 1000;
            break;
        case 0:
            // normal (15 mins)
            period = 15 * 60 * 1000;
            break;
        case 1:
            // rarely (1 hour)
            period = 60 * 60 * 1000;
            break;
        default:
            Log.e(TAG, "Unexpected quiz rate preference.");
            return -1;
        }
        final long now = System.currentTimeMillis();
        final long remainder = now % period;

        return now - remainder + period;
    }

    /**
     * Opens the alerts database in read-write mode. If it cannot be opened, tries to create a new
     * instance of the database. If it cannot be created, throws an exception to signal the failure.
     * 
     * @throws SQLException
     *             if the database could be neither opened or created.
     */
    private void openDb() throws SQLException {
        this.dbHelper = new DatabaseHelper(this.ctx);
        this.db = this.dbHelper.getWritableDatabase();
        this.dbOpened = true;
    }

    /**
     * @return The tab and row index of the latest finished activity according to its timestamp.
     */
    public int[] readLatestEntry() {

        final int[] result = { -1, -1 };

        // safely open the database
        boolean closeMeWhenDone = false;
        if (false == this.dbOpened) {
            closeMeWhenDone = true;
            openDb();
        }

        final String selection = COL_ANSW_ID + "!=-1";
        final String orderBy = COL_ENTRY_TIME + " DESC";
        final Cursor c = this.db.query(TABLE_ENTRY, null, selection, null, null, null, orderBy);
        c.moveToFirst();
        if (false == c.isAfterLast()) {
            result[0] = c.getInt(c.getColumnIndex(COL_QSTN_ID));
            result[1] = c.getInt(c.getColumnIndex(COL_ANSW_ID));
        }
        c.close();

        // only close the database if it was opened by this method
        if (true == closeMeWhenDone) {
            closeDb();
        }

        return result;
    }

    /**
     * @return IDs of the alarms in the database that do not have a proper activity associated with
     *         them yet.
     */
    public long[] readMissed() {

        // safely open the database
        boolean closeMeWhenDone = false;
        if (false == this.dbOpened) {
            closeMeWhenDone = true;
            openDb();
        }

        // select all unfinished activities that are older than the current time
        final String[] columns = { COL_QUIZ_ID };
        final String selct = COL_ANSW_ID + "=-1 AND " + COL_ENTRY_TIME + "<"
                + System.currentTimeMillis();
        final Cursor c = this.db.query(true, TABLE_ENTRY, columns, selct, null, null, null,
                COL_QUIZ_ID, null);

        c.moveToFirst();
        final int rows = c.getCount();
        final long[] ids = new long[rows];
        for (int i = 0; i < rows; i++) {
            ids[i] = c.getLong(c.getColumnIndex(COL_QUIZ_ID));
            c.moveToNext();
        }
        c.close();

        // only close the database if it was opened by this method
        if (true == closeMeWhenDone) {
            closeDb();
        }

        return ids;
    }

    public Quiz readQuiz(int id) {

        // safely open the database
        boolean closeMeWhenDone = false;
        if (false == this.dbOpened) {
            closeMeWhenDone = true;
            openDb();
        }

        // get quiz description
        final String[] columns = { COL_ROW_ID, COL_QUIZ_VAL, COL_QSTN_ID, COL_QSTN_VAL,
                COL_ANSW_ID, COL_ANSW_VAL };
        final String selection = COL_QUIZ_ID + "=" + id;
        final String orderBy = COL_QSTN_ID + " ASC, " + COL_ANSW_ID + " ASC";
        final Cursor c = db.query(TABLE_QUIZ, columns, selection, null, null, null, orderBy);
        c.moveToFirst();
        final Quiz result = new Quiz();
        if (false == c.isAfterLast()) {
            result.id = id;
            result.description = c.getString(c.getColumnIndex(COL_QUIZ_VAL));

            // read quiz questions
            while (false == c.isAfterLast()) {
                Question qstn = new Question();
                qstn.id = c.getInt(c.getColumnIndex(COL_QSTN_ID));
                qstn.value = c.getString(c.getColumnIndex(COL_QSTN_VAL));

                // read question answers
                while ((false == c.isAfterLast())
                        && (qstn.id == c.getInt(c.getColumnIndex(COL_QSTN_ID)))) {
                    Answer answer = new Answer();
                    answer.id = c.getInt(c.getColumnIndex(COL_ANSW_ID));
                    answer.value = c.getString(c.getColumnIndex(COL_ANSW_VAL));
                    qstn.answers.add(answer);
                    c.moveToNext();
                }
                result.questions.add(qstn);
            }
        } else {
            Log.w(TAG, "Cannot find quiz with id " + id);
            return null;
        }
        c.close();

        // only close the database if it was opened by this method
        if (true == closeMeWhenDone) {
            closeDb();
        }

        return result;
    }

    /**
     * @param id
     *            The row id of requested entry.
     * @return The timestamp of the requested entry.
     */
    public long readTimestamp(long id) {

        // safely open the database
        boolean closeMeWhenDone = false;
        if (false == this.dbOpened) {
            closeMeWhenDone = true;
            openDb();
        }

        final Cursor c = this.db.query(TABLE_ENTRY, new String[] { COL_QUIZ_ID, COL_ENTRY_TIME },
                COL_QUIZ_ID + "=" + id, null, null, null, null, null);

        c.moveToFirst();
        long timestamp = -1;
        if (false == c.isAfterLast()) {
            timestamp = c.getLong(c.getColumnIndex(COL_ENTRY_TIME));
        }
        c.close();

        // only close the database if it was opened by this method
        if (true == closeMeWhenDone) {
            closeDb();
        }

        return timestamp;
    }

    /**
     * @return IDs of unfinished alerts from the database that are scheduled in the future.
     */
    public long[] readUpcoming() {

        // safely open the database
        boolean closeMeWhenDone = false;
        if (false == this.dbOpened) {
            closeMeWhenDone = true;
            openDb();
        }

        final String[] columns = { COL_ROW_ID, COL_ENTRY_TIME };
        final String selct = COL_ANSW_ID + "=-1 AND " + COL_ENTRY_TIME + ">"
                + System.currentTimeMillis();
        final Cursor c = this.db.query(TABLE_ENTRY, columns, selct, null, null, null, COL_QUIZ_ID,
                null);

        c.moveToFirst();
        final int rows = c.getCount();
        final long[] ids = new long[rows];
        for (int i = 0; i < rows; i++) {
            ids[i] = c.getLong(c.getColumnIndex(COL_ENTRY_TIME));
            c.moveToNext();
        }
        c.close();

        // only close the database if it was opened by this method
        if (true == closeMeWhenDone) {
            closeDb();
        }

        return ids;
    }

    /**
     * Tries to store all the finished alerts on the remote Regas server.
     */
    public void storeRemote() {

        // safely open the database
        boolean closeMeWhenDone = false;
        if (false == this.dbOpened) {
            closeMeWhenDone = true;
            openDb();
        }

        final String selection = COL_ANSW_ID + "!=-1";
        final String orderBy = COL_ENTRY_TIME + " DESC";
        final Cursor c = this.db.query(TABLE_ENTRY, null, selection, null, null, null, orderBy);
        c.moveToFirst();

        // OldMsgHandler handler = new OldMsgHandler(this.ctx);
        while (false == c.isAfterLast()) {
            // store the answer
            int qstnId = c.getInt(c.getColumnIndex(COL_QSTN_ID));
            int answId = c.getInt(c.getColumnIndex(COL_ANSW_ID));
            long entryTime = c.getLong(c.getColumnIndex(COL_ENTRY_TIME));
            String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm")
                    .format(new Date(entryTime));
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("question id", qstnId);
            data.put("answer id", answId);
            data.put("date", timeString);
            Log.d(TAG, "Pop quiz data not actually sent"); // TODO send pop quiz data
            // handler.sendSensorData("pop quiz", data);

            // delete the answer's entry in the database
            final String delSelection = COL_ROW_ID + "=" + c.getInt(c.getColumnIndex(COL_ROW_ID));
            this.db.delete(TABLE_ENTRY, delSelection, null);

            c.moveToNext();
        }
        c.close();

        // only close the database if it was opened by this method
        if (true == closeMeWhenDone) {
            closeDb();
        }
    }

    /**
     * Updates an alarm entry with the tab and activity data that was entered by the user.
     * 
     * @param id
     *            row ID of the alarm.
     * @param qstnId
     *            selected tab.
     * @param answId
     *            index of the activity in the selected tab.
     * @return <code>true</code> if the entry was successfully updated.
     */
    public boolean update(long id, int qstnId, long answId) {

        // safely open the database
        boolean closeMeWhenDone = false;
        if (false == this.dbOpened) {
            closeMeWhenDone = true;
            openDb();
        }

        Log.d(TAG, "Updating... Entry " + id + ", question " + qstnId + ", answer " + answId);
        final ContentValues newContent = new ContentValues();
        newContent.put(COL_ANSW_ID, answId);

        final String sel = COL_ENTRY_TIME + "=" + id + " AND " + COL_QSTN_ID + "=" + qstnId;
        final boolean saved = this.db.update(TABLE_ENTRY, newContent, sel, null) > 0;

        if ((true == saved) && (answId >= 0)) {
            Log.d(TAG, "Updating... Trying store on CommonSense");
            storeRemote();
        }

        // only close the database if it was opened by this method
        if (true == closeMeWhenDone) {
            closeDb();
        }

        return saved;
    }
}
