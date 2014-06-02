
package hu.rgai.yako.handlers;

import hu.rgai.yako.beens.BatchedProcessState;

public interface BatchedAsyncTaskHandler {
  public void batchedTaskDone(boolean cancelled, String progressKey, BatchedProcessState processState);
}
