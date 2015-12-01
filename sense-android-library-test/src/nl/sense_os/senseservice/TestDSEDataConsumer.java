package nl.sense_os.senseservice;

import android.test.AndroidTestCase;

/**
 * Created by ted@sense-os.nl on 11/30/15.
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public class TestDSEDataConsumer extends AndroidTestCase {


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

    public void testDSEDataConsumer(){
        // create a data producer for
    }
}
