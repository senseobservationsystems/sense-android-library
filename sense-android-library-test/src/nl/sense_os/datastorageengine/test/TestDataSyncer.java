package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import nl.sense_os.datastorageengine.DataSyncer;
import nl.sense_os.datastorageengine.SensorDataProxy;

/**
 * Created by fei on 09/10/15.
 */
public class TestDataSyncer extends AndroidTestCase{
    private CSUtils csUtils;
    private Map<String, String> newUser;
    private String userId;
    private String appKey;
    private String sessionId;
    private DataSyncer dataSyncer;

    @Override
    protected void setUp () throws Exception {
        csUtils = new CSUtils(false);
        newUser = csUtils.createCSAccount();
        userId = newUser.get("id");
        appKey =  csUtils.APP_KEY;
        sessionId= csUtils.loginUser(newUser.get("username"), newUser.get("password"));
        dataSyncer = new DataSyncer(getContext(), userId, SensorDataProxy.SERVER.STAGING, appKey, sessionId, 86400000L);

        //Create a sensor and a data point in local storage and then upload to remote for initialization test
        //Step 1: create a proxy
        SensorDataProxy proxy = new SensorDataProxy(SensorDataProxy.SERVER.STAGING, appKey, sessionId);
        //Step 2: create a sensot in remote. //TODO: It is not possible

        //Step 3: create a sensot data in remote
        Date dateType = new Date();
        int value = 0;
        long date = dateType.getTime();
        JSONArray dataArray = new JSONArray();
            JSONObject meta = new JSONObject();
            meta.put("date",date);
            meta.put("value",value);
            dataArray.put(meta);

        proxy.putSensorData(DataSyncer.SOURCE,"noise_sensor",dataArray);

    }

    @Override
    protected void tearDown () throws Exception {
        csUtils.deleteAccount(newUser.get("username"), newUser.get("password"), newUser.get("id"));
    }

    public void testInitializeSucceeded() throws InterruptedException, ExecutionException {
        //TODO: how to test the asynchronous process, or lose of the internet
        dataSyncer.login();
    }

    public void testExecSchedulerSucceeded() throws InterruptedException, ExecutionException {
        dataSyncer.synchronize();
    }
}
