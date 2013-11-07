
package hu.rgai.android.intent.beens;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FullSimpleMessageParc extends MessageAtomParc {
  
  public static final Parcelable.Creator<FullSimpleMessageParc> CREATOR = new Parcelable.Creator<FullSimpleMessageParc>() {
    public FullSimpleMessageParc createFromParcel(Parcel in) {
      return new FullSimpleMessageParc(in);
    }

    public FullSimpleMessageParc[] newArray(int size) {
      return new FullSimpleMessageParc[size];
    }
  };
  
  public FullSimpleMessageParc(Parcel in) {
    super(in);
  }
  
  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    super.writeToParcel(out, flags);
  }
  
}
