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

public class Question {
    private static final String TAG = "QuizQuestion";
    Vector<Answer> answers;
    int id;
    String value;

    public Question(JSONObject question) {
        try {
            this.id = Integer.parseInt(question.getString("question_id"));
            this.value = question.getString("question_value");
            final JSONArray answerList = question.getJSONArray("answer");
            this.answers = new Vector<Answer>();
            for (int i = 0; i < answerList.length(); i++) {
                final JSONObject answer = answerList.getJSONObject(i);
                this.answers.add(new Answer(answer));
            }
        } catch (final JSONException e) {
            Log.e(TAG, "JSONException parsing a question:", e);
        }
    }

    public Question() {
        this.answers = new Vector<Answer>();
        this.id = -1;
        this.value = "ERROR";
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Question ");
        sb.append(this.id).append(": ");
        sb.append(this.value);
        for (final Answer answer : this.answers) {
            sb.append("\n  Answer ").append(answer.toString());
        }

        return sb.toString();
    }
}
