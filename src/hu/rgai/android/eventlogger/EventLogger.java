package hu.rgai.android.eventlogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;

import android.os.Environment;
import android.util.Log;

public enum EventLogger {
  INSTANCE;

  private static final String SPACE_STR = " ";
  private volatile BufferedWriter bufferedWriter;
  private String logFilePath;
  private long logfileCreatedTime;
  
  public long getLogfileCreatedTime() {
    return logfileCreatedTime;
  }

  private EventLogger(){}
  private ArrayList<String> tempBufferToUpload = new ArrayList<String>();
  private boolean lockedToUpload = false;
  
  
  public synchronized boolean openLogFile( String logFilePath, boolean isFullPath ) {

    File logfile;
    if ( isFullPath ) {
      logfile = new File( logFilePath );
    } else {
      if ( isSdPresent() ) {
        logfile =new File( Environment.getExternalStorageDirectory().getAbsoluteFile(), logFilePath );
        Log.d( "willrgai", "van sd");
      } else {
        logfile = new File( logFilePath );
        Log.d( "willrgai", "nincs sd");
      }
    }

    this.logFilePath = logfile.getPath();
    if ( logfile.exists()){
      try {
        bufferedWriter = new BufferedWriter( new FileWriter(logfile, true) );
        logfileCreatedTime = getLogFileCreateDate();
      } catch (NumberFormatException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      try {
        bufferedWriter = new BufferedWriter( new FileWriter(logfile) );
        logfileCreatedTime = getCurrentTime();
        bufferedWriter.write( Long.toString(logfileCreatedTime));
        bufferedWriter.newLine();
        bufferedWriter.flush();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
    }
    
    return true;
  }
  
  public long getCurrentTime() {
    return Calendar.getInstance().getTimeInMillis();
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
  
  public synchronized void writeToLogFile( String log) {
    Log.d( "willrgai", "writeToLogFile " + log);
    if ( !lockedToUpload ) {
      try {
        bufferedWriter.write( getCurrentTime() + SPACE_STR + log);
        bufferedWriter.flush();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      tempBufferToUpload.add( log );
    }
  }

  private static boolean isSdPresent() {
    return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
  }
  
  private synchronized long getLogFileCreateDate() throws NumberFormatException, IOException {
    FileReader logFileReader = new FileReader( logFilePath );
    BufferedReader br = new BufferedReader( logFileReader );
    long dateInMillis = Long.valueOf( br.readLine() );
    return dateInMillis;
  }
  
  private synchronized void deleteLogFileAndCreateNew()  {
    File logfile = new File( logFilePath );
    logfile.delete();
    openLogFile( logFilePath, true);
    lockedToUpload = false;
    for ( String log : tempBufferToUpload) {
      writeToLogFile(log);
    }
    tempBufferToUpload.clear();    
  }
  
  synchronized boolean uploadLogsAndCreateNewLogfile()  {
    boolean uploadSucces = true;
    lockedToUpload = true;
    uploadSucces = uploadSucces && uploadLogsToServer();
    if ( uploadSucces ) {
      deleteLogFileAndCreateNew();
    } else {
      lockedToUpload = false;
      for ( String log : tempBufferToUpload) {
        writeToLogFile(log);
      }
      tempBufferToUpload.clear();
    }      
    return uploadSucces;
  }
  
  private synchronized boolean uploadLogsToServer(){
    boolean uploadSucces = true;
    return uploadSucces;
  }

    

}
