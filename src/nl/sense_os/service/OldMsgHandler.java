/*
 ************************************************************************************************************
 *     Copyright (C)  2010 Sense Observation Systems, Rotterdam, the Netherlands.  All rights reserved.     *
 ************************************************************************************************************
 */
package nl.sense_os.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;

import nl.sense_os.app.SenseSettings;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OldMsgHandler {

    /**
     * Thread to call a specified number.
     */
    private class CallThread implements Runnable {
        public void run() {
            // try {
            // synchronized (lock)
            // {
            // while(speaking)
            // lock.wait();
            // Log.d(TAG, "Out of lock");
            // }
            // } catch (InterruptedException e) {
            // Log.d(TAG, "InterruptedException in CallThread!", e);
            // }

            final Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(OldMsgHandler.this.number));
            OldMsgHandler.this.number = "";
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            OldMsgHandler.this.context.startActivity(intent);
        }
    }

    /**
     * Thread that listens to input on a socket, performing Text-to-speech or call actions
     * accordingly.
     */
    public class MultiServerThread extends Thread {
        private Socket socket = null;

        public MultiServerThread(Socket socket) {
            super("MultiServerThread");
            this.socket = socket;
        }

        @Override
        public void run() {
            Looper.prepare();
            try {
                final PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);
                final BufferedReader in = new BufferedReader(new InputStreamReader(
                        this.socket.getInputStream()));

                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    int index = 0;
                    OldMsgHandler.this.number = "";
                    inputLine = java.net.URLDecoder.decode(inputLine, "UTF-8");
                    if ((index = inputLine.indexOf("action=say")) != -1) {
                        if ((index = inputLine.indexOf("msg=\"", index)) != -1) {
                            final int endIndex = inputLine.indexOf("\"", index + 5);
                            final String text = inputLine.substring(index + 5, endIndex);
                            say(text);
                        }
                    }
                    if ((index = inputLine.indexOf("action=call")) != -1) {
                        if ((index = inputLine.indexOf("number=\"")) != -1) {
                            final int endIndex = inputLine.indexOf("\"", index + 8);
                            OldMsgHandler.this.number = inputLine.substring(index + 8, endIndex);
                        }
                        Log.d(TAG, "Calling number:" + OldMsgHandler.this.number);
                        out.println("Calling number:" + OldMsgHandler.this.number + "\n\n");
                        final StringBuilder buf = new StringBuilder("tel:");
                        buf.append(OldMsgHandler.this.number);
                        OldMsgHandler.this.number = buf.toString();
                        if (OldMsgHandler.this.url == null) {
                            return;
                        }
                        try {
                            final Vibrator vibrate = (Vibrator) OldMsgHandler.this.context
                                    .getSystemService(Context.VIBRATOR_SERVICE);
                            vibrate.vibrate(3000);
                            say("Calling number " + OldMsgHandler.this.number);
                            // try {
                            // synchronized (lock)
                            // {
                            // while(speaking)
                            // lock.wait(10);
                            // Log.d(TAG, "Out of lock");
                            // }
                            // } catch (InterruptedException e) {
                            // Log.e(TAG, "InterruptedException in MultiServerThread!", e);
                            // }
                            final CallThread callThread = new CallThread();
                            new Thread(callThread).start();
                            final AudioManager am = (AudioManager) OldMsgHandler.this.context
                                    .getSystemService(Context.AUDIO_SERVICE);

                            am.setSpeakerphoneOn(true);
                            // am.setRouting(am.MODE_IN_CALL, am.ROUTE_SPEAKER , am.ROUTE_ALL);

                        } catch (final Exception e) {
                            e.printStackTrace();
                            Log.d(TAG, "Error calling");
                        }
                    }
                    break;
                }
                out.close();
                in.close();
                this.socket.close();

            } catch (final IOException e) {
                e.printStackTrace();
            }
            Looper.loop();
        }
    }

    /**
     * Listener for the MsgHandler's preferences. Updates the fields for this instance when the
     * preference changes.
     */
    private class MyPrefListener implements OnSharedPreferenceChangeListener {

        public void onSharedPreferenceChanged(SharedPreferences _prefs, String key) {
            if (key.equals(PREF_KEY_DEVICE_ID)) {
                OldMsgHandler.this.deviceService_id = _prefs.getString(key, "0");
            } else if (key.equals(PREF_KEY_DEVICE_TYPE)) {
                OldMsgHandler.this.deviceService_type = _prefs.getString(key, "phone_state_service");
            } else if (key.equals(PREF_KEY_PORT)) {
                OldMsgHandler.this.port = _prefs.getInt(key, 8080);
            } else if (key.equals(PREF_KEY_UPDATE)) {
                OldMsgHandler.this.updateFreq = _prefs.getInt(key, 0);
            } else if (key.equals(PREF_KEY_URL)) {
                OldMsgHandler.this.url = _prefs.getString(key,
                        "http://demo.almende.com:8080/relaytodeviceservice");
            }
        }
    };

    @SuppressWarnings("unused")
    private class SendFileThread implements Runnable {
        String cookie;
        String exsistingFileName;
        String sensorName;

        public SendFileThread(String _sensorName, String path, String _cookie) {
            exsistingFileName = path;
            sensorName = _sensorName;
            cookie = _cookie;
        }

        public void run() {
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            DataInputStream inStream = null;

            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "----FormBoundary6bYQOdhfGEj4oCSv";

            int bytesRead, bytesAvailable, bufferSize;

            byte[] buffer;

            int maxBufferSize = 1 * 1024 * 1024;
            String urlString = SenseSettings.URL_SEND_SENSOR_DATA_FILE;

            try {
                // ------------------ CLIENT REQUEST

                FileInputStream fileInputStream = new FileInputStream(new File(exsistingFileName));

                // open a URL connection to the Servlet
                URL url = new URL(urlString);

                // Open a HTTP connection to the URL
                conn = (HttpURLConnection) url.openConnection();

                // Allow Inputs
                conn.setDoInput(true);

                // Allow Outputs
                conn.setDoOutput(true);

                // Don't use a cached copy.
                conn.setUseCaches(false);

                // Use a post method.
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Cookie", cookie);
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; sensorName=\"" + sensorName + "\";"
                        + " filename=\"" + exsistingFileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);
                // create a buffer of maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...

                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                // send multipart form data necesssary after file data...

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // close streams

                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {
                Log.e(TAG, "MalformedURLException uploading file:", ex);
            } catch (IOException ioe) {
                Log.e(TAG, "IOException uploading file:", ioe);
            }

            // ------------------ read the SERVER RESPONSE

            try {
                inStream = new DataInputStream(conn.getInputStream());
                String str;
                boolean sendOK = false;
                while ((str = inStream.readLine()) != null) 
                {
                	 if(str.toLowerCase().contains("ok"))
                		 sendOK = true;                	
                    Log.d(TAG, "Uploaded file... Server response is: " + str);
                }
                if(!sendOK)
            	{
            		Log.d(TAG, "Error sending message, re-login");
            		((SenseService) context).senseServiceLogin();
            	}
                inStream.close();

            } catch (IOException ioex) {
                Log.e(TAG, "IOException reading server response after file upload:", ioex);
            }
        }
    }

    private class SendMessageThread implements Runnable {
        String cookie;
        String url;

        @SuppressWarnings("unused")
        public SendMessageThread() {

        }

        public SendMessageThread(String _url, String Cookie) {
            url = _url;
            cookie = Cookie;
        }

        public void run() {
            try {

                if (nrOfSendMessageThreads < MAX_NR_OF_SEND_Message_THREADS) {
                    ++nrOfSendMessageThreads;
                    URI uri = new URI(url);
                    HttpPost post = new HttpPost(uri);
                    post.setHeader("Cookie", cookie);
                    HttpClient client = new DefaultHttpClient();
                    client.getConnectionManager().closeIdleConnections(2, TimeUnit.SECONDS);
                    HttpResponse response = client.execute(post);

                    if (response != null) {
                        Header header[] = response.getAllHeaders();
                        String output = "";
                        for (Header element : header) {
                            output += element.getName() + "=" + element.getValue() + "\n";
                        }

                        int value = 0;
                        String body = "";
                        InputStream ir = response.getEntity().getContent();

                        while ((value = ir.read()) != -1) {
                            body += "" + (char) value;
                        }

                        int subStrStart = this.url.indexOf("sensorName=") + 11;
                        int subStrEnd = this.url.indexOf("&", subStrStart);
                        String sensor = URLDecoder.decode(this.url
                                .substring(subStrStart, subStrEnd));
                        String outputString = "Sent " + sensor
                                + " data. Response from CommonSense: " + body;
                        Log.d(TAG, outputString);
                        if(!body.toLowerCase().contains("ok"))
						{
							Log.d(TAG, "Error sending message, re-login");
							((SenseService) context).senseServiceLogin();
						}
                        --nrOfSendMessageThreads;
                    }
                }

            } catch (IOException e) {
                --nrOfSendMessageThreads;
                Log.e(TAG, "IOException in SendMessageThread: " + e.getMessage());
            } catch (URISyntaxException e) {
                --nrOfSendMessageThreads;
                Log.e(TAG, "URISyntaxException in SendMessageThread: " + e.getMessage());
            }
        }
    }

    public static final String PREF_KEY_DEVICE_ID = "DeviceId";
    public static final String PREF_KEY_DEVICE_TYPE = "DeviceType";
    public static final String PREF_KEY_PORT = "Port";
    public static final String PREF_KEY_UPDATE = "UpdateFreq";
    public static final String PREF_KEY_URL = "Url";
    public static final String PREF_MSG_HANDLER = "prefMsgHandler";
    public static final String PRIVATE_PREFS = SenseSettings.PRIVATE_PREFS;
    private static final String TAG = "Sense MsgHandler (OLD!)";
    private final Context context;
    private String deviceService_id;
    private String deviceService_type;
    private String httpResponse;
    private int nrOfSendMessageThreads = 0;
    private int MAX_NR_OF_SEND_Message_THREADS = 50;
    private boolean listening;
    private String number = "";
    private int port;
    private ServerSocket serverSocket;
    private final Runnable socketServer = new Runnable() {
        public void run() {
            Looper.prepare();
            try {
                OldMsgHandler.this.serverSocket = new ServerSocket(OldMsgHandler.this.port);

            } catch (final IOException e) {
                Log.d(TAG, "Could not listen on port: " + OldMsgHandler.this.port);
            }
            try {
                while (OldMsgHandler.this.listening) {
                    new MultiServerThread(OldMsgHandler.this.serverSocket.accept()).start();
                }
                OldMsgHandler.this.serverSocket.close();
            } catch (final Exception e) {
                Log.d(TAG, "Error while listening on socket server:", e);
            }
            Looper.loop();
        }
    };
    private int updateFreq;
    private String url;;


    public OldMsgHandler(Context context) {

        this.context = context;

        // get initialization data from preferences
        final SharedPreferences prefs = context.getSharedPreferences(PREF_MSG_HANDLER,
                Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(new MyPrefListener());
        this.deviceService_id = prefs.getString(PREF_KEY_DEVICE_ID, "0");
        this.deviceService_type = prefs.getString(PREF_KEY_DEVICE_TYPE, "phone_state_service");
        this.port = prefs.getInt(PREF_KEY_PORT, 8080);
        this.updateFreq = prefs.getInt(PREF_KEY_UPDATE, 0);
        this.url = prefs.getString(PREF_KEY_URL,
                "http://demo.almende.com/commonSense/sp_state_add.php");

        // this.lastUpdate = -1;
        this.listening = false;
        // this.msgBuffer = new HashMap<String, String>();
        // this.sending = false;

        listen(true);
    }

    public String getDeviceService_id() {
        return this.deviceService_id;
    }

    public String getDeviceService_type() {
        return this.deviceService_type;
    }

    public String getHttpResponse() {
        return this.httpResponse;
    }

    public int getUpdateFreq() {
        return this.updateFreq;
    }

    public String getUrl() {
        return this.url;
    }

    public void listen(boolean _listen) {
        this.listening = _listen;
        if (_listen) {
            new Thread(this.socketServer).start();
        }
    }

    private void say(String text) {
        // synchronized (lock) {
        // speaking = true;
        // }
        // mTts.speak(text, TextToSpeech.QUEUE_ADD, null);
    }

    public void sendAddPopQuizAnswer(String answer) {
        String url = SenseSettings.URL_QUIZ_ADD_ANSWER + "?answer=" + URLEncoder.encode(answer);
        final SharedPreferences prefs = context.getSharedPreferences(PRIVATE_PREFS,
                android.content.Context.MODE_PRIVATE);
        String cookie = prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, "");
        new Thread(new SendMessageThread(url, cookie)).start();
    }

    public void sendAddPopQuizQuestion(String question) {
        String url = SenseSettings.URL_QUIZ_ADD_QUESTION + "?question="
                + URLEncoder.encode(question);
        final SharedPreferences prefs = context.getSharedPreferences(PRIVATE_PREFS,
                android.content.Context.MODE_PRIVATE);
        String cookie = prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, "");
        new Thread(new SendMessageThread(url, cookie)).start();
    }

    public void sendSensorData(String sensorName, Map<String, Object> msg) {
        JSONObject json = new JSONObject();
        try {
            for (Map.Entry<String, Object> entry : msg.entrySet()) {
                if (entry.getValue() instanceof String) {
                    json.put(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Integer) {
                    json.put(entry.getKey(), (Integer) entry.getValue());
                } else if (entry.getValue() instanceof Float) {
                    json.put(entry.getKey(), (Float) entry.getValue());
                } else if (entry.getValue() instanceof Double) {
                    json.put(entry.getKey(), (Double) entry.getValue());
                } else {
                    Log.d(TAG, "Unexpected sensed property: " + entry.getValue());
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONException preparing sensor data sending:", e);
        }
        sendSensorData(sensorName, json.toString(), SenseSettings.SENSOR_DATA_TYPE_JSON);
    }

    /*
     * Send sensordata with sensor name value and the type of sensor data
     */
    public void sendSensorData(String sensorName, String sensorValue, String dataType) {
        String url = SenseSettings.URL_SEND_BATCH_DATA + "?sensorName="
                + URLEncoder.encode(sensorName) + "&sensorValue=" + URLEncoder.encode(sensorValue)
                + "&sensorDataType=" + URLEncoder.encode(dataType);
        final SharedPreferences prefs = context.getSharedPreferences(PRIVATE_PREFS,
                android.content.Context.MODE_PRIVATE);
        String cookie = prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, "");
        new Thread(new SendMessageThread(url, cookie)).start();
    }

    public void sendSensorData(String sensorName, String sensorValue, String dataType,
            String deviceType) {
        String url = SenseSettings.URL_SEND_BATCH_DATA + "?sensorName="
                + URLEncoder.encode(sensorName) + "&sensorValue=" + URLEncoder.encode(sensorValue)
                + "&sensorDataType=" + URLEncoder.encode(dataType) + "&sensorDeviceType="
                + URLEncoder.encode(deviceType);
        final SharedPreferences prefs = context.getSharedPreferences(PRIVATE_PREFS,
                android.content.Context.MODE_PRIVATE);
        String cookie = prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, "");
        new Thread(new SendMessageThread(url, cookie)).start();
    }

    public void uploadFile(String sensorName, String path) {
        final SharedPreferences prefs = context.getSharedPreferences(PRIVATE_PREFS,
                android.content.Context.MODE_PRIVATE);
        String cookie = prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, "");

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        DataInputStream inStream = null;

        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "--FormBoundary6bYQOdhfGEj4oCSv";

        int bytesRead, bufferSize; // bytesAvailable;

        byte[] buffer;

        // int maxBufferSize = 1*1024*1024;
        // String responseFromServer = "";
        String urlString = SenseSettings.URL_SEND_SENSOR_DATA_FILE;

        try {
            // ------------------ CLIENT REQUEST

            File file = new File(path);
            String fileName = file.getName();
            FileInputStream fileInputStream = new FileInputStream(file);

            // open a URL connection to the Servlet
            URL url = new URL(urlString);

            // Open a HTTP connection to the URL
            conn = (HttpURLConnection) url.openConnection();

            // Allow Inputs
            conn.setDoInput(true);

            // Allow Outputs
            conn.setDoOutput(true);

            // Don't use a cached copy.
            conn.setUseCaches(false);

            // Use a post method.
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Cookie", cookie);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            dos = new DataOutputStream(conn.getOutputStream());

            // write sensorName
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"sensorName\";" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(sensorName);
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + lineEnd);

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\";" + " filename=\""
                    + fileName + "\"" + lineEnd);
            dos.writeBytes(lineEnd);
            // create a buffer of maximum size
            // bytesAvailable = fileInputStream.available();
            bufferSize = fileInputStream.available();// Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                // bytesAvailable = fileInputStream.available();
                bufferSize = fileInputStream.available();// Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // close streams
            fileInputStream.close();
            dos.flush();
            dos.close();

        } catch (MalformedURLException ex) {
            Log.e(TAG, "MalformedURLException uploading file:", ex);
        } catch (IOException ioe) {
            Log.e(TAG, "IOException uploading file:", ioe);
        }

        // ------------------ read the SERVER RESPONSE

        try {
            inStream = new DataInputStream(conn.getInputStream());
            String str;
            while ((str = inStream.readLine()) != null) {
                Log.d(TAG, "Uploaded file. Server response is: " + str);
            }
            inStream.close();
        } catch (IOException ioex) {
            Log.e(TAG, "IOException reading server response from inputstream:", ioex);
        }
    }
}
