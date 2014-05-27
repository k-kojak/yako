
package hu.rgai.android.workers;

import hu.rgai.android.handlers.TimeoutHandler;


public abstract class BatchedTimeoutAsyncTask<Params, Progress, Result> extends TimeoutAsyncTask<Params, Progress, Result> {

  private BatchedAsyncTaskExecutor executor;
  
  public BatchedTimeoutAsyncTask(TimeoutHandler handler) {
    super(handler);
  }

  public void setExecutor(BatchedAsyncTaskExecutor executor) {
    this.executor = executor;
  }


  @Override
  protected final void onPostExecute(Result result) {
    executor.taskFinished(false);
    onBatchedPostExecute(result);
  }

  @Override
  public void taskCancelled() {
    executor.taskFinished(true);
  }


  protected abstract void onBatchedPostExecute(Result result);
  
}
