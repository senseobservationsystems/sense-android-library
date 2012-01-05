package nl.sense_os.service;

import nl.sense_os.service.ISenseServiceCallback;

interface ISenseService 
{	
    /**
     * Tries to log in at CommonSense using the supplied username and password. After login, the
     * service remembers the username and password.
     * 
     * @param username
     *            username for login
     * @param pass
     *            hashed password for login
     * @return 0 if login completed successfully, -2 if login was forbidden, and -1 for any other
     *         errors.
     */
    int changeLogin(String username, String password);
    
    /**
     * Logs out a user, destroying his or her records.
     */
    void logout();
    
    /**
     * Registers a new user at CommonSense and logs in immediately.
     *
     * @param username 
     *         Username for the new user.
     * @param password 
     *         Unhashed password String for the new user.
     * @param name 
     *         First name (optional, null if not required).
     * @param surname 
     *         Surname (optional, null if not required).
     * @param email 
     *         Email address (optional, null if not required).
     * @param mobile 
     *         Phone number, preferably in E164 format (optional, null if not required).
     * @return 0 if registration completed successfully, -2 if the user already exists, and -1
     *         otherwise.
     */
    int register(String username, String password, String name, String surname, String email, 
            String mobile);
    
    /**
     * @param appSecret
     *         Secret identifier of the application that requests the session ID. Only certain 
     *         'safe' apps are allowed to access the session ID.
     * @return
     *         The currently active session ID for CommonSense, or null if there is no active 
     *         session
     * @throws RemoteException 
     *         If the app ID is not valid
     */
    String getSessionId(String appSecret);  
    
    boolean getPrefBool(String key, boolean defValue);
    float getPrefFloat(String key, float defValue);
    int getPrefInt(String key, int defValue);
    long getPrefLong(String key, long defValue);
    String getPrefString(String key, String defValue);
    void getStatus(ISenseServiceCallback callback);
    void setPrefBool(String key, boolean value);
    void setPrefFloat(String key, float value);
    void setPrefInt(String key, int value);
    void setPrefLong(String key, long value);
    void setPrefString(String key, String value);
    void toggleAmbience(boolean active);
	void toggleDeviceProx(boolean active);
    void toggleExternalSensors(boolean active);
	void toggleLocation(boolean active);
	void toggleMain(boolean active);
	void toggleMotion(boolean active);
    void togglePhoneState(boolean active);
    void togglePopQuiz(boolean active);
}


