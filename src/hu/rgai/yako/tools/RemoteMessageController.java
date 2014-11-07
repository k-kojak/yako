package hu.rgai.yako.tools;

import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class RemoteMessageController {

  private static final String SERVICE_URL = "http://rgai.inf.u-szeged.hu/medicalrecords/index.php";

  public static HttpResponse sendPostRequest(Map<String, String> params) {
    HttpPost httppost = new HttpPost(SERVICE_URL);
    List<NameValuePair> prms = new LinkedList<NameValuePair>();
    for (Map.Entry<String, String> e : params.entrySet()) {
      prms.add(new BasicNameValuePair(e.getKey(), e.getValue()));
    }
    try {
      HttpEntity e = new UrlEncodedFormEntity(prms, "UTF-8");
      httppost.setEntity(e);
    } catch (UnsupportedEncodingException e) {
      return null;
    }
    try {
      HttpClient httpclient = new DefaultHttpClient();
      return httpclient.execute(httppost);
    } catch (IOException e) {
      Log.d("yako", "", e);
    }
    return null;
  }

  public static String responseToString(HttpResponse response) {
    if (response == null) {
      return null;
    }

    HttpEntity entity = response.getEntity();
    if (entity != null) {
      InputStream instream;
      try {
        instream = entity.getContent();
        try {
          StringBuilder sb = new StringBuilder();
          String line;
          BufferedReader rd = new BufferedReader(new InputStreamReader(instream));
          while ( (line = rd.readLine()) != null ) {
            sb.append(line);
          }
          rd.close();

          return sb.toString();
        } catch(Exception e) {
          Log.d("yako", "", e);
        }
      } catch(Exception e) {
        Log.d("yako", "", e);
      }
    }
    return null;

  }

}
