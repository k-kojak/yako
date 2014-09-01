package hu.rgai.yako.eventlogger;

import static hu.rgai.yako.eventlogger.Constants.API_KEY_STR;
import static hu.rgai.yako.eventlogger.Constants.DEVICE_ID_STR;
import static hu.rgai.yako.eventlogger.Constants.PACKAGE_STR;
import static hu.rgai.yako.eventlogger.Constants.TIMESTAMP_STR;

import java.util.Calendar;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class LogToJsonConverter {

  private static final String RECORDS_STR = "records";
  private static final String EVENTDATAS_STR = "eventdatas";
  private static final String EVENTNAME_STR = "eventname";
  private static final String DATA_STR = "data";
  private final String apiCode;
  private String deviceId = null;

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId( String deviceId) {
    this.deviceId = deviceId;
  }

  private final String packageName;

  public LogToJsonConverter(String apiCode, String packageName) {
    this.apiCode = apiCode;
    this.packageName = packageName;
  }

  public String convertLogToJsonFormat(List<String> logList) {

    try {
      JSONObject record = new JSONObject();
      JSONArray recordsInRecord = new JSONArray();
      for (String log : logList) {
        addRecordsToRecord(recordsInRecord, log);
      }
      record.put(API_KEY_STR, apiCode);
      record.put(DEVICE_ID_STR, deviceId);
      record.put(PACKAGE_STR, packageName);
      record.put(RECORDS_STR, recordsInRecord);
      return record.toString();
    } catch (JSONException e) {
      Log.d("willrgai", "", e);
      return null;
    }
  }

  private void addRecordsToRecord(JSONArray recordsInRecord, String log) {
    JSONObject record = new JSONObject();

    StringTokenizer st = new StringTokenizer(log, " ");
    try {
      Long timeStamp = Long.valueOf(st.nextToken());

      JSONObject event = new JSONObject();
      String eventName = st.nextToken();

      JSONArray datasToEvent = new JSONArray();
      if (st.hasMoreTokens())
        datasToEvent.put(st.nextToken(""));
      event.put(EVENTDATAS_STR, datasToEvent);
      event.put(EVENTNAME_STR, eventName);
      record.put(TIMESTAMP_STR, timeStamp);

      record.put(DATA_STR, event.toString());
      recordsInRecord.put(record);
    } catch (NoSuchElementException e) {
      Log.d("willrgai", "nincs ilyen elem", e);
    } catch (JSONException e) {
      Log.d("willrgai", "json hiba", e);
    }
  }

  public static long getCurrentTime() {
    return Calendar.getInstance().getTimeInMillis();
  }

}
