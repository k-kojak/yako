package hu.rgai.android.eventlogger;

import hu.rgai.android.test.MainActivity;
import android.os.Handler;
import android.util.Log;

public class LogUploadScheduler {
  final private long DEFAULT_WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM = 1000 * 60 * 60 * 24;
  final private long WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM_AFTER_DAEFAULT_WAIT_TIME = 1000 * 15 * 60;
  
  Thread scheduler;
  
  public boolean isRunning = false;
  LogUploader mStatusChecker;
  
  public LogUploadScheduler( MainActivity mainActivity) {
    mStatusChecker = new LogUploader( mainActivity, DEFAULT_WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM, WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM_AFTER_DAEFAULT_WAIT_TIME );
  }
  
  public void startRepeatingTask() {
    
    mStatusChecker.setRepeatTask( true );
    scheduler = new Thread(mStatusChecker);
    scheduler.start();
    isRunning = true;
  }

  public void stopRepeatingTask() {
    isRunning = false;
    
    if ( scheduler.getState() == Thread.State.TIMED_WAITING) {
      mStatusChecker.setRepeatTask( false );
      scheduler.notify();
    } else {
      mStatusChecker.setRepeatTask( false );
    }
    scheduler = null;
  }
  
}

class LogUploader implements Runnable {
  
  boolean repeatTask = false;
  boolean threadIsSleep = false;
  final private long defaultWaitTimeInMilliSecondum;
  final private long waitTimeAfterDefaultWaitTimeInMilliSecondum; 
  MainActivity mainActivity;
  
  public boolean isRepeatTask() {
    return repeatTask;
  }

  public void setRepeatTask(boolean repeatTask) {
    this.repeatTask = repeatTask;
  }

  public LogUploader( MainActivity mainActivity, long defaultWaitTimeInMilliSecondum, long waitTimeAfterDefaultWaitTimeInMilliSecondum) {
    this.mainActivity = mainActivity;
    this.defaultWaitTimeInMilliSecondum = defaultWaitTimeInMilliSecondum;
    this.waitTimeAfterDefaultWaitTimeInMilliSecondum = waitTimeAfterDefaultWaitTimeInMilliSecondum;
  }

  @Override 
  public void run() {
    while ( repeatTask ) {
      long elapsedTimeSinceLogCreated = LogToJsonConverter.getCurrentTime() - EventLogger.INSTANCE.getLogfileCreatedTime();
      Log.d("willrgai", "elapsedtime " + elapsedTimeSinceLogCreated);
      if ( elapsedTimeSinceLogCreated < defaultWaitTimeInMilliSecondum ) {
        try {
          Log.d("willrgai", "defaultWaitTimeInMilliSecondum");
          Thread.sleep( defaultWaitTimeInMilliSecondum - elapsedTimeSinceLogCreated );
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else {
        if ( !mainActivity.isNetworkAvailable()) {
          try {
            Log.d("willrgai", "waitTimeAfterDefaultWaitTimeInMilliSecondum");
            Thread.sleep( waitTimeAfterDefaultWaitTimeInMilliSecondum );
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        } else {
          Log.d("willrgai", "uploadLogsAndCreateNewLogfile");
          EventLogger.INSTANCE.uploadLogsAndCreateNewLogfile( this.mainActivity );
        }
          
      }

      
    }

  }
};