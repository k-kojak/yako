
package hu.rgai.yako.beens;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to store data through the message sending procedure, so when the
 * message was sent (or not) we are able to handle the result properly.
 * 
 * @author Tamas Kojedzinszky
 */
public class SmsSentMessageData extends SimpleSentMessageData {

  private int mItemIndex = 0;
  private int mItemCount = 0;
  
  public static final Parcelable.Creator<SmsSentMessageData> CREATOR = new Parcelable.Creator<SmsSentMessageData>() {
    public SmsSentMessageData createFromParcel(Parcel in) {
      try {
        return new SmsSentMessageData(in);
      } catch (ClassNotFoundException ex) {
        Log.d("rgai", "", ex);
        return null;
      }
    }

    public SmsSentMessageData[] newArray(int size) {
      return new SmsSentMessageData[size];
    }
  };
  
  
  public SmsSentMessageData(Parcel in) throws ClassNotFoundException {
    super(in);
    mItemIndex = in.readInt();
    mItemCount = in.readInt();
  }
  
  
  public SmsSentMessageData(String recipientName) {
    super(recipientName);
  }

  public int getItemIndex() {
    return mItemIndex;
  }

  public int getItemCount() {
    return mItemCount;
  }

  public void setItemIndex(int itemIndex) {
    this.mItemIndex = itemIndex;
  }

  public void setItemCount(int itemCount) {
    this.mItemCount = itemCount;
  }
  
  @Override
  public int describeContents() {
    return 0;
  }

  
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeInt(mItemIndex);
    dest.writeInt(mItemCount);
  }

}
