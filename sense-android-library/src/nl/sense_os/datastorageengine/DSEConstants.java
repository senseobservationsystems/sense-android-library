package nl.sense_os.datastorageengine;

/**
 * Created by fei on 30/10/15.
 */
public class DSEConstants {
    public enum SERVER {LIVE, STAGING};
    //User info
    public static String SESSION_ID = "";
    public static String APP_KEY = "";
    public static String USER_ID = "";

    //Default value of syncing period, 30 mins in milliseconds
    public static long SYNC_RATE = 1800000L;
    public static long PERSIST_PERIOD = 2678400000L;
    //Default value of syncing period, 30 mins in milliseconds
    public static final long DEFAULT_SYNC_RATE = 1800000L;
    // Default value of persistPeriod, 31 days in milliseconds
    public static final long DEFAULT_PERSIST_PERIOD = 2678400000L;

    public static SERVER CURRENT_SERVER = SERVER.STAGING;

}
