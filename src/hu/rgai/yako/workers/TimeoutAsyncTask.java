
package hu.rgai.yako.workers;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import hu.rgai.yako.handlers.TimeoutHandler;
import hu.rgai.yako.tools.AndroidUtils;


/**
 * This class is an extension of AsyncTask with onTimeout functionality.
 * 
 * If a onTimeout is set for the class and the time of execution exceeds the timelimit,
 * .cancel() will be called for the AsyncTask and a .onTimeout() method will be called
 * on the provided TimeoutHandler (if provided). To handle the cancellation properly 
 * on the thread is the responsibility of the programmer, because calling .cancel() will not
 * stop immediately the thread if it is stucked on the doInBackground method.
 * 
 * @author Tamas Kojedzinszky
 * @param <Params>
 * @param <Progress>
 * @param <Result> 
 */
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
   * Params MUST be provided here, because in case its left  empty, the AsyncTask executor
   * would call for .execute(Object[]) instead of .execute(T[]), so thats why we have to
   * set the parameter explicitely even if its empty (or null).
   * 
   * @param params the params to execute
   */
  public void executeTask(final Context context, Params[] params) {
    AndroidUtils.<Params, Progress, Result>startTimeoutAsyncTask(this, params);
    
    if (mTimeout != -1) {
      Handler h = new Handler();
      h.postDelayed(new Runnable() {
        public void run() {
          if (TimeoutAsyncTask.this.getStatus() == AsyncTask.Status.RUNNING) {
            TimeoutAsyncTask.this.taskCancelled();
            TimeoutAsyncTask.this.cancel(true);
            if (mTimeoutHandler != null) {
              mTimeoutHandler.onTimeout(context);
            }
          }
        }
      }, mTimeout);
    }
  }
  
  
  public void taskCancelled() {}
  
}
