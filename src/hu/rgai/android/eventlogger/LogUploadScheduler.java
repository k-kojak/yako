package hu.rgai.android.eventlogger;

import hu.rgai.android.test.MainActivity;
import android.content.Context;

public enum LogUploadScheduler {
  INSTANCE;
  final private long DEFAULT_WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM = 1000 * 60;
  final private long WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM_AFTER_DEFAULT_WAIT_TIME = 1000 * 60;

  Thread scheduler;

  // private Context c;

  public boolean isRunning = false;
  LogUploader mStatusChecker = null;

  private LogUploadScheduler() {
  }

  public synchronized void setContext( Context c) {
    if (mStatusChecker == null)
      mStatusChecker = new LogUploader( c, DEFAULT_WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM, WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM_AFTER_DEFAULT_WAIT_TIME);
  }

  public synchronized void startRepeatingTask() {
    mStatusChecker.setRepeatTask( true);
    scheduler = new Thread( mStatusChecker);
    scheduler.start();
    isRunning = true;
  }

  public synchronized void stopRepeatingTask() {
    isRunning = false;

    if (scheduler.getState() == Thread.State.TIMED_WAITING) {
      mStatusChecker.setRepeatTask( false);
      scheduler.interrupt();
    } else {
      mStatusChecker.setRepeatTask( false);
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
  private final Context c;

  public boolean isRepeatTask() {
    return repeatTask;
  }

  public void setRepeatTask( boolean repeatTask) {
    this.repeatTask = repeatTask;
  }

  public LogUploader( Context c, long defaultWaitTimeInMilliSecondum, long waitTimeAfterDefaultWaitTimeInMilliSecondum) {
    this.c = c;
    this.defaultWaitTimeInMilliSecondum = defaultWaitTimeInMilliSecondum;
    this.waitTimeAfterDefaultWaitTimeInMilliSecondum = waitTimeAfterDefaultWaitTimeInMilliSecondum;
  }

  @Override
  public void run() {
    while (repeatTask) {
      long elapsedTimeSinceLogCreated = LogToJsonConverter.getCurrentTime() - EventLogger.INSTANCE.getLogfileCreatedTime();
      if (elapsedTimeSinceLogCreated < defaultWaitTimeInMilliSecondum) {
        try {
          Thread.sleep( defaultWaitTimeInMilliSecondum - elapsedTimeSinceLogCreated);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else {
        if (!MainActivity.isNetworkAvailable( c)) {
          try {
            Thread.sleep( waitTimeAfterDefaultWaitTimeInMilliSecondum);
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        } else {
          if (!EventLogger.INSTANCE.uploadLogsAndCreateNewLogfile( c)) {
            EventLogger.INSTANCE.writeToLogFile( LOGUPLOAD_FAILED_STR, true);
            try {
              Thread.sleep( waitTimeAfterDefaultWaitTimeInMilliSecondum);
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }

        }

      }

    }

  }
};