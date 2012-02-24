/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.geopaparazzi.library.network;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import eu.geopaparazzi.library.util.Base64;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * Network utils methods.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class NetworkUtilities {

    public static final int maxBufferSize = 4096;

    private static HttpURLConnection makeNewConnection( String fileUrl ) throws Exception {
        // boolean doHttps =
        // CorePlugin.getDefault().getPreferenceStore().getBoolean(KeyManager.keys().getHttpConnectionTypeKey());
        URL url = new URL(fileUrl);
        if (fileUrl.startsWith("https")) {
            HttpsURLConnection urlC = (HttpsURLConnection) url.openConnection();
            return urlC;
        } else {
            HttpURLConnection urlC = (HttpURLConnection) url.openConnection();
            return urlC;
        }
    }

    /**
    * Sends an HTTP GET request to a url
    *
    * @param urlStr - The URL of the server. (Example: " http://www.yahoo.com/search")
    * @param file the output file.
    * @param requestParameters - all the request parameters (Example: "param1=val1&param2=val2"). 
    *           Note: This method will add the question mark (?) to the request - 
    *           DO NOT add it yourself
    * @param user
    * @param password
    * @return - The response from the end point
     * @throws Exception 
    */
    public static void sendGetRequest4File( String urlStr, File file, String requestParameters, String user, String password )
            throws Exception {
        if (requestParameters != null && requestParameters.length() > 0) {
            urlStr += "?" + requestParameters;
        }
        HttpURLConnection conn = makeNewConnection(urlStr);
        conn.setRequestMethod("GET");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setChunkedStreamingMode(0);
        conn.setUseCaches(false);

        if (user != null && password != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(user);
            stringBuilder.append(":");
            stringBuilder.append(password);
            conn.setRequestProperty("Authorization", "Basic " + Base64.encode(stringBuilder.toString().getBytes()));
        }
        conn.connect();

        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = conn.getInputStream();
            out = new FileOutputStream(file);

            byte[] buffer = new byte[maxBufferSize];
            int bytesRead = in.read(buffer, 0, maxBufferSize);
            while( bytesRead > 0 ) {
                out.write(buffer, 0, bytesRead);
                bytesRead = in.read(buffer, 0, maxBufferSize);
            }
            out.flush();

        } finally {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        }
    }

    /**
     * Sends a string via POST to a given url.
     * 
     * @param urlStr the url to which to send to.
     * @param string the string to send as post body.
     * @param user the user or <code>null</code>.
     * @param password the password or <code>null</code>.
     * @return the response.
     * @throws Exception
     */
    public static String sendPost( String urlStr, String string, String user, String password ) throws Exception {
        BufferedOutputStream wr = null;
        HttpURLConnection conn = null;
        try {
            conn = makeNewConnection(urlStr);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setChunkedStreamingMode(0);
            conn.setUseCaches(false);
            if (user != null && password != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(user);
                stringBuilder.append(":");
                stringBuilder.append(password);
                conn.setRequestProperty("Authorization", "Basic " + Base64.encode(stringBuilder.toString().getBytes()));
            }
            conn.connect();

            // Make server believe we are form data...
            wr = new BufferedOutputStream(conn.getOutputStream());
            byte[] bytes = string.getBytes();
            wr.write(bytes);
            wr.flush();

            int responseCode = conn.getResponseCode();
            StringBuilder returnMessageBuilder = new StringBuilder();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                while( true ) {
                    String line = br.readLine();
                    if (line == null)
                        break;
                    returnMessageBuilder.append(line + "\n");
                }
                br.close();
            }

            return returnMessageBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

    public static String sendFilePost( String urlStr, File file, String user, String password ) throws Exception {
        BufferedOutputStream wr = null;
        FileInputStream fis = null;
        HttpURLConnection conn = null;
        try {
            // Authenticator.setDefault(new Authenticator(){
            // protected PasswordAuthentication getPasswordAuthentication() {
            // return new PasswordAuthentication("test", "test".toCharArray());
            // }
            // });
            conn = makeNewConnection(urlStr);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setChunkedStreamingMode(0);
            conn.setUseCaches(false);

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(user);
            stringBuilder.append(":");
            stringBuilder.append(password);
            conn.setRequestProperty("Authorization", "Basic " + Base64.encode(stringBuilder.toString().getBytes()));
            conn.connect();

            wr = new BufferedOutputStream(conn.getOutputStream());
            fis = new FileInputStream(file);
            int bytesAvailable = fis.available();
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            Logger.i("NETWORKUTILITIES", "BUFFER USED: " + bufferSize);
            byte[] buffer = new byte[bufferSize];
            int bytesRead = fis.read(buffer, 0, bufferSize);
            while( bytesRead > 0 ) {
                wr.write(buffer, 0, bufferSize);
                bytesAvailable = fis.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fis.read(buffer, 0, bufferSize);
            }
            wr.flush();

            String responseMessage = conn.getResponseMessage();
            return responseMessage;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (wr != null)
                wr.close();
            if (fis != null)
                fis.close();
            if (conn != null)
                conn.disconnect();
        }
    }

    /**
     * Checks is the network is available.
     * 
     * @param context the {@link Context}.
     * @return true if the network is available.
     */
    public static boolean isNetworkAvailable( Context context ) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for( int i = 0; i < info.length; i++ ) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}