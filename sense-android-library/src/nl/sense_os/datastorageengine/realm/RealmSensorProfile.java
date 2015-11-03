package nl.sense_os.datastorageengine.realm;


import org.json.JSONException;
import org.json.JSONObject;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmSensorProfile extends RealmObject{

    @PrimaryKey
    private String sensorName;
    private String profile; // Stringified JSONObject

    public RealmSensorProfile() {}

    public RealmSensorProfile(String sensorName, String profile) {
        this.sensorName = sensorName;
        this.profile = profile;
    }

    public void setSensorName(String sensorName) { this.sensorName = sensorName; }

    public String getSensorName() { return this.sensorName; }

    public void setProfile(String profile) { this.profile = profile; }

    public String getProfile() { return this.profile; }

    public static JSONObject getProfileAsJSONObject(RealmSensorProfile realmSensorProfile) throws JSONException {
        return new JSONObject(realmSensorProfile.getProfile());
    }

}
