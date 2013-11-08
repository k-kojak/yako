
package hu.rgai.android.intent.beens;

import android.os.Parcel;
import android.os.Parcelable;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullThreadMessage;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.MessageAtom;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FullThreadMessageParc extends FullThreadMessage implements FullMessageParc, Parcelable {

  public static final Parcelable.Creator<FullThreadMessageParc> CREATOR = new Parcelable.Creator<FullThreadMessageParc>() {
    public FullThreadMessageParc createFromParcel(Parcel in) {
      return new FullThreadMessageParc(in);
    }

    public FullThreadMessageParc[] newArray(int size) {
      return new FullThreadMessageParc[size];
    }
  };
  
  public FullThreadMessageParc(Parcel in) {
    super();
    Parcelable[] pArr = in.readParcelableArray(MessageAtom.class.getClassLoader());
    for (Parcelable msga : pArr) {
      messages.add((MessageAtomParc)msga);
    }
  }
  
  public FullThreadMessageParc(FullThreadMessage ftm) {
    super(ftm.getMessages());
  }
  
  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel out, int flags) {
    Parcelable[] pMessages = new Parcelable[messages.size()];
    int i = 0;
    for (MessageAtom ma : messages) {
      pMessages[i++] = (MessageAtomParc)ma;
    }
    out.writeParcelableArray(pMessages, flags);
  }

}
