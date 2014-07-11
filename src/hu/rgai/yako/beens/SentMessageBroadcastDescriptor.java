
package hu.rgai.yako.beens;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class's descendant holds the value for a proper action when a message was sent succesfully.
 * 
 * @author Tamas Kojedzinszky
 */
public class SentMessageBroadcastDescriptor implements Parcelable {

  private final Class mBroadcastClassToCall;
  private final String mAction;
  private int mResultType = 0;
  
  private SentMessageData mMessageData;

  public static final Parcelable.Creator<SentMessageBroadcastDescriptor> CREATOR = new Parcelable.Creator<SentMessageBroadcastDescriptor>() {
    public SentMessageBroadcastDescriptor createFromParcel(Parcel in) {
      try {
        return new SentMessageBroadcastDescriptor(in);
      } catch (ClassNotFoundException ex) {
        Log.d("rgai", "", ex);
        return null;
      }
    }

    public SentMessageBroadcastDescriptor[] newArray(int size) {
      return new SentMessageBroadcastDescriptor[size];
    }
  };
  
  public SentMessageBroadcastDescriptor(Parcel in) throws ClassNotFoundException {
    this(Class.forName(in.readString()), in.readString());
    mResultType = in.readInt();
    
    boolean hasMessageData = in.readByte() == 1;
    if (hasMessageData) {
      Class c = Class.forName(in.readString());
      mMessageData = in.readParcelable(c.getClassLoader());
    } else {
      mMessageData = null;
    }
  }
  
  public SentMessageBroadcastDescriptor(Class mBroadcastClassToLoad, String mAction) {
    this.mBroadcastClassToCall = mBroadcastClassToLoad;
    this.mAction = mAction;
  }

  public Class getBroadcastClassToCall() {
    return mBroadcastClassToCall;
  }

  public String getAction() {
    return mAction;
  }

  public void setResultType(int resultType) {
    this.mResultType = resultType;
  }

  public int getResultType() {
    return mResultType;
  }

  public SentMessageData getMessageData() {
    return mMessageData;
  }

  public void setMessageData(SentMessageData mMessageData) {
    this.mMessageData = mMessageData;
  }
  
  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(getBroadcastClassToCall().getName());
    dest.writeString(getAction());
    dest.writeInt(getResultType());
    
    dest.writeByte((byte)(mMessageData == null ? 0 : 1));
    if (mMessageData != null) {
      dest.writeString(mMessageData.getClass().getName());
      dest.writeParcelable(mMessageData, flags);
    }
  }
    
}
