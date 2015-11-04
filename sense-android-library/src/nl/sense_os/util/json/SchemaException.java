package nl.sense_os.util.json;

public class SchemaException extends Exception {
    public SchemaException(){}
    public SchemaException(String message){
        super(message);
    }
    public SchemaException(String message, Throwable cause){
        super(message, cause);
    }
    public SchemaException(Throwable cause){
        super(cause);
    }
}
