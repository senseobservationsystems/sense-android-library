package nl.sense_os.service;

interface ISenseServiceCallback 
{	
    void statusReport(int status);
    
    /**
     * Callback for change login requests.
     * 
     * @param result 
     *            0 if login completed successfully, -2 if login was forbidden, and -1 for any other errors.
     */
    void onChangeLoginResult(int result);
    
    /**
     * Callback for registration requests.
     * 
     * @param result 
     *            0 if registration completed successfully, -2 if the username is already taken, and -1 for any other errors.
     */
    void onRegisterResult(int result);
}


