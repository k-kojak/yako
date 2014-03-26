package hu.rgai.android.eventlogger;

import static hu.rgai.android.test.Constants.SPACE_STR;
import static hu.rgai.android.test.Constants.apiCodeToAI;
import static hu.rgai.android.test.Constants.appPackageName;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.apache.commons.lang3.StringEscapeUtils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.Log;

public enum EventLogger {
  INSTANCE;

  private volatile BufferedWriter bufferedWriter;
  private boolean logFileOpen = false;
  String logFilePath;
  long logfileCreatedTime;

  private String appVersion = null;

  LogToJsonConverter logToJsonConverter = new LogToJsonConverter(apiCodeToAI, appPackageName);

  public long getLogfileCreatedTime() {
    return logfileCreatedTime;
  }

  private EventLogger() {
  }

  private final ArrayList<String> tempBufferToUpload = new ArrayList<String>();
  boolean lockedToUpload = false;
  private Context context = null;
  Thread actUploaderThread = null;
  boolean sdCard = false;

  public synchronized boolean openLogFile(String logFilePath, boolean isFullPath) {

    File logfile = null;
    if (isFullPath) {
      logfile = new File(logFilePath);
    } else {
      if (isSdPresent()) {
        sdCard = true;
        logfile = new File(Environment.getExternalStorageDirectory().getAbsoluteFile(), logFilePath);
      } else {
        logfile = new File(logFilePath);
      }
    }
    this.logFilePath = logfile.getPath();
    if (logfile.exists()) {
      try {
        if (!sdCard)
          bufferedWriter = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(logFilePath, Context.MODE_APPEND)));
        else
          bufferedWriter = new BufferedWriter(new FileWriter(logfile, true));
        logfileCreatedTime = getLogFileCreateDate();
      } catch (NumberFormatException e) {
        try {
          bufferedWriter.close();
        } catch (IOException e1) {
          e1.printStackTrace();
        }
        lockedToUpload = true;
        deleteLogFileAndCreateNew();
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      try {
        if (!sdCard)
          bufferedWriter = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(logFilePath, Context.MODE_PRIVATE)));
        else
          bufferedWriter = new BufferedWriter(new FileWriter(logfile));
        logfileCreatedTime = LogToJsonConverter.getCurrentTime();
        bufferedWriter.write(Long.toString(logfileCreatedTime));
        bufferedWriter.newLine();
        bufferedWriter.flush();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }
    logFileOpen = true;
    return true;
  }

  public String getAppVersion() {
    if (appVersion == null) {
      PackageInfo pInfo;
      try {
        pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        appVersion = pInfo.versionName;
      } catch (NameNotFoundException e) {
        e.printStackTrace();
        appVersion = "not_checked_version";
      }
    }
    return appVersion;
  }

  public boolean isLogFileOpen() {
    return logFileOpen;
  }

  public synchronized boolean closeLogFile() {
    try {
      bufferedWriter.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return true;
  }

  public synchronized void writeToLogFile(String log, boolean logTimeStamp) {
    if (logTimeStamp) {
      writeFormatedLogToLogFile(LogToJsonConverter.getCurrentTime() + SPACE_STR + log);
    } else {
      writeFormatedLogToLogFile(log);
    }
  }

  private void writeFormatedLogToLogFile(String log) {
    Log.d("willrgai", log);
    if (!lockedToUpload) {
      try {
        bufferedWriter.write(StringEscapeUtils.escapeJava(log));
        bufferedWriter.newLine();
        bufferedWriter.flush();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } else {
      tempBufferToUpload.add(StringEscapeUtils.escapeJava(log));
    }
  }

  private static boolean isSdPresent() {
    return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
  }

  private synchronized long getLogFileCreateDate() throws NumberFormatException, IOException {
    BufferedReader br;
    if (sdCard)
      br = new BufferedReader(new FileReader(logFilePath));
    else
      br = new BufferedReader(new InputStreamReader(context.openFileInput(logFilePath)));
    String readLine = br.readLine();
    Long dateInMillis = Long.valueOf(readLine);
    br.close();
    return dateInMillis;
  }

  synchronized void deleteLogFileAndCreateNew() {
    File logfile = new File(logFilePath);
    logfile.delete();
    openLogFile(logFilePath, true);
    saveTempBufferToLogFileAndClear();
  }

  void saveTempBufferToLogFileAndClear() {
    lockedToUpload = false;
    for (String log : tempBufferToUpload) {
      writeToLogFile(log, false);
    }
    tempBufferToUpload.clear();
  }

  synchronized void uploadLogsAndCreateNewLogfile(final Context context) {
    if (actUploaderThread == null || !actUploaderThread.isAlive()) {
      actUploaderThread = new Thread(new Uploader(logToJsonConverter, context));
      actUploaderThread.start();
    }
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }

  public static class LOGGER_STRINGS {

    public static class OTHER {
      public static final String CLICK_TO_MESSAGEGROUP_STR = "click to messagegroup";
      public static final String SPACE_STR = " ";
      public static final String UNDERLINE_SIGN_STR = "_";
      public static final String NEW_MESSAGE_STR = "newMessage";
      public static final String SENDMESSAGE_STR = "sendmessage";
      public static final String EDITTEXT_WRITE_STR = "edittext_write";
    }

    public static class MAINPAGE {
      public static final String PAUSE_STR = "mainpage:pause";
      public static final String RESUME_STR = "mainpage:resume";
      public static final String BACKBUTTON_STR = "mainpage:backbutton";
      public static final String STR = "MainPage";
    }

    public static class EMAIL {
      public static final String EMAIL_BACKBUTTON_STR = "Email:backbutton";
    }

    public static class SCROLL {
      public static final String END_STR = "scroll:end";
      public static final String START_STR = "scroll:start";
    }
    
    public static class NOTIFICATION {
      public static final String NOTIFICATION_POPUP_STR = "notification:popup";
    }
    
    public static class APPLICATION {
      public static final String APPLICATION_START_STR = "application:start";
    }
    
    public static class LOG_UPLOAD {
      public static final String UPLOAD_FAILED_STR = "log_upload:failed";
    }
    
    public static class CLICK {
      public static final String CLICK_ACCOUNT_BTN = "click:account_button";
      public static final String CLICK_MESSAGE_SEND_BTN = "click:message_send_button";
      public static final String CLICK_REFRESH_BTN = "click:refresh_button";
      public static final String CLICK_LOAD_MORE_BTN = "click:load_more_button";
    }
    
    public static class MESSAGE_REPLY {
      public static final String MESSAGE_REPLY_BACKBUTTON_STR = "MessageReply:backbutton";
    }
    
    public static class THREAD {
      public static final String THREAD_BACKBUTTON_STR = "thread:backbutton";
      public static final String THREAD_PAUSE_STR = "thread:pause";
      public static final String THREAD_RESUME_STR = "thread:resume";
    }
    
    public static class ACCOUNTSETTING {
      public static final String ACCOUNT_SETTINGS_LIST_BACKBUTTON_STR = "AccountSettingsList:backbutton";
      public static final String FACEBOOK_SETTING_ACTIVITY_BACKBUTTON_STR = "FacebookSettingActivity:backbutton";
      public static final String GMAIL_SETTING_ACTIVITY_BACKBUTTON_STR = "GmailSettingActivity:backbutton";
      public static final String INFMAIL_SETTING_ACTIVITY_BACKBUTTON_STR = "InfmailSettingActivity:backbutton";
      public static final String SIMPLE_EMAIL_SETTING_ACTIVITY_BACKBUTTON_STR = "SimpleEmailSettingActivity:backbutton";
    }
    
    

  }

}
