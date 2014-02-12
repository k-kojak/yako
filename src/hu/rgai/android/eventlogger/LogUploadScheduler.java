package hu.rgai.android.eventlogger;

import hu.rgai.android.test.MainActivity;

public class LogUploadScheduler {
  final private long DEFAULT_WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM = 1000 * 60 * 15;
  final private long WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM_AFTER_DAEFAULT_WAIT_TIME = 1000 * 5 * 60;

  Thread scheduler;

  public boolean isRunning = false;
  LogUploader mStatusChecker;

  public LogUploadScheduler() {
    mStatusChecker = new LogUploader(DEFAULT_WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM, WAIT_TIME_TO_UPLOAD_IN_MILLISECUNDUM_AFTER_DAEFAULT_WAIT_TIME);
  }

  public void startRepeatingTask() {

    mStatusChecker.setRepeatTask(true);
    scheduler = new Thread(mStatusChecker);
    scheduler.start();
    isRunning = true;
  }

  public void stopRepeatingTask() {
    isRunning = false;

    if (scheduler.getState() == Thread.State.TIMED_WAITING) {
      mStatusChecker.setRepeatTask(false);
      scheduler.interrupt();
    } else {
      mStatusChecker.setRepeatTask(false);
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

  public boolean isRepeatTask() {
    return repeatTask;
  }

  public void setRepeatTask(boolean repeatTask) {
    this.repeatTask = repeatTask;
  }

  public LogUploader(long defaultWaitTimeInMilliSecondum, long waitTimeAfterDefaultWaitTimeInMilliSecondum) {

    this.defaultWaitTimeInMilliSecondum = defaultWaitTimeInMilliSecondum;
    this.waitTimeAfterDefaultWaitTimeInMilliSecondum = waitTimeAfterDefaultWaitTimeInMilliSecondum;
  }

  @Override
  public void run() {
    while (repeatTask) {
      long elapsedTimeSinceLogCreated = LogToJsonConverter.getCurrentTime() - EventLogger.INSTANCE.getLogfileCreatedTime();
      if (elapsedTimeSinceLogCreated < defaultWaitTimeInMilliSecondum) {
        try {
          Thread.sleep(defaultWaitTimeInMilliSecondum - elapsedTimeSinceLogCreated);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else {
        if (!MainActivity.isNetworkAvailable()) {
          try {
            Thread.sleep(waitTimeAfterDefaultWaitTimeInMilliSecondum);
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        } else {
          if (!EventLogger.INSTANCE.uploadLogsAndCreateNewLogfile(MainActivity.instance)) {
            EventLogger.INSTANCE.writeToLogFile(LOGUPLOAD_FAILED_STR, true);
            try {
              Thread.sleep(waitTimeAfterDefaultWaitTimeInMilliSecondum);
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