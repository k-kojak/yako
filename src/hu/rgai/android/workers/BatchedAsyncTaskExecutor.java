
package hu.rgai.android.workers;

import android.util.Log;
import hu.rgai.android.beens.BatchedProcessState;
import hu.rgai.android.handlers.BatchedAsyncTaskHandler;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public class BatchedAsyncTaskExecutor {
  
  private final int mTasksCount;
  private int mTasksDone;
  private final List<BatchedTimeoutAsyncTask> mTasks;
  private final Object[][] mParams;
  private final String mProgressId;
  private BatchedAsyncTaskHandler mBatchHandler;
  
  private volatile static TreeSet<String> runningStacks = new TreeSet<String>();
  private volatile static TreeMap<String, BatchedProcessState> runningStacksState = new TreeMap<String, BatchedProcessState>();
  
  
  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks, Object[][] params,
          String progressId, BatchedAsyncTaskHandler handler) throws Exception {
    
    mTasksDone = 0;
    mTasksCount = tasks.size();
    mTasks = tasks;
    mParams = params;
    mProgressId = progressId;
    mBatchHandler = handler;
    
    if (mParams != null && mTasks.size() != mParams.length) {
      throw new Exception("Parameter length mismatch exception: tasks vs params: " + mTasks.size() + " vs " + mParams.length);
    }
    for (BatchedTimeoutAsyncTask task : tasks) {
      task.setExecutor(this);
    }
  }
  
  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks, Object[][] params,
          String progressId) throws Exception {
    this(tasks, params, progressId, null);
  }
  
  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks, Object[][] params) throws Exception {
    this(tasks, params, null, null);
  }
  
  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks) throws Exception {
    this(tasks, null, null, null);
  }
  
  public BatchedAsyncTaskExecutor(List<BatchedTimeoutAsyncTask> tasks, String progressId) throws Exception {
    this(tasks, null, progressId, null);
  }
  
  public boolean execute() {
    if (mProgressId == null || !runningStacks.contains(mProgressId)) {
      if (mProgressId != null) {
        runningStacks.add(mProgressId);
        runningStacksState.put(mProgressId, new BatchedProcessState(mTasksCount));
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
    if (mProgressId != null && runningStacksState.containsKey(mProgressId)) {
      runningStacksState.get(mProgressId).increaseDoneProcesses();
    }
    if (mTasksDone == mTasksCount) {
      if (mProgressId != null) {
        runningStacks.remove(mProgressId);
        runningStacksState.remove(mProgressId);
      }
      if (mBatchHandler != null) {
        mBatchHandler.batchedTaskDone(mProgressId);
      }
    }
  }
  
  public static boolean isProgressRunning(String progressId) {
    return runningStacks.contains(progressId);
  }
  
  public static BatchedProcessState getProgressState(String progressId) {
    if (runningStacksState.containsKey(progressId)) {
      return runningStacksState.get(progressId);
    } else {
      return null;
    }
  }
  
}
