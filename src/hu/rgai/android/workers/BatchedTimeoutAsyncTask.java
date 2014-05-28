
package hu.rgai.android.workers;

import hu.rgai.android.handlers.TimeoutHandler;


public abstract class BatchedTimeoutAsyncTask<Params, Progress, Result> extends TimeoutAsyncTask<Params, Progress, Result> {

  private BatchedAsyncTaskExecutor executor = null;
  
  public BatchedTimeoutAsyncTask(TimeoutHandler handler) {
    super(handler);
  }

  public void setExecutor(BatchedAsyncTaskExecutor executor) {
    this.executor = executor;
  }


  @Override
  protected final void onPostExecute(Result result) {
    if (executor != null) {
      executor.taskFinished(false);
    }
    onBatchedPostExecute(result);
  }

  @Override
  public void taskCancelled() {
    if (executor != null) {
      executor.taskFinished(true);
    }
  }


  protected abstract void onBatchedPostExecute(Result result);
  
}
