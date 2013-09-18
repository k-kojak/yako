/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.android.intent.beens.account;

import android.os.Parcel;
import android.os.Parcelable;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;


/**
 *
 * @author Tamas Kojedzinszky
 */
public class EmailAccountAndr extends EmailAccount implements AccountAndr, Parcelable {

  private int messageLimit;

  public static final Parcelable.Creator<EmailAccountAndr> CREATOR = new Parcelable.Creator<EmailAccountAndr>() {
    public EmailAccountAndr createFromParcel(Parcel in) {
      return new EmailAccountAndr(in);
    }

    public EmailAccountAndr[] newArray(int size) {
      return new EmailAccountAndr[size];
    }
  };
  
  public EmailAccountAndr(Parcel in) {
    super(in.readString(), in.readString(), in.readString(), in.readString(), in.readInt(), in.readInt(), in.readByte() == 1);
    this.messageLimit = in.readInt();
  }
  
  public EmailAccountAndr(String email, String password, String imapAddress, String smtpAddress, boolean ssl, int messageLimit) {
    super(email, password, imapAddress, smtpAddress, ssl);
    this.messageLimit = messageLimit;
  }
  
  public EmailAccountAndr(String email, String password, String imapAddress, String smtpAddress, int messageLimit) {
    this(email, password, imapAddress, smtpAddress, true, messageLimit);
  }

  public int getMessageLimit() {
    return messageLimit;
  }

  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel out, int flags) {
    out.writeString(this.email);
    out.writeString(this.password);
    out.writeString(this.imapAddress);
    out.writeString(this.smtpAddress);
    out.writeInt(this.imapPort);
    out.writeInt(this.smtpPort);
    out.writeByte(this.ssl == true ? (byte)1 : 0);
    out.writeInt(messageLimit);
  }

  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final EmailAccountAndr other = (EmailAccountAndr) obj;
    if ((this.email == null) ? (other.email != null) : !this.email.equals(other.email)) {
      return false;
    }
    if (this.accountType != other.accountType) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString() {
    return this.accountType.toString() + " -> " + email;
  }
  
}