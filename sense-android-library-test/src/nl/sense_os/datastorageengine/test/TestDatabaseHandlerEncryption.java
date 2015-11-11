package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import nl.sense_os.datastorageengine.DatabaseHandler;
import nl.sense_os.datastorageengine.DatabaseHandlerException;
import nl.sense_os.datastorageengine.Sensor;
import nl.sense_os.datastorageengine.SensorException;
import nl.sense_os.datastorageengine.SensorOptions;
import nl.sense_os.datastorageengine.SensorProfileException;
import nl.sense_os.datastorageengine.SensorProfiles;
import nl.sense_os.util.json.SchemaException;

public class TestDatabaseHandlerEncryption extends AndroidTestCase {


    public void testEncryption () throws SensorProfileException, SchemaException, SensorException, DatabaseHandlerException, JSONException {
        byte[] encryptionKey = new byte[64];
        new SecureRandom().nextBytes(encryptionKey);

        // delete realm instances
        deleteRealm(encryptionKey);

        try {
            String userId = "userId";
            String sourceName = "sense-android";
            String sensorName = "noise";

            // Create a sensor profile by hand, so we don't have to fetch them from the server via SensorDataProxy
            SensorProfiles profiles = new SensorProfiles(getContext(), encryptionKey);
            profiles.create("noise",
                    new JSONObject("{\"$schema\": \"http:\\/\\/json-schema.org\\/draft-04\\/schema#\",\"description\": \"The Ambient noise in decibel\",\"type\": \"number\"}"));

            DatabaseHandler databaseHandler = new DatabaseHandler(getContext(), encryptionKey, userId);
            SensorOptions options = new SensorOptions();
            options.setMeta(new JSONObject("{\"text\":\"secret information...\"}"));
            databaseHandler.createSensor(sourceName, sensorName, options);

            // if we create a database handler without encryption key, it should not be readable
            try {
                byte[] noEncryptionKey = null;
                DatabaseHandler databaseHandler2 = new DatabaseHandler(getContext(), noEncryptionKey, userId);
                databaseHandler2.getSensor(sourceName, sensorName);
                fail("Should have thrown an exception");
            }
            catch (IllegalArgumentException err) {
                assertEquals("Illegal Argument: Invalid format of Realm file.", err.getMessage());
            }
        }
        finally {
            // delete realm instances
            deleteRealm(encryptionKey);
        }

    }


    /**
     * Helper function to delete realm
     * @param encryptionKey
     */
    void deleteRealm (byte[] encryptionKey) {
        RealmConfiguration config = new RealmConfiguration.Builder(getContext()).encryptionKey(encryptionKey).build();
        Realm.deleteRealm(config);

        RealmConfiguration config2 = new RealmConfiguration.Builder(getContext()).build();
        Realm.deleteRealm(config2);
    }

}
