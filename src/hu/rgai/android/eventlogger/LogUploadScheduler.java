package hu.rgai.android.eventlogger;

import hu.rgai.android.test.MainActivity;
import android.content.Context;

public enum LogUploadScheduler {
  INSTANCE;
  final private long DEFAULT_WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM = 1000 * 60 * 60 * 24;
  final private long WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM_AFTER_DEFAULT_WAIT_TIME = 1000 * 60 * 15;

  Thread scheduler;

  public boolean isRunning = false;
  LogUploader mStatusChecker = null;

  private LogUploadScheduler() {
  }

  public synchronized void setContext( Context c) {
    if (mStatusChecker == null)
      mStatusChecker = new LogUploader(c, DEFAULT_WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM, WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM_AFTER_DEFAULT_WAIT_TIME);
  }

  public synchronized void startRepeatingTask() {
    mStatusChecker.setRepeatTask(true);
    scheduler = new Thread(mStatusChecker);
    scheduler.start();
    isRunning = true;
  }

  public synchronized void stopRepeatingTask() {
    isRunning = false;
    mStatusChecker.setRepeatTask(false);
  }

}

class LogUploader implements Runnable {

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
    // TODO Auto-generated method stub
    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
    while ( repeatTask ) {
      long elapsedTimeSinceLogCreated = LogToJsonConverter.getCurrentTime() - EventLogger.INSTANCE.getLogfileCreatedTime();
      if ( elapsedTimeSinceLogCreated < defaultWaitTimeInMilliSecondum ) {
        try {
          Thread.sleep(defaultWaitTimeInMilliSecondum - elapsedTimeSinceLogCreated);
        } catch ( InterruptedException e ) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else {
        if ( !MainActivity.isNetworkAvailable(c) ) {
          try {
            Thread.sleep(waitTimeAfterDefaultWaitTimeInMilliSecondum);
          } catch ( InterruptedException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        } else {
          try {
            EventLogger.INSTANCE.uploadLogsAndCreateNewLogfile(c);
            Thread.sleep(waitTimeAfterDefaultWaitTimeInMilliSecondum);
          } catch ( InterruptedException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }

      }

    }
  }
};