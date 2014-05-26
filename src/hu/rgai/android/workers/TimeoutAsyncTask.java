
package hu.rgai.android.workers;

import android.os.AsyncTask;
import android.os.Handler;
import hu.rgai.android.handlers.TimeoutHandler;
import hu.rgai.android.tools.AndroidUtils;

public abstract class TimeoutAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
  
  private long mTimeout = -1;
  private TimeoutHandler mTimeoutHandler = null;

  public TimeoutAsyncTask(TimeoutHandler handler) {
    mTimeoutHandler = handler;
  }

  public void setTimeout(long timeout) {
    this.mTimeout = timeout;
  }
  
  /**
   * This function sets the timeoutHandler variable which .timeout() function will be called on
   * a timeout event.
   * 
   * @param handler the timeout handler
   */
  protected final void setTimeoutHandler(TimeoutHandler handler) {
    mTimeoutHandler = handler;
  }
  
  /**
   * Params MUST be provided here, because in case its left  empty, the AsyncTask executor
   * would call for .execute(Object[]) instead of .execute(T[]), so thats why we have to
   * set the parameter explicitely even if its empty.
   * 
   * @param params the params to execute
   */
  public void executeTask(Params[] params) {
    AndroidUtils.<Params, Progress, Result>startTimeoutAsyncTask(this, params);
    
    if (mTimeout != -1) {
      Handler h = new Handler();
      h.postDelayed(new Runnable() {
        public void run() {
          if (TimeoutAsyncTask.this.getStatus() == AsyncTask.Status.RUNNING) {
            TimeoutAsyncTask.this.cancel(true);
            if (mTimeoutHandler != null) {
              mTimeoutHandler.timeout();
            }
          }
        }
      }, mTimeout);
    }
  }
  
}
