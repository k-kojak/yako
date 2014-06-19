
package hu.rgai.yako.beens;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to store data through the message sending procedure, so when the
 * message was sent (or not) we are able to handle the result properly.
 * 
 * @author Tamas Kojedzinszky
 */
public class SimpleSentMessageData implements SentMessageData {

  private final String mRecipientName;
  private Account mAccountToLoad;
  
  public static final Parcelable.Creator<SimpleSentMessageData> CREATOR = new Parcelable.Creator<SimpleSentMessageData>() {
    public SimpleSentMessageData createFromParcel(Parcel in) {
      try {
        return new SimpleSentMessageData(in);
      } catch (ClassNotFoundException ex) {
        Logger.getLogger(SimpleSentMessageData.class.getName()).log(Level.SEVERE, null, ex);
        return null;
      }
    }

    public SimpleSentMessageData[] newArray(int size) {
      return new SimpleSentMessageData[size];
    }
  };
  
  
  public SimpleSentMessageData(Parcel in) throws ClassNotFoundException {
    mRecipientName = in.readString();
    boolean isAccountSetToLoad = in.readByte() == 1;
    if (isAccountSetToLoad) {
      Class c = Class.forName(in.readString());
      mAccountToLoad = in.readParcelable(c.getClassLoader());
    } else {
      mAccountToLoad = null;
    }
  }
  
  
  public SimpleSentMessageData(String recipientName) {
    mRecipientName = recipientName;
  }
  
  public void setAccountToLoad(Account account) {
    mAccountToLoad = account;
  }

  public String getRecipientName() {
    return mRecipientName;
  }

  public Account getAccountToLoad() {
    return mAccountToLoad;
  }

  
  @Override
  public int describeContents() {
    return 0;
  }

  
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(mRecipientName);
    dest.writeByte((byte)(mAccountToLoad != null ? 1 : 0));
    if (mAccountToLoad != null) {
      dest.writeString(mAccountToLoad.getClass().getName());
      dest.writeParcelable(mAccountToLoad, flags);
    }
  }

}
