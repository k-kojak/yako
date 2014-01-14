package hu.rgai.android.eventlogger;

import hu.rgai.android.eventlogger.communication.*;
import hu.rgai.android.eventlogger.communication.HTTP.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import static hu.rgai.android.test.Constants.*;

public enum EventLogger {
  INSTANCE;

  //static Context context;
  private volatile BufferedWriter bufferedWriter;
  private String logFilePath;
  private long logfileCreatedTime;
  LogToJsonConverter logToJsonConverter = new LogToJsonConverter( apiCodeToAI, appPackageName);
  
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
      } else {
        logfile = new File( logFilePath );
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
        logfileCreatedTime = LogToJsonConverter.getCurrentTime();
        bufferedWriter.write( Long.toString( logfileCreatedTime ) );
        bufferedWriter.newLine();
        bufferedWriter.flush();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
    }
    
    return true;
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
        bufferedWriter.write( LogToJsonConverter.getCurrentTime() + SPACE_STR + log);
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
  
  synchronized boolean uploadLogsAndCreateNewLogfile( Context context ) {
    boolean uploadSucces = true;
    lockedToUpload = true;
    try {
      uploadSucces = uploadSucces && uploadLogsToServer( context );
    } catch (ClientProtocolException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
  
  public synchronized boolean uploadLogsToServer( Context context ) throws ClientProtocolException, IOException{
    
    boolean uploadSucces = true;
    final HttpPost httpPost = new HttpPost(SERVLET_URL);
    final StringEntity httpEntity = new StringEntity("{\"apiKey\": \"bb6ae46aa833d6faadc8758d07cf00d234984f33\" \"deviceId\": \"10122423432532\", \"package\": \"hu.rgai.android\" \"records\": [ { \"timestamp\": \"102123123213213\", \"data\": [] },]}", org.apache.http.protocol.HTTP.UTF_8);
    httpEntity.setContentType("application/json");
    httpPost.setEntity(httpEntity);     
    HttpResponse res = new HttpClient( context, HTTP.createHttpParams( 20000)).execute(httpPost);
    Log.d("willrgai", res.toString() );
    return uploadSucces;
  }

    

}
