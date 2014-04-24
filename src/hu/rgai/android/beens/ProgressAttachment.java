
package hu.rgai.android.beens;

import hu.rgai.android.workers.AttachmentDownloader;
import hu.uszeged.inf.rgai.messagelog.beans.Attachment;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ProgressAttachment extends Attachment {
  
  private volatile int mProgress;
  private volatile boolean inProgress = false;
  private volatile AttachmentDownloader mAttachmentDownloader;
  
  public ProgressAttachment(Attachment attachment) {
    this(attachment.getFileName(), attachment.getSize());
  }
  
  public ProgressAttachment(String fileName, long size) {
    super(fileName, size);
    setProgress(0);
  }
  
  public ProgressAttachment(String fileName, long size, int progress) {
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

  public AttachmentDownloader getAttachmentDownloader() {
    return mAttachmentDownloader;
  }

  public void setAttachmentDownloader(AttachmentDownloader mAttachmentDownloader) {
    this.mAttachmentDownloader = mAttachmentDownloader;
  }
  
}
