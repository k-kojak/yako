package hu.rgai.android.eventlogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import android.os.Environment;

public enum EventLogger {
  INSTANCE;

  private PrintStream outStream;
  private boolean logFileIsopen = false;
  
  private EventLogger(){}
  
  public synchronized boolean openLogFile( String logFileName ) throws FileNotFoundException{
    if ( logFileIsopen )
      return false;
    outStream = new PrintStream( new File( Environment.getExternalStorageDirectory().getAbsoluteFile(), logFileName ));
    logFileIsopen = true;
    return true;
  }
  
  public synchronized boolean closeLogFile() throws FileNotFoundException{
    if ( !logFileIsopen )
      return false;
    outStream.close();
    logFileIsopen = false;
    return true;
  }
  
  public synchronized void writeToLogFile( String log){
    outStream.println( log );
  }

  
  
  
  
  
    

}
