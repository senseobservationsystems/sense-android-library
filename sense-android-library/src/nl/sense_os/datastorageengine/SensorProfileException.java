package nl.sense_os.datastorageengine;

public class SensorProfileException extends Exception{

    //Constructs a DatabaseHandlerException with no detail message
    public SensorProfileException(){}

    //Constructs a DatabaseHandlerException with the specified detail message
    public SensorProfileException(String message){
        super(message);
    }

    //Constructs a DatabaseHandlerException with the specified detail message and cause
    public SensorProfileException(String message, Throwable cause){
        super(message, cause);
    }

    //Constructs a DatabaseHandlerException with the specified cause and a detail message of the cause if it is not null.
    public SensorProfileException(Throwable cause){
        super(cause);
    }

}
