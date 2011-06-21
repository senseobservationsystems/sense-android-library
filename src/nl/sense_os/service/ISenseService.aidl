package nl.sense_os.service;

import nl.sense_os.service.ISenseServiceCallback;

interface ISenseService 
{	
    int changeLogin(String username, String password);
    boolean getPrefBool(String key, boolean defValue);
    float getPrefFloat(String key, float defValue);
    int getPrefInt(String key, int defValue);
    long getPrefLong(String key, long defValue);
    String getPrefString(String key, String defValue);
    void getStatus(ISenseServiceCallback callback);
    int register(String username, String password);
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


