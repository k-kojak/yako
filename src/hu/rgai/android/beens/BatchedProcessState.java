
package hu.rgai.android.beens;

public class BatchedProcessState {

  private int mProcessDone;
  private final int mTotalProcess;

  public BatchedProcessState(int mTotalProcess) {
    this.mProcessDone = 0;
    this.mTotalProcess = mTotalProcess;
  }

  public int getProcessDone() {
    return mProcessDone;
  }

  public int getTotalProcess() {
    return mTotalProcess;
  }

  public void increaseDoneProcesses() {
    this.mProcessDone++;
  }
  
}
