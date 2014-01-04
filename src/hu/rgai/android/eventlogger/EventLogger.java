package hu.rgai.android.eventlogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Calendar;

import android.os.Environment;
import android.util.Log;

public enum EventLogger {
  INSTANCE;

  private static final String SPACE_STR = " ";
  private PrintStream outStream;
  private boolean logFileIsopen = false;
  
  private EventLogger(){}
  
  public synchronized boolean openLogFile( String logFileName ) throws FileNotFoundException{
    if ( logFileIsopen )
      return false;
    if ( isSdPresent() ) {
      outStream = new PrintStream( new File( Environment.getExternalStorageDirectory().getAbsoluteFile(), logFileName ));
      Log.d( "willrgai", "vansd");
    } else {
      outStream = new PrintStream( new File( logFileName ));
      Log.d( "willrgai", "nincssd");
    }
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
  
  public synchronized void writeToLogFile( String log) {
    Log.d( "willrgai", "writeToLogFile " + log);
    outStream.print( Calendar.getInstance().getTimeInMillis() );
    outStream.print( SPACE_STR );
    outStream.println( log );
  }

  public static boolean isSdPresent() {
    return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
  }
  
  
  
  
    

}
