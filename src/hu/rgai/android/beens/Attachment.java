
package hu.rgai.android.beens;

import android.os.Parcel;
import android.os.Parcelable;
import hu.rgai.android.workers.AttachmentDownloader;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class Attachment implements Parcelable {
  
  private String fileName;
  private long size; // in bytes
  private volatile int mProgress; // in percent
  private volatile boolean inProgress;
  private volatile AttachmentDownloader mAttachmentDownloader;
  
  public static final Parcelable.Creator<Attachment> CREATOR = new Parcelable.Creator<Attachment>() {
    public Attachment createFromParcel(Parcel in) {
      return new Attachment(in);
    }

    public Attachment[] newArray(int size) {
      return new Attachment[size];
    }
  };
  
  public Attachment(Parcel in) {
    fileName = in.readString();
    size = in.readLong();
    mProgress = in.readInt();
    inProgress = in.readByte() == 1;
  }
  
  public Attachment(String fileName, long size) {
    this(fileName, size, 0);
  }
  
  public Attachment(String fileName, long size, int progress) {
    this.fileName = fileName;
    this.size = size;
    mAttachmentDownloader = null;
    
    setProgress(progress);
  }
  
  public int getProgress() {
    return mProgress;
  }
  
  public String getFileName() {
    return fileName;
  }

  public long getSize() {
    return size;
  }
  
  public void setSize(long size) {
    this.size = size;
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

  @Override
  public String toString() {
    return "Attachment{" + "fileName=" + fileName + ", size=" + size + ", mProgress=" + mProgress + ", inProgress=" + inProgress + ", mAttachmentDownloader=" + mAttachmentDownloader + '}';
  }
  

  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel out ,int flags) {
    out.writeString(fileName);
    out.writeLong(size);
    out.writeInt(mProgress);
    out.writeByte((byte)(inProgress ? 1 : 0));
  }
  
}
