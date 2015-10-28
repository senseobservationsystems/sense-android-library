package nl.sense_os.datastorageengine;

/**
 * Created by fei on 16/09/15.
 */
public class DatabaseHandlerException extends Exception{

    //Constructs a DatabaseHandlerException with no detail message
    public DatabaseHandlerException(){}

    //Constructs a DatabaseHandlerException with the specified detail message
    public DatabaseHandlerException(String message){
        super(message);
    }

    //Constructs a DatabaseHandlerException with the specified detail message and cause
    public DatabaseHandlerException(String message, Throwable cause){
        super(message, cause);
    }

    //Constructs a DatabaseHandlerException with the specified cause and a detail message of the cause if it is not null.
    public DatabaseHandlerException(Throwable cause){
        super(cause);
    }

}
