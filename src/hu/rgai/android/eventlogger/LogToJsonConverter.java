package hu.rgai.android.eventlogger;

import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.provider.Settings.Secure;

import static hu.rgai.android.test.Constants.*;

public class LogToJsonConverter {

  private String apiCode;
  //private static String deviceId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);;
  private String packageName;


  public LogToJsonConverter(String apiCode, String packageName) {
    this.apiCode = apiCode;
    this.packageName = packageName;
  }
  
  public String convertLogToJsonFormat( List<String> logList) {
    
    try {
      JSONObject record = new JSONObject();
      record.put( API_KEY_STR, apiCode);
      //record.put( DEVICE_ID_STR, deviceId);
      record.put( PACKAGE_STR, packageName);
      JSONArray recordsInRecord = new JSONArray();
      for (String log : logList) {
        addRecordsToRecord(recordsInRecord, log);
      }
      
      return record.toString();
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  private void addRecordsToRecord(JSONArray recordsInRecord, String log) {
    JSONObject timeStamp = new JSONObject();
    StringTokenizer st = new StringTokenizer( log, " ");
    try {
      timeStamp.put( TIMESTAMP_STR, Long.getLong(st.nextToken()));
      recordsInRecord.put(timeStamp);
      JSONObject event = new JSONObject();
      String eventName = st.nextToken();

      JSONArray datasToEvent = new JSONArray();
      
      while ( st.hasMoreTokens()) {
        datasToEvent.put(st.nextToken());
      }
      event.put(eventName, datasToEvent);
      recordsInRecord.put(event);
      
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    recordsInRecord.put( timeStamp );
  }
  
  public static long getCurrentTime() {
    return Calendar.getInstance().getTimeInMillis();
  }

}
