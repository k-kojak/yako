/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.yako.beens;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import hu.rgai.yako.messageproviders.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookAccount extends Account {
  private String displayName;
  private String uniqueName;
  private String id;
  private String password;
  private MessageProvider.Type accountType;
  private int messageLimit;

  public static final Parcelable.Creator<FacebookAccount> CREATOR = new Parcelable.Creator<FacebookAccount>() {
    public FacebookAccount createFromParcel(Parcel in) {
      return new FacebookAccount(in);
    }

    public FacebookAccount[] newArray(int size) {
      return new FacebookAccount[size];
    }
  };
  
  public FacebookAccount() {}
  
  
  public FacebookAccount(Parcel in) {
    this(in.readString(), in.readString(), in.readString(), in.readString(), in.readInt());
  }
  
  public FacebookAccount(String displayName, String uniqueName, String id, String password, int messageLimit) {
    this.displayName = displayName;
    this.uniqueName = uniqueName;
    this.id = id;
    this.password = password;
    this.messageLimit = messageLimit;
    
    this.accountType = MessageProvider.Type.FACEBOOK;
  }

  public int getMessageLimit() {
    return messageLimit;
  }

  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel out, int flags) {
    out.writeString(displayName);
    out.writeString(uniqueName);
    out.writeString(id);
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
    final FacebookAccount other = (FacebookAccount) obj;
    if ((uniqueName == null) ? (other.getUniqueName() != null) : !uniqueName.equals(other.getUniqueName())) {
      return false;
    }
    if (!accountType.equals(other.getAccountType())) {
      return false;
    }
    return true;
  }

  public String getPassword() {
    return password;
  }
  
  @Override
  public MessageProvider.Type getAccountType() {
    return accountType;
  }
  
  @Override
  public String getDisplayName() {
    return displayName;
  }

  public String getUniqueName() {
    return uniqueName;
  }

  public String getId() {
    return id;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 73 * hash + (this.uniqueName != null ? this.uniqueName.hashCode() : 0);
    hash = 73 * hash + (this.accountType != null ? this.accountType.hashCode() : 0);
    return hash;
  }

  @Override
  public String toString() {
    return "FacebookAccount{" + "displayName=" + displayName + ", uniqueName=" + uniqueName + ", type=" + accountType + '}';
  }

  public boolean isInternetNeededForLoad() {
    return true;
  }

  public boolean isUnique() {
    return true;
  }
  
}
