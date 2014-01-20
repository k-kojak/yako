package hu.rgai.android.eventlogger;

import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static hu.rgai.android.test.Constants.*;

public class LogToJsonConverter {

  private static final String EVENTDATAS_STR = "eventdatas";
  private static final String EVENTNAME_STR = "eventname";
  private static final String DATA_STR = "data";
  private String apiCode;
  private String deviceId = null;
  
  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  private String packageName;


  public LogToJsonConverter(String apiCode, String packageName) {
    this.apiCode = apiCode;
    this.packageName = packageName;
  }
  
  public String convertLogToJsonFormat( List<String> logList) {
    
    try {
      JSONObject record = new JSONObject();
      record.put( API_KEY_STR, apiCode);
      record.put( DEVICE_ID_STR, deviceId);
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
    JSONObject record = new JSONObject();

    StringTokenizer st = new StringTokenizer( log, " ");
    try {
      record.put( TIMESTAMP_STR, Long.getLong(st.nextToken()));
      JSONObject event = new JSONObject();
      String eventName = st.nextToken();

      JSONArray datasToEvent = new JSONArray();
      while ( st.hasMoreTokens()) {
        datasToEvent.put(st.nextToken());
      }
      event.put( EVENTNAME_STR, eventName);
      event.put( EVENTDATAS_STR, datasToEvent);
      record.put( DATA_STR, event.toString().replaceAll("\"", "\u0020"));
      recordsInRecord.put(event);
      
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    recordsInRecord.put( record );
  }
  
  public static long getCurrentTime() {
    return Calendar.getInstance().getTimeInMillis();
  }

}
