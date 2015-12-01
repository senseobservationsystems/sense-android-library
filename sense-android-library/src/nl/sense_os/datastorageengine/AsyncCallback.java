package nl.sense_os.datastorageengine;

/**
 * A generic interface for receiving a success/failure status asynchronously.
 * Created by ted@sense-os.nl on 11/6/15.
 *
 * Copyright (c) 2015 Sense Observation Systems BV. All rights reserved.
 */
public interface AsyncCallback {

    /**
     * Callback method called on success
     **/
    void onSuccess();

    /**
     * Callback method called on failure
     * @param throwable If available a throwable is send with with failure response.
     **/
    void onFailure(Throwable throwable);
}
