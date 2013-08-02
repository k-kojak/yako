/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.android.intent.beens.account;

import android.os.Parcel;
import android.os.Parcelable;
import hu.uszeged.inf.rgai.messagelog.beans.account.GmailAccount;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class GmailAccountAndr extends GmailAccount implements AccountAndr, Parcelable {
  
  private int messageLimit;

  public static final Parcelable.Creator<GmailAccountAndr> CREATOR = new Parcelable.Creator<GmailAccountAndr>() {
    public GmailAccountAndr createFromParcel(Parcel in) {
      return new GmailAccountAndr(in);
    }

    public GmailAccountAndr[] newArray(int size) {
      return new GmailAccountAndr[size];
    }
  };
  
  public GmailAccountAndr(Parcel in) {
    super(in.readString(), in.readString());
    this.messageLimit = in.readInt();
  }
  
  public GmailAccountAndr(int messageLimit, String email, String password) {
    super(email, password);
    this.messageLimit = messageLimit;
  }

  public int getMessageLimit() {
    return messageLimit;
  }

  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel out, int flags) {
    out.writeString(email);
    out.writeString(password);
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
    final GmailAccountAndr other = (GmailAccountAndr) obj;
    if ((this.email == null) ? (other.email != null) : !this.email.equals(other.email)) {
      return false;
    }
    if (this.accountType != other.accountType) {
      return false;
    }
    return true;
  }
  
}
