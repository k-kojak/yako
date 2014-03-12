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
import java.util.ArrayList;

import org.apache.commons.lang3.StringEscapeUtils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public enum EventLogger {
  INSTANCE;

  private volatile BufferedWriter bufferedWriter;
  private boolean logFileOpen = false;
  String logFilePath;
  long logfileCreatedTime;
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

  public synchronized boolean openLogFile( String logFilePath, boolean isFullPath) {

    File logfile;
    if (isFullPath) {
      logfile = new File( logFilePath);
    } else {
      if (isSdPresent()) {
        logfile = new File( Environment.getExternalStorageDirectory().getAbsoluteFile(), logFilePath);
      } else {
        logfile = new File( Environment.getDataDirectory().getAbsoluteFile(), logFilePath);
      }
    }
    this.logFilePath = logfile.getPath();
    if (logfile.exists()) {
      try {
        bufferedWriter = new BufferedWriter( new FileWriter( logfile, true));
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
        bufferedWriter = new BufferedWriter( new FileWriter( logfile));
        logfileCreatedTime = LogToJsonConverter.getCurrentTime();
        bufferedWriter.write( Long.toString( logfileCreatedTime));
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

  public boolean isLogFileOpen() {
    return logFileOpen;
  }

  public synchronized boolean closeLogFile() {
    try {
      bufferedWriter.close();
    } catch ( IOException e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return true;
  }

  public synchronized void writeToLogFile( String log, boolean logTimeStamp) {
    if (logTimeStamp) {
      writeFormatedLogToLogFile( LogToJsonConverter.getCurrentTime() + SPACE_STR + log);
    } else {
      writeFormatedLogToLogFile( log);
    }

  }

  private void writeFormatedLogToLogFile( String log) {
    Log.d( "willrgai", log);
    if (!lockedToUpload) {
      try {
        bufferedWriter.write( StringEscapeUtils.escapeJava( log));
        bufferedWriter.newLine();
        bufferedWriter.flush();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } else {
      tempBufferToUpload.add( StringEscapeUtils.escapeJava( log));
    }
  }

  private static boolean isSdPresent() {
    return android.os.Environment.getExternalStorageState().equals( android.os.Environment.MEDIA_MOUNTED);
  }

  private synchronized long getLogFileCreateDate() throws NumberFormatException, IOException {
    FileReader logFileReader = new FileReader( logFilePath);
    BufferedReader br = new BufferedReader( logFileReader);
    String readLine = br.readLine();
    Long dateInMillis = Long.valueOf( readLine);
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
      writeToLogFile( log, false);
    }
    tempBufferToUpload.clear();
  }

  synchronized void uploadLogsAndCreateNewLogfile( final Context context) {
    if ( actUploaderThread == null || !actUploaderThread.isAlive() ) {
      actUploaderThread = new Thread(new Uploader(logToJsonConverter, context));
      actUploaderThread.start();
    }
  }

  public Context getContext() {
    return context;
  }

  public void setContext( Context context) {
    this.context = context;
  }

  public static class LOGGER_STRINGS {

    public static class OTHER {
      public static final String CLICK_TO_MESSAGEGROUP_STR = "click to messagegroup";
      public static final String SPACE_STR = " ";

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

  }

}
