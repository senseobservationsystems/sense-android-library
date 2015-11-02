package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import nl.sense_os.datastorageengine.DSEConstants;
import nl.sense_os.datastorageengine.DataSyncer;
import nl.sense_os.datastorageengine.DatabaseHandler;
import nl.sense_os.datastorageengine.SensorDataProxy;

/**
 * Created by fei on 09/10/15.
 */
public class TestDataSyncer extends AndroidTestCase{
    private CSUtils csUtils;
    private Map<String, String> newUser;
    SensorDataProxy.SERVER server = SensorDataProxy.SERVER.STAGING;
    private String appKey = "E9Noi5s402FYo2Gc6a7pDTe4H3UvLkWa";  // application key for dev, android, Brightr ASML
    private String sessionId;
    private DataSyncer dataSyncer;


    @Override
    protected void setUp () throws Exception {
        csUtils = new CSUtils(false);
        newUser = csUtils.createCSAccount();
        String userId = newUser.get("id");
        sessionId = csUtils.loginUser(newUser.get("username"), newUser.get("password"));
        DatabaseHandler databaseHandler = new DatabaseHandler(getContext(), userId);
        SensorDataProxy proxy = new SensorDataProxy(server, appKey, sessionId);
        dataSyncer = new DataSyncer(getContext(), databaseHandler, proxy);

        // add a data point of a sensor to remote
//        Date dateType = new Date();
//        int value = 0;
//        long date = dateType.getTime();
//        JSONArray dataArray = new JSONArray();
//            JSONObject meta = new JSONObject();
//            meta.put("date",date);
//            meta.put("value",value);
//            dataArray.put(meta);
//        //TODO: no sensor is created in backend
//        proxy.putSensorData(DataSyncer.SOURCE,"noise_sensor",dataArray);

    }

    @Override
    protected void tearDown () throws Exception {
        csUtils.deleteAccount(newUser.get("username"), newUser.get("password"), newUser.get("id"));
    }

//    public void testInitializeSucceeded() throws InterruptedException, ExecutionException {
//        //TODO: how to test the asynchronous process, or lose of the internet
//        dataSyncer.initialize();
//    }

    public void testExecSchedulerSucceeded() throws InterruptedException, ExecutionException {
        dataSyncer.enablePeriodicSync();
    }
}
