package sensei.sense_os.nl.sensedemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import nl.sense_os.service.ServiceStateHelper;
import nl.sense_os.service.commonsense.SenseApi;
import nl.sense_os.service.constants.SensePrefs;

/**
 * Created by ted on 10/27/15.
 */
public class SetupUser {

    private static final String TAG = "Setup User";
    public final static String uniqueID = ""+ System.currentTimeMillis();
    public final static String email = "spam+ce_"+uniqueID+"@sense-os.nl";
    public final static String password = SenseApi.hashPassword("Test1234");
    public final static String publicGroupID = "2924";
    public final static String privateGroupID = "24776";
    public final static String privateGroupPassword = SenseApi.hashPassword("test");

    public static boolean setup(Context context)
    {
        boolean success = createUser(context);
        success &= login(context);
        success &= joinPublicGroup(context);
        success &= joinPrivateGroup(context);
        return success;
    }

    public static boolean createUser(Context context)
    {
        String name = "Test";
        String surname = "User";
        String mobile = "1234567890";
        Log.d(TAG, "Username:"+email);
        try {
            int result = SenseApi.registerUser(context, email, password, name, surname, email, mobile);
            if(result != 0){
                throw new RuntimeException("Error creating user: "+result);
            }
            return true;
        } catch(Exception e) {
            String message = e.getMessage() != null? e.getMessage() : e.toString();
            Log.e(TAG,message);
            return false;
        }
    }

    public static boolean login(Context context)
    {
        try {
            int result = SenseApi.login(context, email, password);
            if(result != 0){
                throw new RuntimeException("Error login user: "+result);
            }
            ServiceStateHelper.getInstance(context).setLoggedIn(true);
            return true;
        } catch(Exception e) {
            String message = e.getMessage() != null? e.getMessage() : e.toString();
            Log.e(TAG, message);
            return false;
        }
    }

    public static boolean joinPublicGroup(Context context)
    {
        try {
            if(SenseApi.joinGroup(context, publicGroupID))
                return true;
            throw new RuntimeException("Error joining public group");
        }
        catch(Exception e) {
            String message = e.getMessage() != null? e.getMessage() : e.toString();
            Log.e(TAG, message);
            return false;
        }
    }

    public static boolean joinPrivateGroup(Context context)
    {
        try {
            if(SenseApi.joinPrivateGroup(context, privateGroupID, privateGroupPassword))
                return true;
            throw new RuntimeException("Error joining public group");
        }
        catch(Exception e) {
            String message = e.getMessage() != null? e.getMessage() : e.toString();
            Log.e(TAG, message);
            return false;
        }
    }

    public static void clearUserPreferences(Context context)
    {
        SharedPreferences mainPrefs = context.getSharedPreferences(SensePrefs.AUTH_PREFS, Context.MODE_PRIVATE);
        mainPrefs.edit().clear().commit();
    }
}
