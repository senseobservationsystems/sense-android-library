package nl.sense_os.datastorageengine;

import android.util.Log;

import org.apache.http.client.HttpResponseException;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

public class HTTPUtil {
    private static final String TAG = "HTTPUtil";

    // FIXME: set COMPRESS true again as soon as the backend supports it
    protected static boolean COMPRESS = false;

    /**
     * Perform an HTTP request to the Sensor data API.
     *
     * @param method The request method, such as POST or GET.
     * @param url Complete URL to perform request to.
     * @param headers Map with headers. Optional.
     * @param body Content for the request. Optional.
     * @throws IOException
     *            IOException is thrown when the inputStream has errors.
     * @return Returns a Response containing response code, body, and response headers.
     */
    public static Response request(String method, URL url, Map<String, String> headers, String body) throws IOException {
        Log.d(TAG, "request method=" + method + ", url=" + url + ", body=\n" + body);
        HttpURLConnection urlConnection = null;

        // TODO: use a library like https://github.com/kevinsawicki/http-request instead of our own baked request method

        try {
            // open new URL connection channel.
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

            // set request method
            if(method != null) {
                urlConnection.setRequestMethod(method.toUpperCase());
            }

            // set headers
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // send content
            if (body != null) {
                urlConnection.setDoOutput(true);
                // When no charset is given in the Content-Type header "ISO-8859-1" should be
                // assumed (see http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7.1).
                // Because we're uploading UTF-8 the charset should be set to UTF-8.
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                // send content
                DataOutputStream printout;
                if (COMPRESS) {
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
                    int contentLength = body.getBytes("UTF-8").length;
                    urlConnection.setFixedLengthStreamingMode(contentLength);
                    urlConnection.setRequestProperty("Content-Length", "" + contentLength);
                    printout = new DataOutputStream(urlConnection.getOutputStream());
                }
                // Write the string in UTF-8 encoding
                printout.write(body.getBytes("UTF-8"));
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
            StringBuffer responseBody = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                responseBody.append(line);
                responseBody.append('\r');
            }

            // clean up
            reader.close();
            reader = null;
            inputStream.close();
            inputStream = null;

            // validate response code, throw an exception when not 200
            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "response code=" + responseCode + ", body length=" + responseBody.length());
            Log.d(TAG, "response body=\n" + responseBody.toString());
            if (responseCode != 200) {
                try {
                    // we expect the body to be a JSONObject like {"code": number, "reason": string}
                    JSONObject errorBody = new JSONObject(responseBody.toString());
                    throw new HttpResponseException(
                            errorBody.optInt("code", responseCode),
                            errorBody.getString("reason"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                throw new HttpResponseException(responseCode, responseBody.toString());
            }

            // create the response
            Response response = new Response();
            response.code = urlConnection.getResponseCode();
            response.headers = urlConnection.getHeaderFields();
            response.body = responseBody.toString();
            return response;

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Helper class containing the code, response body, and headers of an HTTP request.
     * Contains helper methods to parse the response body as a JSONObject or JSONArray.
     */
    public static class Response {
        public Integer code = null;
        public String body = null;
        public Map<String, List<String>> headers = new HashMap<>();

        public JSONObject toJSONObject() throws JSONException {
            Log.d("body", body);
            return new JSONObject(body);
        }

        public JSONArray toJSONArray() throws JSONException {
            return new JSONArray(body);
        }
    }

    /**
     * Helper function to encode an URL parameter
     * @param param
     * @return Returns the url encoded parameter
     * @throws UnsupportedEncodingException
     */
    public static String encode (String param) throws UnsupportedEncodingException {
        if (param != null) {
            return URLEncoder.encode(param, "UTF-8");
        }
        else {
            return "";
        }
    }
}
