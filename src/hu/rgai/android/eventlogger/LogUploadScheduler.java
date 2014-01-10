package hu.rgai.android.eventlogger;

import hu.rgai.android.test.MainActivity;
import android.os.Handler;

public class LogUploadScheduler {
  
  final private int SECONDUM_IN_ONE_DAY = 1000 * 3600 * 24;
  
  boolean startedNow = true;
  private int mInterval = SECONDUM_IN_ONE_DAY;
  private Handler mHandler = new Handler();
  MainActivity mainActivity;
  
  public LogUploadScheduler( MainActivity mainActivity) {
    this.mainActivity = mainActivity;
  }
  
  Runnable mStatusChecker = new Runnable() {
    @Override 
    public void run() {
      if ( !mainActivity.isNetworkAvailable() || startedNow) {
        if ( startedNow )
          
        mHandler.postDelayed( mStatusChecker, mInterval);
      } else {
        startedNow = false;
      }
    }
  };
  
  void startRepeatingTask() {
    mStatusChecker.run(); 
  }

  void stopRepeatingTask() {
    mHandler.removeCallbacks(mStatusChecker);
  }
  
}
