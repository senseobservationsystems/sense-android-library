package nl.sense_os.datastorageengine;

//import com.fasterxml.jackson.databind.JsonNode;
//import com.github.fge.jackson.JsonLoader;
//import com.github.fge.jsonschema.core.exceptions.ProcessingException;
//import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
//import com.github.fge.jsonschema.core.load.configuration.LoadingConfigurationBuilder;
//import com.github.fge.jsonschema.core.report.ProcessingReport;
//import com.github.fge.jsonschema.examples.Utils;
//import com.github.fge.jsonschema.main.JsonSchema;
//import com.github.fge.jsonschema.main.JsonSchemaFactory;

import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;
import nl.sense_os.datastorageengine.realm.RealmDataPoint;

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

//    public static boolean validate(Realm realm, String sensorName, DataPoint dataPoint) throws IOException, ProcessingException {
//        // the default validator for draft v4
//        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
//        //TODO: solve this resource issue later, should be sensorProfile here
//        SensorProfile sensorProfile = realm
//                .where(SensorProfile.class)
//                .equalTo("sensorName", sensorName)
//                .findFirst();
//
//        final JsonNode defaultStructure = JsonLoader.fromString(sensorProfile.getDataStructure());
//        final JsonNode actaulStructure = JsonLoader.fromString(dataPoint.getValueAsString());
//
//        final JsonSchema defaultSchema = factory.getJsonSchema(defaultStructure);
//
//
//        //Should be the value of the dataPoint here
//        //TODO: throw an exception instead ?
//        return defaultSchema.validInstance(actaulStructure);
//    }


}
