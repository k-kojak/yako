
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
  protected final void onCancelled() {
    executor.taskFinished();
    onBatchedCancelled();
  }
  

  @Override
  protected final void onCancelled(Result result) {
    executor.taskFinished();
    onBatchedCancelled(result);
  }
  

  @Override
  protected final void onPostExecute(Result result) {
    executor.taskFinished();
    
    onBatchedPostExecute(result);
  }
  
  
  protected abstract void onBatchedCancelled();
  protected abstract void onBatchedCancelled(Result result);
  protected abstract void onBatchedPostExecute(Result result);

}
