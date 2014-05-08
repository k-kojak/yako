package hu.rgai.android.intent.beens.account;

import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.MessageProvider.Type;
import android.os.Parcel;
import android.os.Parcelable;

public class SmsAccountAndr implements Account, Parcelable {
	public static final Parcelable.Creator<SmsAccountAndr> CREATOR = new Parcelable.Creator<SmsAccountAndr>() {
	    public SmsAccountAndr createFromParcel(Parcel in) {
	      return new SmsAccountAndr(in);
	    }

	    public SmsAccountAndr[] newArray(int size) {
	      return new SmsAccountAndr[size];
	    }
	};
	  
	
	public SmsAccountAndr() {}
	
	public SmsAccountAndr(Parcel in) {
	}

	@Override
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type getAccountType() {
		// TODO Auto-generated method stub
		return MessageProvider.Type.SMS;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getMessageLimit() {
		// TODO Auto-generated method stub
		return 10;
	}
  
  // There are only 1 account to SMS (to SIM card), so yes, 2 SMS accounts are always the same
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    return hash;
  }
  
}
