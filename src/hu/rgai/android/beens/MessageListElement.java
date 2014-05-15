package hu.rgai.android.beens;

import android.os.Parcel;
import android.os.Parcelable;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.messageproviders.MessageProvider.Type;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class MessageListElement implements Parcelable, Comparable<MessageListElement> {

  protected String id;
  protected boolean seen;
  protected String title;
  protected String subTitle;
  protected int unreadCount;
  //TODO: this could be a list
  protected Person from;
  protected List<Person> recipients;
  protected Date date;
  protected Type messageType;
  protected boolean updateFlags;
  
  protected FullMessage fullMessage;

  protected Account account;
  protected String prettyDate = null;
  
  protected static Map<MessageProvider.Type, ClassLoader> stringToClassLoader = null;
  protected static Date today = new Date();
  protected static Date thisYear = new Date();
  
  public static final Parcelable.Creator<MessageListElement> CREATOR = new Parcelable.Creator<MessageListElement>() {
    public MessageListElement createFromParcel(Parcel in) {
      return new MessageListElement(in);
    }

    public MessageListElement[] newArray(int size) {
      return new MessageListElement[size];
    }
  };
  
  /**
   * Constructor for a message element in a list.
   * 
   * @param id id of the message
   * @param seen <code>true</code> if the message is seen, <code>false</code> otherwise
   * @param title title of the message, can be <code>null</code>
   * @param subTitle subtitle of the message, can be <code>null</code>
   * @param unreadCount the number of unread messages
   * @param from a Person object, the sender of the message
   * @param date date of the message
   * @param recipients the list of recipients
   * @param messageType type of the message, see {@link hu.uszeged.inf.rgai.messagelog.MessageProvider.Type} for available types
   * @param updateFlags indicates that this message already exists at the display list, only update the flag infos of this message, but nothing else
   */
  public MessageListElement(String id, boolean seen, String title, String subTitle, int unreadCount, Person from,
          List<Person> recipients, Date date, Account account, Type messageType, boolean updateFlags) {
    this.id = id;
    this.seen = seen;
    this.title = title;
    this.subTitle = subTitle;
    this.unreadCount = unreadCount;
    this.from = from;
    this.recipients = recipients;
    this.date = date;
    this.account = account;
    this.messageType = messageType;
    this.updateFlags = updateFlags;
  }
  
  public MessageListElement(Parcel in) {
    initStringToClassLoader();
    
    this.id = in.readString();
    this.seen = in.readByte() == 1;
    this.title = in.readString();
    this.subTitle = in.readString();
    this.unreadCount = in.readInt();
    this.from = in.readParcelable(Person.class.getClassLoader());
    
    recipients = new LinkedList<Person>();
    in.readList(recipients, Person.class.getClassLoader());
    this.date = new Date(in.readLong());
    this.messageType = Type.valueOf(in.readString());
    this.fullMessage = in.readParcelable(FullMessage.class.getClassLoader());
//    this.prettyDate = in.readString();
    
    if (!stringToClassLoader.containsKey(messageType)) {
      // TODO: display error message
//      Log.d("rgai", "Unsupported account type -> " + messageType);
//      System.exit(1);
    } else {
      this.account = (Account)in.readParcelable(stringToClassLoader.get(messageType));
    }
  }
  
//  public MessageListElement(MessageListElement mle, Account account) {
//    this(mle.getId(), mle.isSeen(), mle.getTitle(), mle.getSubTitle(), mle.getUnreadCount(),
//            mle.getFrom(), mle.getRecipientsList(), mle.getDate(), mle.getMessageType(), mle.isUpdateFlags(),
//            mle.getFullMessage(), account);
//    updatePrettyDateString();
//  }
  
//  public MessageListElement(String id, boolean seen, String title, String subTitle,
//          int unreadCount, Person from, List<Person> recipients, Date date, MessageProvider.Type messageType,
//          boolean updateFlags, FullMessage fullMessage, Account account) {
//    this(id, seen, title, subTitle, unreadCount, from, recipients, date, messageType, updateFlags);
//    convertFullMessageToParc(fullMessage, messageType);
//    
//    this.account = account;
//    updatePrettyDateString();
//  }
  
  /**
   * Default constructor.
   */
  public MessageListElement() {
  }
  
  /**
   * Constructor for a message element in a list.
   * 
   * @param id id of the message
   * @param seen <code>true</code> if the message is seen, <code>false</code> otherwise
   * @param title title of the message, can be <code>null</code>
   * @param unreadCount the number of unread messages
   * @param from a Person object, the sender of the message
   * @param recipients the recipients of the message
   * @param date date of the message
   * @param messageType type of the message, see {@link hu.uszeged.inf.rgai.messagelog.MessageProvider.Type} for available types
   */
  public MessageListElement(String id, boolean seen, String title, int unreadCount, Person from, List<Person> recipients, Date date, Account account, Type messageType) {
    this(id, seen, title, null, unreadCount, from, recipients, date, account, messageType, false);
  }
  
  /**
   * Constructor for a message element in a list.
   * 
   * @param id id of the message
   * @param seen true if the message already seen, false otherwise
   * @param title title of the message, can be <code>null</code>
   * @param from a Person object, the sender of the message
   * @param date date of the message
   * @param recipients the original recipients
   * @param messageType type of the message, see {@link hu.uszeged.inf.rgai.messagelog.MessageProvider.Type} for available types
   */
  public MessageListElement(String id, boolean seen, String title, Person from, List<Person> recipients, Date date, Account account, Type messageType) {
    this(id, seen, title, null, -1, from, recipients, date, account, messageType, false);
  }
  
   /**
   * Constructor for a message element in a list.
   * 
   * @param id id of the message
   * @param seen true if the message already seen, false otherwise
   * @param title title of the message, can be <code>null</code>
   * @param snippet snippet of the message, can be <code>null</code>
   * @param from a Person object, the sender of the message
   * @param recipients the original recipients
   * @param date date of the message
   * @param messageType type of the message, see {@link hu.uszeged.inf.rgai.messagelog.MessageProvider.Type} for available types
   */
  public MessageListElement(String id, boolean seen, String title, String snippet,
          Person from, List<Person> recipients, Date date, Account account, Type messageType) {
    this(id, seen, title, snippet, -1, from, recipients, date, account, messageType, false);
  }
  
  public MessageListElement(String id, boolean seen, Person from, Date date, Account account, Type messageType, boolean updateFlags) {
    this(id, seen, null, null, -1, from, null, date, account, messageType, updateFlags);
  }
  
  private void initStringToClassLoader() {
	  if (stringToClassLoader == null) {
	      stringToClassLoader = new EnumMap<MessageProvider.Type, ClassLoader>(MessageProvider.Type.class);
	      stringToClassLoader.put(MessageProvider.Type.EMAIL, EmailAccount.class.getClassLoader());
	      stringToClassLoader.put(MessageProvider.Type.FACEBOOK, FacebookAccount.class.getClassLoader());
	      stringToClassLoader.put(MessageProvider.Type.GMAIL, GmailAccount.class.getClassLoader());
	      stringToClassLoader.put(MessageProvider.Type.SMS, SmsAccount.class.getClassLoader());
	    }	  
  }
  
//  private void convertFullMessageToParc(FullMessage fullMessage, Type type) {
//    if (fullMessage != null) {
//      Class fullParcMessageClass = Settings.getAccountTypeToFullParcMessageClass().get(type);
//      Class fullMessageClass = Settings.getAccountTypeToFullMessageClass().get(type);
//      if (fullParcMessageClass == null) {
//        throw new RuntimeException("Full message class is null, " + account.getAccountType() + " is not a valid TYPE.");
//      }
//      try {
//        Constructor constructor = fullParcMessageClass.getConstructor(fullMessageClass);
//        this.fullMessage = (FullMessage) constructor.newInstance(fullMessage);
//      } catch (NoSuchMethodException ex) {
//        Logger.getLogger(MessageListElement.class.getName()).log(Level.SEVERE, null, ex);
//      } catch (InstantiationException ex) {
//        Logger.getLogger(MessageListElement.class.getName()).log(Level.SEVERE, null, ex);
//      } catch (IllegalAccessException ex) {
//        Logger.getLogger(MessageListElement.class.getName()).log(Level.SEVERE, null, ex);
//      } catch (IllegalArgumentException ex) {
//        Logger.getLogger(MessageListElement.class.getName()).log(Level.SEVERE, null, ex);
//      } catch (InvocationTargetException ex) {
//        Logger.getLogger(MessageListElement.class.getName()).log(Level.SEVERE, null, ex);
//      }
//    }
//  }
          

  public static void refreshCurrentDates() {
    Date d = new Date();
    
    Calendar c = new GregorianCalendar();
    c.setTime(d);
    c.set(Calendar.HOUR, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    today = c.getTime();
    
    c.set(Calendar.DAY_OF_MONTH, 1);
    c.set(Calendar.MONTH, 0);
    thisYear = c.getTime();
  }
  
  public int describeContents() {
    return 0;
  }
  
  public String getPrettyDate() {
    if (prettyDate == null) {
      updatePrettyDateString(new SimpleDateFormat());
    }
    return prettyDate;
  }
  
  
  public void updatePrettyDateString(SimpleDateFormat sdf) {
    if (date.before(thisYear)) {
      sdf.applyPattern("yyyy/MM/dd");
    } else if (date.after(today)) {
      sdf.applyPattern("HH:mm");
    } else {
      sdf.applyPattern("MMM d");
    }
    
    prettyDate = sdf.format(date);
  }
  
  public final void updatePrettyDateString() {
    updatePrettyDateString(new SimpleDateFormat());
  }

  public Account getAccount() {
    return account;
  }
  
  public Person getFrom() {
    return from;
  }
  
  public String getId() {
    return id;
  }

  public boolean isSeen() {
    return seen;
  }

  public String getTitle() {
    return title;
  }

  public boolean isGroupMessage() {
    return recipients != null && recipients.size() > 1;
  }
  
  public List<Person> getRecipientsList() {
    if (recipients == null) {
      List<Person> rec = new LinkedList<Person>();
      rec.add(from);
      return rec;
    } else {
      return recipients;
    }
  }

  public void setFrom(Person from) {
    this.from = from;
  }

  public Date getDate() {
    return date;
  }

  public String getSubTitle() {
    return subTitle;
  }

  public int getUnreadCount() {
    return unreadCount;
  }

  public void setUnreadCount(int unreadCount) {
    this.unreadCount = unreadCount;
  }
  
  public Type getMessageType() {
    return messageType;
  }

  public boolean isUpdateFlags() {
    return updateFlags;
  }

  public FullMessage getFullMessage() {
    return fullMessage;
  }

  public void setFullMessage(FullMessage fullMessage) {
    this.fullMessage = fullMessage;
  }
  
  public void setSeen(boolean seen) {
    this.seen = seen;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setSubTitle(String subTitle) {
    this.subTitle = subTitle;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  @Override
  public String toString() {
    return "MessageListElement{" + "id=" + id + ", account="+ account +", seen=" + seen + ", title=" + title
            + ", subTitle=" + subTitle + ", from=" + from + ", date=" + date + " ("+ (date != null ? date.getTime() : "-") +"), messageType="
            + messageType + ", updateFlags=" + updateFlags +'}';
  }
  
  public void writeToParcel(Parcel out, int flags) {
    initStringToClassLoader();
    out.writeString(this.id);
    out.writeByte((byte)(seen ? 1 : 0));
    out.writeString(title);
    out.writeString(subTitle);
    out.writeInt(unreadCount);
    out.writeParcelable((Parcelable)from, flags);
    out.writeList(recipients);
    out.writeLong(date.getTime());
    out.writeString(messageType.toString());
    out.writeParcelable((Parcelable)fullMessage, flags);
//    out.writeString(prettyDate);
    if (!stringToClassLoader.containsKey(messageType)) {
    	
    } else {
    	out.writeParcelable((Parcelable)account, flags);
    }
    
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    
    if (getClass() != obj.getClass()) {
      return false;
    }
    
    final MessageListElement other = (MessageListElement) obj;
    
    if (!this.id.equals(other.id)) {
      return false;
    }
    
    if ((this.account == null) ? (other.account != null) : !this.account.equals(other.account)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 37 * hash + (this.id != null ? this.id.hashCode() : 0);
    hash = 37 * hash + (this.account != null ? this.account.hashCode() : 0);
    return hash;
  }
  
  public int compareTo(MessageListElement o) {
    if (this.id.equals(o.getId()) && this.account.equals(o.account)) {
      return 0;
    } else {
      // TODO: THIS IS A SOURCE OF UGLY BUGS!
      // FIXME
      return -1 * this.date.compareTo(o.date);
    }
  }
  
}
