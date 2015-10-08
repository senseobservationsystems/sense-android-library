package nl.sense_os.datastorageengine;

public class SensorException extends Exception{

    //Constructs a DatabaseHandlerException with no detail message
    public SensorException(){}

    //Constructs a DatabaseHandlerException with the specified detail message
    public SensorException(String message){
        super(message);
    }

    //Constructs a DatabaseHandlerException with the specified detail message and cause
    public SensorException(String message, Throwable cause){
        super(message, cause);
    }

    //Constructs a DatabaseHandlerException with the specified cause and a detail message of the cause if it is not null.
    public SensorException(Throwable cause){
        super(cause);
    }

}
