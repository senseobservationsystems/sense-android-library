package nl.sense_os.service.constants;


/**
 * Utility class that contains constants to map the status preference integer to actual status of
 * the system.<br/>
 * <br/>
 * Example:
 * 
 * <pre>
 * int status = ServiceStateHelper.getInstance(service).getStatusCode();
 * boolean ambience = (status &amp; SenseStatusCode.AMBIENCE) &gt; 0;
 * </pre>
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class SenseStatusCodes {

    public static final int AMBIENCE = 0x01;
    public static final int CONNECTED = 0x02;
    public static final int DEVICE_PROX = 0x04;
    public static final int EXTERNAL = 0x08;
    public static final int LOCATION = 0x10;
    public static final int MOTION = 0x20;
    public static final int PHONESTATE = 0x40;
    public static final int QUIZ = 0x80;
    public static final int RUNNING = 0x100;

    private SenseStatusCodes() {
        // private constructor to prevent instantiation
    }
}
