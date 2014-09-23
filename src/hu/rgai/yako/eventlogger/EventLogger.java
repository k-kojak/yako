package hu.rgai.yako.eventlogger;

import static hu.rgai.yako.eventlogger.Constants.SPACE_STR;
import static hu.rgai.yako.eventlogger.Constants.apiCodeToAI;
import static hu.rgai.yako.eventlogger.Constants.appPackageName;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringEscapeUtils;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

public enum EventLogger {
  INSTANCE;

  private static final int GPS_AND_PROCCES_LOGGING_WAIT_TIME = 60 * 1000;


  public enum LogFilePaths {
    FILE_TO_UPLOAD_PATH(getFullPathToFile("upload_logfile.txt")),
    FILE_TO_MESSAGES_PATH(getFullPathToFile("messages_logfile.txt"));
    
    private final String path;

    /**
     * @param path
     */
    private LogFilePaths(final String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return path;
    }
  }
  
  Map< LogFilePaths, BufferedWriter> fileWriters = new HashMap<LogFilePaths, BufferedWriter>();
  
  private boolean logFileOpen = false;

  long logfileCreatedTime;

  private final Handler h = new Handler();

  private String appVersion = null;

  private LocationLogger locationLogger;

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

  static boolean sdCard = false;

  Set<ArchivedRunningAppProcessInfo> runnedApps = new HashSet<ArchivedRunningAppProcessInfo>();

  public static String getFullPathToFile( String path) {
    
    if (isSdPresent()) {
      sdCard = true;
      return new File(Environment.getExternalStorageDirectory().getAbsoluteFile(), path).getAbsolutePath();
    } else {
      return new File(path).getName();
    }
  }
  
  public synchronized boolean openAllLogFile() {
    boolean opening = true;

    opening = opening && openLogFile(LogFilePaths.FILE_TO_MESSAGES_PATH);
    opening = opening && openLogFile(LogFilePaths.FILE_TO_UPLOAD_PATH);

    logFileOpen = opening;
    return opening;
  }
  
  private synchronized boolean openLogFile(LogFilePaths logFilePath) {
    File logfile = new File(logFilePath.toString());
    if (logfile.exists()) {
      openExistingLogFile(logFilePath, logfile);
    } else {
      openNotExistingLogfile(logFilePath, logfile);
    }
    return true;
  }

  private void openNotExistingLogfile(LogFilePaths logFilePath, File logfile) {
    try {
      BufferedWriter bufferedWriter;
      if (!sdCard)
        bufferedWriter = new BufferedWriter( new OutputStreamWriter( context.openFileOutput( logFilePath.toString(), Context.MODE_PRIVATE)));
      else
        bufferedWriter = new BufferedWriter(new FileWriter(logfile));
      logfileCreatedTime = LogToJsonConverter.getCurrentTime();
      bufferedWriter.write(Long.toString(logfileCreatedTime));
      bufferedWriter.newLine();
      bufferedWriter.flush();
      fileWriters.put( logFilePath, bufferedWriter);
    } catch (IOException e) {
      Log.d("willrgai", "", e);
      // TODO Auto-generated catch block
    }
  }

  private void openExistingLogFile(LogFilePaths logFilePath, File logfile) {
    try {
      BufferedWriter bufferedWriter;
      if (!sdCard)
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(logFilePath.toString(), Context.MODE_APPEND)));
      else
        bufferedWriter = new BufferedWriter(new FileWriter(logfile, true));
      logfileCreatedTime = getLogFileCreateDate();
      fileWriters.put( logFilePath, bufferedWriter);
    } catch (NumberFormatException e) {
      Log.d("willrgai", "", e);
      lockedToUpload = true;
      deleteLogFileAndCreateNew(logFilePath);
    } catch (IOException e) {
      Log.d("willrgai", "", e);
    }
  }

  public String getAppVersion() {
    if (appVersion == null) {
      PackageInfo pInfo;
      try {
        pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        appVersion = pInfo.versionName;
      } catch (NameNotFoundException e) {
        Log.d("willrgai", "", e);
        appVersion = "not_checked_version";
      }
    }
    return appVersion;
  }

  public boolean isLogFileOpen() {
    return logFileOpen;
  }

  public synchronized boolean closeLogFile() {
    for (Map.Entry<LogFilePaths, BufferedWriter> entry_fileWriters : fileWriters.entrySet()) {
      try {
        entry_fileWriters.getValue().close();
      } catch (IOException e) {
        Log.d("willrgai", "", e);
      }
    }
    return true;
  }

  public synchronized void writeToLogFile( LogFilePaths logFilePath, String log, boolean logTimeStamp) {
    if (logTimeStamp) {
      writeFormatedLogToLogFile( logFilePath, LogToJsonConverter.getCurrentTime() + SPACE_STR + log);
    } else {
      writeFormatedLogToLogFile( logFilePath, log);
    }
  }

  private void writeFormatedLogToLogFile( LogFilePaths logFilePath, String log) {
//    Log.d("willrgai", log);
    BufferedWriter bufferedWriter = fileWriters.get(logFilePath);
    if (!lockedToUpload) {
      try {
        bufferedWriter.write( StringEscapeUtils.escapeJava(log));
        bufferedWriter.newLine();
        bufferedWriter.flush();
      } catch (IOException e) {
        Log.d( "willrgai", "", e);
      } catch (Exception ex) {
        Log.d( "willrgai", "", ex);
      }
    } else {
      tempBufferToUpload.add(StringEscapeUtils.escapeJava(log));
    }
  }

  public void writeRunningProcessesNamesToLogFile() {
    ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    Set<ArchivedRunningAppProcessInfo> runningApps = new HashSet<ArchivedRunningAppProcessInfo>();
    long timeStamp = LogToJsonConverter.getCurrentTime();
    for (RunningAppProcessInfo pid : am.getRunningAppProcesses()) {
      if (pid.importance == RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE || pid.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND || pid.importance == RunningAppProcessInfo.IMPORTANCE_VISIBLE)
        runningApps.add(new ArchivedRunningAppProcessInfo(pid));
    }

    if (runnedApps.containsAll(runningApps) && runningApps.size() == runnedApps.size())
      return;

    runnedApps = runningApps;
    StringBuilder sb = new StringBuilder(String.valueOf(timeStamp));
    sb.append(SPACE_STR).append(LOGGER_STRINGS.OTHER.RUNNING_APPLICATIONS);
    for (ArchivedRunningAppProcessInfo archivedRunningAppProcessInfo : runnedApps) {
      sb.append(SPACE_STR).append(String.valueOf(archivedRunningAppProcessInfo.uid))
          .append(SPACE_STR).append(archivedRunningAppProcessInfo.processName)
          .append(SPACE_STR).append(archivedRunningAppProcessInfo.importance);
    }
    writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, sb.toString(), false);
  }

  private static boolean isSdPresent() {
    return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
  }

  private synchronized long getLogFileCreateDate() throws NumberFormatException, IOException {
    BufferedReader br;
    if (sdCard)
      br = new BufferedReader(new FileReader(LogFilePaths.FILE_TO_UPLOAD_PATH.toString()));
    else
      br = new BufferedReader(new InputStreamReader(context.openFileInput(LogFilePaths.FILE_TO_UPLOAD_PATH.toString())));
    String readLine = br.readLine();
    Long dateInMillis = Long.valueOf(readLine);
    br.close();
    return dateInMillis;
  }

  synchronized void deleteLogFileAndCreateNew( LogFilePaths logFilePath) {
    File logfile = new File( logFilePath.toString());
    logfile.delete();
    openLogFile( logFilePath);
    saveTempBufferToLogFileAndClear( logFilePath);
  }

  void saveTempBufferToLogFileAndClear( LogFilePaths logFilePath) {
    lockedToUpload = false;
    for (String log : tempBufferToUpload) {
      writeToLogFile(logFilePath, log, false);
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

  private final Runnable myRunnable = new Runnable() {
    @Override
    public void run() {
      locationLogger.updateLocation();
      writeRunningProcessesNamesToLogFile();
      h.postDelayed(myRunnable, GPS_AND_PROCCES_LOGGING_WAIT_TIME);
    }
  };

  public void setContext(Context context) {
    this.context = context;
    locationLogger = new LocationLogger(context);
    h.postDelayed(myRunnable, GPS_AND_PROCCES_LOGGING_WAIT_TIME);
  }

  public void zipMessages() {
    try {
      zipFile( LogFilePaths.FILE_TO_MESSAGES_PATH.toString(),  LogFilePaths.FILE_TO_MESSAGES_PATH.toString() +  ".YAKOZIP" + String.valueOf(getLogfileCreatedTime()) );
      deleteLogFileAndCreateNew(LogFilePaths.FILE_TO_MESSAGES_PATH);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  
  public void zipFile(String inputFilePath, String outputFilePath) throws IOException {
    FileOutputStream fileOutputStream;
    if (!sdCard)
      fileOutputStream = context.openFileOutput(outputFilePath, Context.MODE_PRIVATE);
    else
      fileOutputStream = new FileOutputStream(outputFilePath);
    ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

    File inputFile = new File(inputFilePath);

    ZipEntry zipEntry = new ZipEntry(inputFile.getName());
    zipOutputStream.putNextEntry(zipEntry);
    
    FileInputStream fileInputStream;
    if (!sdCard)
      fileInputStream = context.openFileInput( inputFilePath);
    else
      fileInputStream = new FileInputStream(inputFile);
    byte[] buf = new byte[1024];
    int bytesRead;

    while ((bytesRead = fileInputStream.read(buf)) > 0) {
        zipOutputStream.write(buf, 0, bytesRead);
        Log.d("willrgai", String.valueOf(bytesRead));
    }
    
    // close ZipEntry to store the stream to the file
    zipOutputStream.closeEntry();
    zipOutputStream.close();
    fileInputStream.close();
    
  }
  
  public static class LOGGER_STRINGS {

    public static class OTHER {
      public static final String CLICK_TO_MESSAGEGROUP_STR = "click to messagegroup";
      public static final String SPACE_STR = " ";
      public static final String UNDERLINE_SIGN_STR = "_";
      public static final String NEW_MESSAGE_STR = "newMessage";
      public static final String SENDMESSAGE_STR = "sendmessage";
      public static final String EDITTEXT_WRITE_STR = "edittext_write";
      public static final String MESSAGE_WRITE_FROM_CONTACT_LIST = "message_write_from_contact_list";
      public static final String RUNNING_APPLICATIONS = "running_applications";
    }

    public static class MAINPAGE {
      public static final String PAUSE_STR = "mainpage:pause";
      public static final String RESUME_STR = "mainpage:resume";
      public static final String BACKBUTTON_STR = "mainpage:backbutton";
      public static final String STR = "MainPage";
      public static final String ALL_STR = "all";
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

class ArchivedRunningAppProcessInfo {
  int importance;

  int uid;

  String processName;

  public ArchivedRunningAppProcessInfo(RunningAppProcessInfo pid) {
    importance = pid.importance;
    uid = pid.uid;
    processName = pid.processName;
  }

  @Override
  public int hashCode() {
    int hash = 1;
    hash = hash * 17 + uid;
    hash = hash * 31 + processName.hashCode();
    hash = hash * 13 + importance;
    return hash;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null)
      return false;
    if (!(other instanceof ArchivedRunningAppProcessInfo))
      return false;
    ArchivedRunningAppProcessInfo o = (ArchivedRunningAppProcessInfo) other;
    if (uid != o.uid)
      return false;
    if (importance != o.importance)
      return false;
    if (!processName.equals(o.processName))
      return false;
    return true;
  }
}
