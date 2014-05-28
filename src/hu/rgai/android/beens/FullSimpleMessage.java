
package hu.rgai.android.beens;

import android.os.Parcel;
import android.os.Parcelable;
import hu.rgai.android.messageproviders.MessageProvider;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FullSimpleMessage implements FullMessage, Comparable<FullSimpleMessage> {
  
  protected String id;
  protected String subject;
  protected HtmlContent content;
  protected Date date;
  protected Person from;
  protected boolean isMe;
  protected MessageProvider.Type messageType;
  protected List<Attachment> attachments;
  
  public static final Parcelable.Creator<FullSimpleMessage> CREATOR = new Parcelable.Creator<FullSimpleMessage>() {
    public FullSimpleMessage createFromParcel(Parcel in) {
      return new FullSimpleMessage(in);
    }

    public FullSimpleMessage[] newArray(int size) {
      return new FullSimpleMessage[size];
    }
  };

  public FullSimpleMessage() {}
  
  public FullSimpleMessage(Parcel in) {
    this.id = in.readString();
    this.subject = in.readString();
    this.content = in.readParcelable(HtmlContent.class.getClassLoader());
    this.date = new Date(in.readLong());
    this.from = in.readParcelable(Person.class.getClassLoader());
    this.isMe = in.readByte() == 1;
    this.messageType = MessageProvider.Type.valueOf(in.readString());
    
    attachments = new LinkedList<Attachment>();
    in.readList(attachments, Attachment.class.getClassLoader());
  }
  
  /**
   * Constructor for an atom message.
   * 
   * @param id the id of the message
   * @param subject the subject of the message
   * @param content the content of the message with content mime type
   * @param date the date of the message
   * @param from a Person object, the sender of the message
   * @param attachments attachments if is there any, can be <code>null</code>
   * @param isMe true if the sender of the message was me, false otherwise
   * @param type type of the message
   */
  public FullSimpleMessage(String id, String subject, HtmlContent content, Date date, Person from,
          boolean isMe, MessageProvider.Type type, List<Attachment> attachments) {
    this.id = id;
    this.subject = subject;
    this.content = content;
    this.date = date;
    this.from = from;
    this.isMe = isMe;
    this.messageType = type;
    this.attachments = attachments;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public HtmlContent getContent() {
    return content;
  }

  public void setContent(HtmlContent content) {
    this.content = content;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public Person getFrom() {
    return from;
  }

  public void setFrom(Person from) {
    this.from = from;
  }
  
  public List<Attachment> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<Attachment> attachments) {
    this.attachments = attachments;
  }
  
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean isIsMe() {
    return isMe;
  }

  public void setIsMe(boolean isMe) {
    this.isMe = isMe;
  }

  public MessageProvider.Type getMessageType() {
    return messageType;
  }

  public void setMessageType(MessageProvider.Type messageType) {
    this.messageType = messageType;
  }

  @Override
  public int compareTo(FullSimpleMessage o) {
    if (this.getId().equals(o.getId())) {
      return 0;
    } else {
      // TODO: this comparison is not enough, sometimes the date can be exactly the same
      return this.getDate().compareTo(o.getDate());
    }
  }
  
  public int describeContents() {
    return 0;
  }
  
  public void writeToParcel(Parcel out, int flags) {
    out.writeString(id);
    out.writeString(subject);
    out.writeParcelable((Parcelable)content, flags);
    out.writeLong(date.getTime());
    // from MUST be parcelable here
    out.writeParcelable(from, flags);
    out.writeByte(isMe ? (byte)1 : (byte)0);
    out.writeString(messageType.toString());
    out.writeList(attachments);
    
  }

  @Override
  public String toString() {
    return "FullSimpleMessage{" + "id=" + id + ", subject=" + subject + ", content=" + content + ", date=" + date + ", from=" + from + ", isMe=" + isMe + ", messageType=" + messageType + ", attachments=" + attachments + '}';
  }
  
}
