
package hu.rgai.android.intent.beens;

import android.os.Parcel;
import android.os.Parcelable;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.FullSimpleMessage;
import hu.uszeged.inf.rgai.messagelog.beans.fullmessage.MessageAtom;
import java.util.Date;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageAtomParc extends MessageAtom implements FullMessageParc, Parcelable {

  public static final Parcelable.Creator<MessageAtomParc> CREATOR = new Parcelable.Creator<MessageAtomParc>() {
    public MessageAtomParc createFromParcel(Parcel in) {
      return new MessageAtomParc(in);
    }

    public MessageAtomParc[] newArray(int size) {
      return new MessageAtomParc[size];
    }
  };
  
  public MessageAtomParc(Parcel in) {
    this.id = in.readString();
    this.subject = in.readString();
    this.content = in.readString();
    this.date = new Date(in.readLong());
    this.from = in.readParcelable(PersonAndr.class.getClassLoader());
    this.messageType = MessageProvider.Type.valueOf(in.readString());
    //TODO: read attachments
    this.attachments = null;
    
  }
  
  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel out, int flags) {
    out.writeString(id);
    out.writeString(subject);
    out.writeString(content);
    out.writeLong(date.getTime());
    out.writeParcelable((Parcelable)new PersonAndr(from), flags);
    out.writeString(messageType.toString());
    // TODO: write attachments
    
  }
  
}
