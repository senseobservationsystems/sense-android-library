package nl.sense_os.senseservice;

import android.test.AndroidTestCase;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import nl.sense_os.datastorageengine.DSEConfig;
import nl.sense_os.datastorageengine.DataStorageEngine;
import nl.sense_os.platform.SensePlatform;
import nl.sense_os.service.constants.SensePrefs;

/**
 * Created by ted@sense-os.nl on 12/1/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public class TestDSEConfig extends AndroidTestCase{
    @Override
    protected void setUp() throws Exception {
        SenseServiceUtils.createAccountAndLoginService(getContext());
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        SenseServiceUtils.logout(getContext());
        super.tearDown();
    }

    public void testUpdateConfig() throws InterruptedException, ExecutionException, TimeoutException {
        // set the Sense pref
        SensePlatform sensePlatform = SenseServiceUtils.getSensePlatform(getContext());
        // set local persist period
        sensePlatform.getService().setPrefInt(SensePrefs.Main.Advanced.RETENTION_HOURS, 60);

        // Get the DSE
        DataStorageEngine dataStorageEngine = DataStorageEngine.getInstance(getContext());
        // Wait until the DSE is ready
        dataStorageEngine.onReady().get(60, TimeUnit.SECONDS);
        // get the DSE periodic sync
        DSEConfig dseConfig = dataStorageEngine.getConfig();
        // set local persist period
        sensePlatform.getService().setPrefInt(SensePrefs.Main.Advanced.RETENTION_HOURS, 1);

        // wait one second
        // Wait until the DSE is ready
        dataStorageEngine.onReady().get(60, TimeUnit.SECONDS);
        // check the difference with the previous config
        DSEConfig updatedConfig = dataStorageEngine.getConfig();
        assertFalse(dseConfig.localPersistancePeriod == updatedConfig.localPersistancePeriod);
    }

    public void testDataSync(){

    }

}
