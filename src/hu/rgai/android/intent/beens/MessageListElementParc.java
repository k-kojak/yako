package hu.rgai.android.intent.beens;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.EmailAccountAndr;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.intent.beens.account.GmailAccountAndr;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.MessageProvider.Type;
import hu.uszeged.inf.rgai.messagelog.beans.MessageListElement;
import hu.uszeged.inf.rgai.messagelog.beans.Person;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.ocpsoft.prettytime.PrettyTime;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageListElementParc extends MessageListElement implements Parcelable, Comparable<MessageListElementParc> {

  // TODO: replace fromTemp with Person object
//  PersonAndr from;
  AccountAndr account;
  private static Map<MessageProvider.Type, ClassLoader> stringToClassLoader = null;
  
  public static final Parcelable.Creator<MessageListElementParc> CREATOR = new Parcelable.Creator<MessageListElementParc>() {
    public MessageListElementParc createFromParcel(Parcel in) {
      return new MessageListElementParc(in);
    }

    public MessageListElementParc[] newArray(int size) {
      return new MessageListElementParc[size];
    }
  };
  
  public MessageListElementParc(Parcel in) {
    if (stringToClassLoader == null) {
      stringToClassLoader = new EnumMap<MessageProvider.Type, ClassLoader>(MessageProvider.Type.class);
      stringToClassLoader.put(MessageProvider.Type.EMAIL, EmailAccountAndr.class.getClassLoader());
      stringToClassLoader.put(MessageProvider.Type.FACEBOOK, FacebookAccountAndr.class.getClassLoader());
      stringToClassLoader.put(MessageProvider.Type.GMAIL, GmailAccountAndr.class.getClassLoader());
    }
    
    this.id = in.readString();
    this.seen = in.readByte() == 1;
    this.title = in.readString();
    this.subTitle = in.readString();
    this.from = in.readParcelable(PersonAndr.class.getClassLoader());
    this.date = new Date(in.readLong());
    this.messageType = Type.valueOf(in.readString());
    this.fullMessage = in.readParcelable(FullMessageParc.class.getClassLoader());
    
    if (!stringToClassLoader.containsKey(messageType)) {
      // TODO: display error message
      Log.d("rgai", "Unsupported account type -> " + messageType);
      System.exit(1);
    } else {
      this.account = (AccountAndr)in.readParcelable(stringToClassLoader.get(messageType));
    }
  }
  
  public MessageListElementParc(MessageListElement mle, AccountAndr account) {
    this(mle.getId(), mle.isSeen(), mle.getTitle(), mle.getSubTitle(), mle.getFrom(), mle.getDate(), mle.getMessageType(), account);
//    setFromTemp();
  }
  
  public MessageListElementParc(String id, boolean seen, String title, String subTitle, Person from, Date date, MessageProvider.Type messageType, AccountAndr account) {
    super(id, seen, title, subTitle, from, date, messageType);
    this.account = account;
//    setFromTemp();
  }

  public MessageListElementParc(String id, boolean seen, String title, Person from, Date date, MessageProvider.Type messageType, AccountAndr account) {
    super(id, seen, title, from, date, messageType);
    this.account = account;
//    setFromTemp();
  }

  public MessageListElementParc(String id, String title, String subTitle, Person from, Date date, MessageProvider.Type messageType, AccountAndr account) {
    super(id, title, subTitle, from, date, messageType);
    this.account = account;
//    setFromTemp();
  }

  public MessageListElementParc(String id, String title, Person from, Date date, MessageProvider.Type messageType, AccountAndr account) {
    super(id, title, from, date, messageType);
    this.account = account;
//    setFromTemp();
  }

  public MessageListElementParc(String id, Person from, Date date, MessageProvider.Type messageType, AccountAndr account) {
    super(id, from, date, messageType);
    this.account = account;
//    setFromTemp();
  }
  
//  private void setFromTemp() {
//    this.fromTemp = this.from;
//  }
  
  public int describeContents() {
    return 0;
  }
  
  public String getFormattedDate() {
    PrettyTime pt = new PrettyTime();
    return pt.format(date);
  }
  
//  public String getFromTemp() {
//    return fromTemp;
//  }
  
  public AccountAndr getAccount() {
    return account;
  }
  
  public void writeToParcel(Parcel out, int flags) {
    out.writeString(this.id);
    out.writeByte((byte)(seen ? 1 : 0));
    out.writeString(title);
    out.writeString(subTitle);
    out.writeParcelable((Parcelable)new PersonAndr(from), flags);
    out.writeLong(date.getTime());
    out.writeString(messageType.toString());
    out.writeParcelable((Parcelable)fullMessage, flags);
    
    out.writeParcelable((Parcelable)account, flags);
    
//    if (account.getAccountType().equals(MessageProvider.Type.EMAIL)) {
//      out.writeParcelable(new EmailAccountAndr((EmailAccountAndr)account), flags);
//    } else if (account.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
//      out.writeParcelable(new FacebookAccountParc((FacebookAccount)account), flags);
//    } else if (account.getAccountType().equals(MessageProvider.Type.GMAIL)) {
//      out.writeParcelable(new GmailAccountParc((GmailAccount)account), flags);
//    } else {
//      // TODO: throw normal exception
//      Log.d("rgai", "Unsupported account type -> " + account.getAccountType());
//      System.exit(1);
//    }
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    
    if (getClass() != obj.getClass()) {
      return false;
    }
    
    final MessageListElementParc other = (MessageListElementParc) obj;
    
    if (!this.id.equals(other.id)) {
      return false;
    }
    
    if ((this.account == null) ? (other.account != null) : !this.account.equals(other.account)) {
      return false;
    }

    return true;
  }
  
  @Override
  public String toString() {
    return id + "@" + account + "("+ title +")";
  }

  public int compareTo(MessageListElementParc o) {
    if (this.equals(o)) {
      return 0;
    } else {
      return -1 * this.date.compareTo(o.date);
    }
  }
  
}
