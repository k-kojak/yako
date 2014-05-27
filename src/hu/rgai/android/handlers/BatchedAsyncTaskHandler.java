
package hu.rgai.android.handlers;

import hu.rgai.android.beens.BatchedProcessState;

public interface BatchedAsyncTaskHandler {
  public void batchedTaskDone(boolean cancelled, String progressKey, BatchedProcessState processState);
}
