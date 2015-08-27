package nl.sense_os.datastorageengine;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import nl.sense_os.datastorageengine.CommonSenseProxy;

/**
 * Created by ronald on 24-8-15.
 */
public class CSUtils {

    public static final String APP_KEY = "wRgE7HZvhDsRKaRm6YwC3ESpIqqtakeg";
    //creates random account on CommonSense.
    // @ return map{"email", email  ;  "username", <username>  ; "password" <password> }
    public static Map<String, String> createCSAccount() throws IOException {

        HttpURLConnection urlConnection = null;
        HashMap<String, String> response = new HashMap<String, String>();
        String urlString = "http://api.staging.sense-os.nl/users.json";

        JSONObject user = new JSONObject();
        JSONObject postData = new JSONObject();

        long now = System.currentTimeMillis();
        String username = "spam+"+now+"@sense-os.nl";
        String email = "spam+"+now+"@sense-os.nl";
        String password = "87f95196987d8c3bf339e2a52be957f4" ;
        String userid = "";

        try{
            user.put("username", username);
            user.put("email", email);
            user.put("password", password);
            postData.put("user", user);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            final boolean compress = true;
            // open new URL connection channel.
            URL url = new URL(urlString);
            if ("https".equals(url.getProtocol().toLowerCase(Locale.ENGLISH))) {
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                https.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                urlConnection = https;
            } else {
                urlConnection = (HttpURLConnection) url.openConnection();
            }
            // some parameters
            urlConnection.setUseCaches(false);
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestMethod("POST");

            urlConnection.setDoOutput(true);
                // When no charset is given in the Content-Type header "ISO-8859-1" should be
                // assumed (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7.1).
                // Because we're uploading UTF-8 the charset should be set to UTF-8.
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                // send content
            DataOutputStream printout;

            // do not set content size
                    // use chunked transfer mode instead
                urlConnection.setChunkedStreamingMode(0);
                urlConnection.setRequestProperty("Content-Encoding", "gzip");
                GZIPOutputStream zipStream = new GZIPOutputStream(
                            urlConnection.getOutputStream());
                    printout = new DataOutputStream(zipStream);

                // Write the string in UTF-8 encoding
                printout.write(postData.toString().getBytes("UTF-8"));
                printout.flush();
                printout.close();


        // get response, or read error message
            InputStream inputStream;
            try {
                inputStream = urlConnection.getInputStream();
            } catch (IOException e) {
                inputStream = urlConnection.getErrorStream();
            }
            if (null == inputStream) {
                throw new IOException("could not get InputStream");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 1024);
            String line;
            StringBuffer responseContent = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
                responseContent.append('\r');
            }
            response.put("RESPONSE_CONTENT", responseContent.toString());
            response.put("RESPONSE_CODE", Integer.toString(urlConnection.getResponseCode()));
            try {
                JSONObject userobj = new JSONObject(response.get("RESPONSE_CONTENT")).getJSONObject("user");
                userid = userobj.getString("id");
            }catch(JSONException e){
                return null;
            }
            // clean up
            reader.close();
            reader = null;
            inputStream.close();
            inputStream = null;

            // get headers
            Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
            String key, valueString;
            List<String> value;
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                key = entry.getKey();
                value = entry.getValue();
                if (null != key && null != value) {
                    key = key.toLowerCase(Locale.ENGLISH);
                    valueString = value.toString();
                    valueString = valueString.substring(1, valueString.length() - 1);
                    response.put(key, valueString);
                }
            }

            if(response.get("RESPONSE_CODE").equals("201")){
                HashMap<String, String> result = new HashMap<String, String>();
                result.put("username", username);
                result.put("email", email);
                result.put("password", password);
                result.put("id",userid);

                return result;
            }else{
                return null;
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (ProtocolException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

    }

    public static boolean deleteAccount(String userName, String passWord,String userId){

        //String appKey = "";

        CommonSenseProxy csProxy = new CommonSenseProxy(false,  APP_KEY);

        try {
            String sessionId = csProxy.loginUser(userName, passWord);
            String url = "http://api.staging.sense-os.nl/users/"+userId;
            Map<String, String> response = request(url, null, sessionId, "DELETE");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }


    /**
     * Performs request at CommonSense API. Returns the response code, content, and headers.
     *
     * @param urlString
     *            Complete URL to perform request to.
     * @param content
     *            (Optional) Content for the request. If the content is not null, the request method
     *            is automatically POST. The default method is GET.
     * @param sessionID
     *            (Optional) Session ID header for the request.
     * @param requestMethod
     *            The required method for the HTTP request, such as POST or GET.
     * @return Map with SenseApi.KEY_CONTENT and SenseApi.KEY_RESPONSE_CODE fields, plus fields for
     *         all response headers.
     * @throws IOException
     */

    public static Map<String, String> request( String urlString,
                                               JSONObject content, String sessionID, String requestMethod) throws IOException {

        HttpURLConnection urlConnection = null;
        HashMap<String, String> result = new HashMap<String, String>();
        try {
            final boolean compress = true;
            // open new URL connection channel.
            URL url = new URL(urlString);
            if ("https".equals(url.getProtocol().toLowerCase(Locale.ENGLISH))) {
                HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
                https.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                urlConnection = https;
            } else {
                urlConnection = (HttpURLConnection) url.openConnection();
            }
            // some parameters
            urlConnection.setUseCaches(false);
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setRequestProperty("Accept", "application/json");
            if(null != APP_KEY)
                urlConnection.setRequestProperty("APPLICATION-KEY", APP_KEY);

            if(sessionID != null)
                urlConnection.setRequestProperty("SESSION-ID", sessionID);

            if(requestMethod != null)
                urlConnection.setRequestMethod(requestMethod);

            // send content (if available)
            if (null != content) {
                urlConnection.setDoOutput(true);
                // When no charset is given in the Content-Type header "ISO-8859-1" should be
                // assumed (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7.1).
                // Because we're uploading UTF-8 the charset should be set to UTF-8.
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                // send content
                DataOutputStream printout;
                if (compress) {
                    // do not set content size
                    // use chunked transfer mode instead
                    urlConnection.setChunkedStreamingMode(0);
                    urlConnection.setRequestProperty("Content-Encoding", "gzip");
                    GZIPOutputStream zipStream = new GZIPOutputStream(
                            urlConnection.getOutputStream());
                    printout = new DataOutputStream(zipStream);
                } else {
                    // set content size
                    // The content length should be in bytes. We cannot use string length here
                    // because that counts the number of chars while not accounting for multibyte
                    // chars
                    int contentLength = content.toString().getBytes("UTF-8").length;
                    urlConnection.setFixedLengthStreamingMode(contentLength);
                    urlConnection.setRequestProperty("Content-Length", "" + contentLength);
                    printout = new DataOutputStream(urlConnection.getOutputStream());
                }
                // Write the string in UTF-8 encoding
                printout.write(content.toString().getBytes("UTF-8"));
                printout.flush();
                printout.close();
            }

            // get response, or read error message
            InputStream inputStream;
            try {
                inputStream = urlConnection.getInputStream();
            } catch (IOException e) {
                inputStream = urlConnection.getErrorStream();
            }
            if (null == inputStream) {
                throw new IOException("could not get InputStream");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 1024);
            String line;
            StringBuffer responseContent = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
                responseContent.append('\r');
            }
            result.put("RESPONSE_CONTENT", responseContent.toString());
            result.put("RESPONSE_CODE", Integer.toString(urlConnection.getResponseCode()));

            // clean up
            reader.close();
            reader = null;
            inputStream.close();
            inputStream = null;

            // get headers
            Map<String, List<String>> headerFields = urlConnection.getHeaderFields();
            String key, valueString;
            List<String> value;
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                key = entry.getKey();
                value = entry.getValue();
                if (null != key && null != value) {
                    key = key.toLowerCase(Locale.ENGLISH);
                    valueString = value.toString();
                    valueString = valueString.substring(1, valueString.length() - 1);
                    result.put(key, valueString);
                }
            }

            return result;

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

}
