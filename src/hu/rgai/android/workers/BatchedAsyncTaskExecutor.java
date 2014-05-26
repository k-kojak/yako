
package hu.rgai.android.workers;

import android.util.Log;
import hu.rgai.android.handlers.BatchedAsyncTaskHandler;
import java.util.List;
import java.util.TreeSet;

public class BatchedAsyncTaskExecutor {
  
  private final int mTasksCount;
  private int mTasksDone;
  private final List<BatchedTimeoutAsyncTask> mTasks;
  private final Object[][] mParams;
  private final String mProgressKey;
  private BatchedAsyncTaskHandler mBatchHandler;
  
          
  private volatile static TreeSet<String> runningStack = new TreeSet<String>();
  
  
  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks, Object[][] params,
          String progressKey, BatchedAsyncTaskHandler handler) throws Exception {
    
    mTasksDone = 0;
    mTasksCount = tasks.size();
    mTasks = tasks;
    mParams = params;
    mProgressKey = progressKey;
    mBatchHandler = handler;
    
    if (mParams != null && mTasks.size() != mParams.length) {
      throw new Exception("Parameter length mismatch exception: tasks vs params: " + mTasks.size() + " vs " + mParams.length);
    }
    for (BatchedTimeoutAsyncTask task : tasks) {
      task.setExecutor(this);
    }
  }
  
  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks, Object[][] params,
          String progressKey) throws Exception {
    this(tasks, params, progressKey, null);
  }
  
  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks, Object[][] params) throws Exception {
    this(tasks, params, null, null);
  }
  
  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks) throws Exception {
    this(tasks, null, null, null);
  }
  
  public boolean execute() {
    if (mProgressKey == null || !runningStack.contains(mProgressKey)) {
      if (mProgressKey != null) {
        runningStack.add(mProgressKey);
      }
      int i = 0;
      for (BatchedTimeoutAsyncTask task : mTasks) {
        if (mParams != null) {
          task.executeTask(mParams[i++]);
        } else {
          task.executeTask(null);
        }
      }
      return true;
    } else {
      return false;
    }
  }
  
  public synchronized void taskFinished() {
    mTasksDone++;
    if (mTasksDone == mTasksCount) {
      if (mProgressKey != null) {
        runningStack.remove(mProgressKey);
      }
      if (mBatchHandler != null) {
        mBatchHandler.batchedTaskDone(mProgressKey);
      }
    }
  }
  
}
