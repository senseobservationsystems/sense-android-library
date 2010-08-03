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
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MsgHandler {

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

            final Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(MsgHandler.this.number));
            MsgHandler.this.number = "";
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            MsgHandler.this.context.startActivity(intent);
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
                    MsgHandler.this.number = "";
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
                            MsgHandler.this.number = inputLine.substring(index + 8, endIndex);
                        }
                        Log.d(TAG, "Calling number:" + MsgHandler.this.number);
                        out.println("Calling number:" + MsgHandler.this.number + "\n\n");
                        final StringBuilder buf = new StringBuilder("tel:");
                        buf.append(MsgHandler.this.number);
                        MsgHandler.this.number = buf.toString();
                        if (MsgHandler.this.url == null) {
                            return;
                        }
                        try {
                            final Vibrator vibrate = (Vibrator) MsgHandler.this.context
                                    .getSystemService(Context.VIBRATOR_SERVICE);
                            vibrate.vibrate(3000);
                            say("Calling number " + MsgHandler.this.number);
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
                            final AudioManager am = (AudioManager) MsgHandler.this.context
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
                MsgHandler.this.deviceService_id = _prefs.getString(key, "0");
            } else if (key.equals(PREF_KEY_DEVICE_TYPE)) {
                MsgHandler.this.deviceService_type = _prefs.getString(key, "phone_state_service");
            } else if (key.equals(PREF_KEY_PORT)) {
                MsgHandler.this.port = _prefs.getInt(key, 8080);
            } else if (key.equals(PREF_KEY_UPDATE)) {
                MsgHandler.this.updateFreq = _prefs.getInt(key, 0);
            } else if (key.equals(PREF_KEY_URL)) {
                MsgHandler.this.url = _prefs.getString(key,
                        "http://demo.almende.com:8080/relaytodeviceservice");
            }
        }
    };

    class SendFileThread implements Runnable {
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
            BufferedReader br = null;
            DataOutputStream dos = null;
            DataInputStream inStream = null;

            InputStream is = null;
            OutputStream os = null;
            boolean ret = false;

            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "----FormBoundary6bYQOdhfGEj4oCSv";

            int bytesRead, bytesAvailable, bufferSize;

            byte[] buffer;

            int maxBufferSize = 1 * 1024 * 1024;
            String responseFromServer = "";
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
                System.out.println("From ServletCom CLIENT REQUEST:" + ex);
            }

            catch (IOException ioe) {
                System.out.println("From ServletCom CLIENT REQUEST:" + ioe);
            }

            // ------------------ read the SERVER RESPONSE

            try {
                inStream = new DataInputStream(conn.getInputStream());
                String str;
                while ((str = inStream.readLine()) != null) {
                    System.out.println("Server response is: " + str);
                    System.out.println("");
                }
                inStream.close();

            } catch (IOException ioex) {
                System.out.println("From (ServerResponse): " + ioex);

            }
        }
    }
    class SendMessageThread implements Runnable {
        String cookie;
        Thread runner;
        String url;

        public SendMessageThread() {
        }

        public SendMessageThread(String _url, String Cookie) {
            url = _url;
            cookie = Cookie;
        }

        public void run() {
            try {
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
                    String sensor = URLDecoder.decode(this.url.substring(subStrStart, subStrEnd));
                    String outputString = "Sent " + sensor
                            + " data. Response from CommonSense: " + body;
                    Log.d(TAG, outputString);
                }
                
            } catch (Exception e) {
                System.out.println("error: " + e.getMessage());
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
    private static final String TAG = "Sense MsgHandler";
    private final Context context;
    private String deviceService_id;
    private String deviceService_type;
    private String httpResponse;
    // private long lastUpdate;
    private boolean listening;
    // private final HashMap<String, String> msgBuffer;
    // private boolean speaking = false;
    // private String lock = "locking";
    // private TextToSpeech mTts;
    // private MyInitListener mil;
    // private MyOnUtteranceCompletedListener mouc;
    private String number = "";
    private int port;
    // private boolean sending;
    private ServerSocket serverSocket;
    private final Runnable socketServer = new Runnable() {
        public void run() {
            Looper.prepare();
            try {
                MsgHandler.this.serverSocket = new ServerSocket(MsgHandler.this.port);

            } catch (final IOException e) {
                Log.d(TAG, "Could not listen on port: " + MsgHandler.this.port);
            }
            try {
                while (MsgHandler.this.listening) {
                    new MultiServerThread(MsgHandler.this.serverSocket.accept()).start();
                }
                MsgHandler.this.serverSocket.close();
            } catch (final Exception e) {
                Log.d(TAG, "Error while listening on socket server:", e);
            }
            Looper.loop();
        }
    };

    private int updateFreq;

    private String url;;

    // private void initTTS() {
    // mil = new MyInitListener();
    // mTts = new TextToSpeech(context, mil);
    // mouc = new MyOnUtteranceCompletedListener();
    // mTts.setLanguage(Locale.US);
    // mTts.setOnUtteranceCompletedListener(mouc);
    // }

    public MsgHandler(Context context) {

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

    // private class MyInitListener implements OnInitListener {
    //
    // public void onInit(int status) {
    //
    // }
    //
    // };

    // private class MyOnUtteranceCompletedListener implements OnUtteranceCompletedListener {
    //
    // public void onUtteranceCompleted(String utteranceId) {
    // synchronized (lock) {
    // speaking = false;
    // }
    // lock.notifyAll();
    // }
    // };

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

    /*
     * @deprecated
     * 
     * public void sendPhoneState(String stateName, String stateValue) { String url =
     * SenseSettings.URL_PHONESTATE_ADD
     * +"?stateName="+URLEncoder.encode(stateName)+"&stateValue="+URLEncoder.encode(stateValue);
     * final SharedPreferences prefs = context.getSharedPreferences(PRIVATE_PREFS,
     * android.content.Context.MODE_PRIVATE); String cookie =
     * prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, ""); new Thread(new SendMessageThread(url,
     * cookie)).start(); }
     */

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

    /*
     * @deprecated
     * 
     * public void sendPopQuizAnswer(String questionID, String answerID, String quizDate) { String
     * url =
     * SenseSettings.URL_QUIZ_SEND_ANSWER+"?questionId="+questionID+"&answerId="+answerID+"&quizDate="
     * +URLEncoder.encode(quizDate); final SharedPreferences prefs =
     * context.getSharedPreferences(PRIVATE_PREFS, android.content.Context.MODE_PRIVATE); String
     * cookie = prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, ""); new Thread(new
     * SendMessageThread(url, cookie)).start(); }
     */
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

    /*
     * @deprecated
     * 
     * public void sendPhoneLocation(String longitude, String latitude) { String url =
     * SenseSettings.URL_LOCATION_ADD+"?longitude="+longitude+"&latitude="+latitude; final
     * SharedPreferences prefs = context.getSharedPreferences(PRIVATE_PREFS,
     * android.content.Context.MODE_PRIVATE); String cookie =
     * prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, ""); new Thread(new SendMessageThread(url,
     * cookie)).start(); }
     */
    public void sendSensorData(String sensorName, Map<String, String> msg) {
        JSONObject json = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : msg.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
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
        String url = SenseSettings.URL_SEND_SENSOR_DATA + "?sensorName="
                + URLEncoder.encode(sensorName) + "&sensorValue=" + URLEncoder.encode(sensorValue)
                + "&sensorDataType=" + URLEncoder.encode(dataType);
        final SharedPreferences prefs = context.getSharedPreferences(PRIVATE_PREFS,
                android.content.Context.MODE_PRIVATE);
        String cookie = prefs.getString(SenseSettings.PREF_LOGIN_COOKIE, "");
        new Thread(new SendMessageThread(url, cookie)).start();
    }

    public void sendSensorData(String sensorName, String sensorValue, String dataType,
            String deviceType) {
        String url = SenseSettings.URL_SEND_SENSOR_DATA + "?sensorName="
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
        BufferedReader br = null;
        DataOutputStream dos = null;
        DataInputStream inStream = null;

        InputStream is = null;
        OutputStream os = null;
        boolean ret = false;

        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "--FormBoundary6bYQOdhfGEj4oCSv";

        int bytesRead, bufferSize; // bytesAvailable;

        byte[] buffer;

        // int maxBufferSize = 1*1024*1024;
        String responseFromServer = "";
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
            System.out.println("From ServletCom CLIENT REQUEST:" + ex);
        }

        catch (IOException ioe) {
            System.out.println("From ServletCom CLIENT REQUEST:" + ioe);
        }

        // ------------------ read the SERVER RESPONSE

        try {
            inStream = new DataInputStream(conn.getInputStream());
            String str;
            while ((str = inStream.readLine()) != null) {
                System.out.println("Server response is: " + str);
                System.out.println("");
            }
            inStream.close();

        } catch (IOException ioex) {
            System.out.println("From (ServerResponse): " + ioex);

        }
    }
}
