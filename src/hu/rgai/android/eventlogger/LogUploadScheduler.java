package hu.rgai.android.eventlogger;

import hu.rgai.android.test.MainActivity;

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
  
  private static final String LOGUPLOAD_FAILED_STR = "logupload:failed";
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
      if ( elapsedTimeSinceLogCreated < defaultWaitTimeInMilliSecondum ) {
        try {
          Thread.sleep( defaultWaitTimeInMilliSecondum - elapsedTimeSinceLogCreated );
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else {
        if ( !mainActivity.isNetworkAvailable()) {
          try {
            Thread.sleep( waitTimeAfterDefaultWaitTimeInMilliSecondum );
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        } else {

          if (!EventLogger.INSTANCE.uploadLogsAndCreateNewLogfile( this.mainActivity )) {
            EventLogger.INSTANCE.writeToLogFile(LOGUPLOAD_FAILED_STR, true);
          }
            
        }
          
      }

      
    }

  }
};