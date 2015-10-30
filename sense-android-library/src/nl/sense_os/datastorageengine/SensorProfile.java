package nl.sense_os.datastorageengine;


import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by fei on 26/10/15.
 */
public class SensorProfile extends RealmObject{

    @PrimaryKey
    private String sensorName;
    private String dataStructure;

    public SensorProfile() {}

    public SensorProfile(String sensorName, String dataStructure) {
        this.sensorName = sensorName;
        this.dataStructure = dataStructure;
    }

    public void setSensorName(String sensorName) { this.sensorName = sensorName; }

    public String getSensorName() { return this.sensorName; }

    public void setDataStructure(String dataStructure) { this.dataStructure = dataStructure; }

    public String getDataStructure() { return this.dataStructure; }

}
