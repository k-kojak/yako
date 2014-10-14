
package hu.rgai.yako.beens;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.TreeSet;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FullThreadMessage implements FullMessage {
  
  private TreeSet<FullSimpleMessage> mMessages = null;

  public static final Parcelable.Creator<FullThreadMessage> CREATOR = new Parcelable.Creator<FullThreadMessage>() {
    public FullThreadMessage createFromParcel(Parcel in) {
      return new FullThreadMessage(in);
    }

    public FullThreadMessage[] newArray(int size) {
      return new FullThreadMessage[size];
    }
  };
  
  public FullThreadMessage(Parcel in) {
    mMessages = new TreeSet<FullSimpleMessage>();
    Parcelable[] pArr = in.readParcelableArray(FullSimpleMessage.class.getClassLoader());
    for (Parcelable msga : pArr) {
      mMessages.add((FullSimpleMessage) msga);
    }
  }

  public FullThreadMessage(TreeSet<FullSimpleMessage> messages) {
    this.mMessages = messages;
  }
  
  /**
   * Default constructor.
   */
  public FullThreadMessage() {
    mMessages = new TreeSet<FullSimpleMessage>();
  }
  

  public TreeSet<FullSimpleMessage> getMessages() {
    return mMessages;
  }

  public void addMessage(FullSimpleMessage ma) {
    mMessages.add(ma);
  }
  
  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel out, int flags) {
    Parcelable[] pMessages = new Parcelable[mMessages.size()];
    int i = 0;
    for (FullSimpleMessage ma : mMessages) {
      pMessages[i++] = ma;
    }
    out.writeParcelableArray(pMessages, flags);
  }

  @Override
  public String toString() {
    return "FullThreadMessage{" + "mMessages=" + mMessages + '}';
  }
  
}
