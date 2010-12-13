/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service.popquiz;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Answer {
    private static final String TAG = "QuizAnswer";
    public int id;
    public String value;

    public Answer(JSONObject json) {
        try {
            this.id = Integer.parseInt(json.getString("answer_id"));
            this.value = json.getString("answer_value");
        } catch (final JSONException e) {
            Log.e(TAG, "JSONException parsing aan answer:", e);
        }
    }
    
    public Answer() {
        
    }
    
    @Override
    public String toString() {
        return id + ") " + value;
    }
}
