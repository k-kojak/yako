package hu.rgai.yako.beens;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//android.os.Parcel;
import android.os.Parcel;
import android.os.Parcelable;
import hu.rgai.yako.messageproviders.MessageProvider;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class EmailAccount extends Account {

  protected static final int default_imap_port = 993;
  protected static final int default_smtp_port = 455;
  
  
  protected String email;
  protected String password;
  protected String imapAddress;
  protected String smtpAddress;
  protected int imapPort;
  protected int smtpPort;
  protected boolean ssl;
  protected MessageProvider.Type accountType;

  public static final Parcelable.Creator<EmailAccount> CREATOR = new Parcelable.Creator<EmailAccount>() {
    public EmailAccount createFromParcel(Parcel in) {
      return new EmailAccount(in);
    }

    public EmailAccount[] newArray(int size) {
      return new EmailAccount[size];
    }
  };


  public EmailAccount(Parcel in) {
    this(in.readString(), in.readString(), in.readString(), in.readString(), in.readInt(),
            in.readInt(), in.readByte() == 1, in.readLong());
  }

  public EmailAccount(String email, String password, String imapAddress, String smtpAddress, boolean ssl) {
    this(email, password, imapAddress, smtpAddress, ssl, -1);
  }

  public EmailAccount(String email, String password, String imapAddress, String smtpAddress, boolean ssl, int _id) {
    this(email, password, imapAddress, smtpAddress, default_imap_port, default_smtp_port, ssl, _id);
  }

  public EmailAccount(String email, String password, String imapAddress, String smtpAddress, int imapPort, int smtpPort,
                      boolean ssl, long _id) {
    super(_id);
    this.email = email;
    this.password = password;
    this.imapAddress = imapAddress;
    this.smtpAddress = smtpAddress;
    this.imapPort = imapPort;
    this.smtpPort = smtpPort;
    this.ssl = ssl;
    this.accountType = MessageProvider.Type.EMAIL;
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
    out.writeByte(this.ssl == true ? (byte) 1 : 0);
    out.writeLong(m_id);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final EmailAccount other = (EmailAccount) obj;
    if ((this.email == null) ? (other.email != null) : !this.email.equals(other.email)) {
      return false;
    }
    if (this.accountType != other.accountType) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 79 * hash + (this.email != null ? this.email.hashCode() : 0);
    hash = 79 * hash + (this.accountType != null ? this.accountType.hashCode() : 0);
    return hash;
  }
  

  @Override
  public String toString() {
    return this.accountType.toString() + " -> " + email;
  }

  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public String getImapAddress() {
    return imapAddress;
  }

  public String getSmtpAddress() {
    return smtpAddress;
  }

  public int getImapPort() {
    return imapPort;
  }

  public int getSmtpPort() {
    return smtpPort;
  }

  public boolean isSsl() {
    return ssl;
  }

  public MessageProvider.Type getAccountType() {
    return accountType;
  }

  public String getDisplayName() {
    return email;
  }

  public boolean isInternetNeededForLoad() {
    return true;
  }

  public boolean isUnique() {
    return false;
  }

  @Override
  public boolean isThreadAccount() {
    return false;
  }

}
