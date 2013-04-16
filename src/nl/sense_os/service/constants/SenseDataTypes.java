package nl.sense_os.service.constants;

/**
 * Set of predefined names for the data types that CommonSense supports. Useful for creating new
 * sensor data value Intents.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 */
public class SenseDataTypes {

    public static final String BOOL = "bool";
    public static final String FLOAT = "float";    
    public static final String ARRAY = "array";    
    public static final String INT = "int";
    public static final String JSON = "json";
    public static final String STRING = "string";
    public static final String FILE = "file";    
    public static final String JSON_TIME_SERIES = "json time serie";

    private SenseDataTypes() {
        // private constructor to prevent instantiation
    }
}
