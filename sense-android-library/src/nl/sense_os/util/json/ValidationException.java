package nl.sense_os.util.json;

public class ValidationException extends Exception {
    public ValidationException(){}
    public ValidationException(String message){
        super(message);
    }
    public ValidationException(String message, Throwable cause){
        super(message, cause);
    }
    public ValidationException(Throwable cause){
        super(cause);
    }
}
