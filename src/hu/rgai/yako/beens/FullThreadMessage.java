
package hu.rgai.yako.beens;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FullThreadMessage implements FullMessage {
  
  private Set<FullSimpleMessage> messages = null;

  public static final Parcelable.Creator<FullThreadMessage> CREATOR = new Parcelable.Creator<FullThreadMessage>() {
    public FullThreadMessage createFromParcel(Parcel in) {
      return new FullThreadMessage(in);
    }

    public FullThreadMessage[] newArray(int size) {
      return new FullThreadMessage[size];
    }
  };
  
  public FullThreadMessage(Parcel in) {
    messages = new TreeSet<FullSimpleMessage>();
    Parcelable[] pArr = in.readParcelableArray(FullSimpleMessage.class.getClassLoader());
    for (Parcelable msga : pArr) {
      messages.add((FullSimpleMessage)msga);
    }
  }
  
  /**
   * Default constructor.
   */
  public FullThreadMessage() {
    messages = new TreeSet<FullSimpleMessage>();
  }
  
  public FullThreadMessage(Set<FullSimpleMessage> messages) {
    this.messages = messages;
  }
  
  public Set<FullSimpleMessage> getMessages() {
    return messages;
  }

  public void addMessage(FullSimpleMessage ma) {
    messages.add(ma);
  }
  
  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel out, int flags) {
    Parcelable[] pMessages = new Parcelable[messages.size()];
    int i = 0;
    for (FullSimpleMessage ma : messages) {
      pMessages[i++] = (FullSimpleMessage)ma;
    }
    out.writeParcelableArray(pMessages, flags);
  }
  
}
