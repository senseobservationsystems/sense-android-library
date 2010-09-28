/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service.popquiz;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

public class Quiz {
    private static final String TAG = "Quiz";
    public String description;
    public int id;
    public Vector<Question> questions;

    public Quiz(JSONObject quiz) {
        try {
            this.id = quiz.getInt("quiz_id");
            this.description = quiz.getString("quiz_value");
            final JSONArray questionList = quiz.getJSONArray("answer");
            this.questions = new Vector<Question>();
            for (int i = 0; i < questionList.length(); i++) {
                final JSONObject question = questionList.getJSONObject(i);
                this.questions.add(new Question(question));
            }
        } catch (final JSONException e) {
            Log.e(TAG, "JSONException parsing quiz", e);
        }
    }

    public Quiz() {
        this.questions = new Vector<Question>();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("== Quiz ==\n");
        sb.append(this.id).append(": ");
        sb.append(this.description). append("\n");
        for (final Question question : this.questions) {
            sb.append(question.toString()).append("\n");
        }

        return sb.toString();
    }
}
