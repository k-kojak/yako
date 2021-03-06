/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.yako.beens;

import android.os.Parcel;
import android.os.Parcelable;
import hu.rgai.yako.messageproviders.MessageProvider;


/**
 *
 * @author Tamas Kojedzinszky
 */
public class GmailAccount extends EmailAccount {
  
  public static final Parcelable.Creator<GmailAccount> CREATOR = new Parcelable.Creator<GmailAccount>() {
    public GmailAccount createFromParcel(Parcel in) {
      return new GmailAccount(in);
    }

    public GmailAccount[] newArray(int size) {
      return new GmailAccount[size];
    }
  };
  
  public GmailAccount(Parcel in) {
    this(in.readString(), in.readString(), in.readLong());
  }

  public GmailAccount(String email, String password) {
    this(email, password, -1);
  }

  public GmailAccount(String email, String password, long _id) {
    super(email, password, "imap.gmail.com", "smtp.gmail.com", default_imap_port, default_smtp_port, true, _id);
    this.accountType = MessageProvider.Type.GMAIL;
  }
  
  @Override
  public int describeContents() {
    return 0;
  }
  
  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeString(email);
    out.writeString(password);
    out.writeLong(m_id);
  }
  
  @Override
  public String toString() {
    return "GmailAccount{" + "email=" + email + '}';
  }
  
}
