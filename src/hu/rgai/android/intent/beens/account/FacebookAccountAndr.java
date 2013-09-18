/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.android.intent.beens.account;

import android.os.Parcel;
import android.os.Parcelable;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookAccountAndr extends FacebookAccount implements AccountAndr, Parcelable {
  
  private int messageLimit;

  public static final Parcelable.Creator<FacebookAccountAndr> CREATOR = new Parcelable.Creator<FacebookAccountAndr>() {
    public FacebookAccountAndr createFromParcel(Parcel in) {
      return new FacebookAccountAndr(in);
    }

    public FacebookAccountAndr[] newArray(int size) {
      return new FacebookAccountAndr[size];
    }
  };
  
  public FacebookAccountAndr(Parcel in) {
    super(in.readString(), in.readString());
    this.messageLimit = in.readInt();
  }
  
  public FacebookAccountAndr(int messageLimit, String userName, String password) {
    super(userName, password);
    this.messageLimit = messageLimit;
  }

  public int getMessageLimit() {
    return messageLimit;
  }

  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel out, int flags) {
    out.writeString(getUserName());
    out.writeString(getPassword());
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
    final FacebookAccountAndr other = (FacebookAccountAndr) obj;
    if ((this.getUserName() == null) ? (other.getUserName() != null) : !this.getUserName().equals(other.getUserName())) {
      return false;
    }
    if (this.getAccountType() != other.getAccountType()) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString() {
    return accountType.toString() + " -> " + userName;
  }

}
