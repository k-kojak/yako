/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.android.intent.beens.account;

import android.os.Parcel;
import android.os.Parcelable;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookSessionAccount;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookSessionAccountAndr extends FacebookSessionAccount implements AccountAndr, Parcelable {
  
  private int messageLimit;

  public static final Parcelable.Creator<FacebookSessionAccountAndr> CREATOR = new Parcelable.Creator<FacebookSessionAccountAndr>() {
    public FacebookSessionAccountAndr createFromParcel(Parcel in) {
      return new FacebookSessionAccountAndr(in);
    }

    public FacebookSessionAccountAndr[] newArray(int size) {
      return new FacebookSessionAccountAndr[size];
    }
  };
  
  public FacebookSessionAccountAndr(Parcel in) {
    super(in.readString(), in.readString());
    this.messageLimit = in.readInt();
  }
  
  public FacebookSessionAccountAndr(int messageLimit, String displayName, String uniqueName) {
    super(displayName, uniqueName);
    this.messageLimit = messageLimit;
  }

  public int getMessageLimit() {
    return messageLimit;
  }

  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel out, int flags) {
    out.writeString(getDisplayName());
    out.writeString(getUniqueName());
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
    final FacebookSessionAccountAndr other = (FacebookSessionAccountAndr) obj;
    if ((this.getUniqueName() == null) ? (other.getUniqueName() != null) : !this.getUniqueName().equals(other.getUniqueName())) {
      return false;
    }
    if (this.getAccountType() != other.getAccountType()) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString() {
    return accountType.toString() + " -> " + displayName + " ("+ uniqueName +")";
  }

}
