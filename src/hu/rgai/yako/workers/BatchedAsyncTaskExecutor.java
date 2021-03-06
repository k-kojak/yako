
package hu.rgai.yako.workers;

import android.content.Context;
import hu.rgai.yako.beens.BatchedProcessState;
import hu.rgai.yako.handlers.BatchedAsyncTaskHandler;
import java.util.List;
import java.util.TreeMap;

public class BatchedAsyncTaskExecutor {
  
  private final int mTasksCount;
  private final List<BatchedTimeoutAsyncTask> mTasks;
  private final Object[][] mParams;
  private final String mProgressId;
  private BatchedAsyncTaskHandler mBatchHandler;
  
  private volatile static TreeMap<String, BatchedProcessState> runningStacksState = new TreeMap<>();
  
  
  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks, Object[][] params,
          String progressId, BatchedAsyncTaskHandler handler) throws Exception {
    
    mTasksCount = tasks.size();
    mTasks = tasks;
    mParams = params;
    mProgressId = progressId;
    mBatchHandler = handler;
    
    if (mParams != null && mTasks.size() != mParams.length) {
      throw new Exception("Parameter length mismatch exception: tasks vs params: " + mTasks.size() + " vs " + mParams.length);
    }
//    for (BatchedTimeoutAsyncTask task : tasks) {
//      task.setExecutor(this);
//    }
  }
  
//  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks, Object[][] params,
//          String progressId) throws Exception {
//    this(tasks, params, progressId, null);
//  }
  
//  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks, Object[][] params) throws Exception {
//    this(tasks, params, null, null);
//  }
//
//  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks) throws Exception {
//    this(tasks, null, null, null);
//  }
//
//  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks, String progressId) throws Exception {
//    this(tasks, null, progressId, null);
//  }
  
  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks, String progressId, BatchedAsyncTaskHandler handler) throws Exception {
    this(tasks, null, progressId, handler);
  }
  
  public boolean execute(Context context) {
    for (BatchedTimeoutAsyncTask task : mTasks) {
      task.setExecutor(this);
    }
    if (mProgressId == null || !runningStacksState.containsKey(mProgressId) || runningStacksState.get(mProgressId).isDone()) {
      if (mProgressId != null) {
        runningStacksState.put(mProgressId, new BatchedProcessState(mTasksCount));
        taskFinished(true, false);
      }
      int i = 0;
      for (BatchedTimeoutAsyncTask task : mTasks) {
        if (mParams != null) {
          task.executeTask(context, mParams[i++]);
        } else {
          task.executeTask(context, null);
        }
      }
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * 
   * @param initialCall true if called by onPreExecute, false if called from  onCancelled, onPostExecute
   */
  private synchronized void taskFinished(boolean initialCall, boolean cancelled) {
    if (mProgressId != null && runningStacksState.containsKey(mProgressId)) {
      if (!initialCall) {
        runningStacksState.get(mProgressId).increaseDoneProcesses();
      }
      if (mBatchHandler != null) {
        mBatchHandler.batchedTaskDone(cancelled, mProgressId, runningStacksState.get(mProgressId));
      }
    }
  }
  
  /**
   * 
   * @param cancelled true if task finished with cancellation
   */
  public synchronized void taskFinished(boolean cancelled) {
    taskFinished(false, cancelled);
  }
  
  public static boolean isProgressRunning(String progressId) {
    return runningStacksState.containsKey(progressId) && !runningStacksState.get(progressId).isDone();
  }
  
//  public static BatchedProcessState getProgressState(String progressId) {
//    if (runningStacksState.containsKey(progressId)) {
//      return runningStacksState.get(progressId);
//    } else {
//      return null;
//    }
//  }
  
}
