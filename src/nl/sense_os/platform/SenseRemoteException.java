package nl.sense_os.platform;

//TODO: for some reason we cannot make this a RemoteException, then the jvm will complain when constructing it.
public class SenseRemoteException extends Exception {
	private static final long serialVersionUID = 1L;

	public SenseRemoteException(String msg) {
		super(msg);
	}
}