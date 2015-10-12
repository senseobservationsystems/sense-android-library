package nl.sense_os.datastorageengine.test;

import android.test.AndroidTestCase;
import java.util.concurrent.ExecutionException;
import nl.sense_os.datastorageengine.DataSyncer;
import nl.sense_os.datastorageengine.SensorDataProxy;

/**
 * Created by fei on 09/10/15.
 */
public class TestDataSyncer extends AndroidTestCase{

    private DataSyncer dataSyncer;
    private String userId = "userId";
    private String appKey = "etw3534ywer";
    private String sessionId = "sessionId";

    @Override
    protected void setUp () throws Exception {
        dataSyncer = new DataSyncer(getContext(), userId, SensorDataProxy.SERVER.STAGING, appKey, sessionId);
    }

    @Override
    protected void tearDown () throws Exception {
        super.tearDown();
    }

    private void testInitializeSucceeded() throws InterruptedException, ExecutionException {
        dataSyncer.initialize();
    }

    private void testExecSchedulerSucceeded() throws InterruptedException, ExecutionException {
        dataSyncer.execScheduler();
    }
}
