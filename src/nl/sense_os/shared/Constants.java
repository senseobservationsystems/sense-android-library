package nl.sense_os.shared;

public class Constants {

    public static final String PREF_STATUS_AMBIENCE = "ambience component status";
    public static final String PREF_STATUS_DEV_PROX = "device proximity component status";
    public static final String PREF_STATUS_EXTERNAL = "external services component status";
    public static final String PREF_STATUS_LOCATION = "location component status";
    public static final String PREF_STATUS_MAIN = "main service status";
    public static final String PREF_STATUS_MOTION = "motion component status";
    public static final String PREF_STATUS_PHONESTATE = "phone state component status";
    public static final String PREF_STATUS_POPQUIZ = "pop quiz component status";    
    public static final int STATUSCODE_AMBIENCE = 0x01;
    public static final int STATUSCODE_CONNECTED = 0x02;
    public static final int STATUSCODE_DEVICE_PROX = 0x04;
    public static final int STATUSCODE_EXTERNAL = 0x08;
    public static final int STATUSCODE_LOCATION = 0x10;
    public static final int STATUSCODE_MOTION = 0x20;
    public static final int STATUSCODE_PHONESTATE = 0x40;
    public static final int STATUSCODE_QUIZ = 0x80;
    public static final int STATUSCODE_RUNNING = 0x100;
}
