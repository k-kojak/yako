
package hu.rgai.android.beens;

import hu.uszeged.inf.rgai.messagelog.beans.Attachment;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ProgressAttachment extends Attachment {
  
  private volatile int mProgress;
  private volatile boolean inProgress = false;
  
  public ProgressAttachment(Attachment attachment) {
    this(attachment.getFileName(), attachment.getSize());
  }
  
  public ProgressAttachment(String fileName, int size) {
    super(fileName, size);
    setProgress(0);
  }
  
  public ProgressAttachment(String fileName, int size, int progress) {
    super(fileName, size);
    setProgress(progress);
  }

  public int getProgress() {
    return mProgress;
  }

  public final void setProgress(int mProgress) {
    this.mProgress = mProgress;
    if (mProgress == 100) {
      inProgress = false;
    }
  }

  public void setInProgress(boolean inProgress) {
    this.inProgress = inProgress;
  }
  
  public boolean isInProgress() {
    return inProgress;
  }
  
  public boolean isDownloaded() {
    return !inProgress && mProgress == 100;
  }
  
}
