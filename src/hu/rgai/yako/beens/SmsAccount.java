package hu.rgai.yako.beens;

import android.os.Parcel;
import android.os.Parcelable;
import hu.rgai.yako.messageproviders.MessageProvider;

// TODO: this class should be a singletone
public final class SmsAccount extends Account {
  
  public static final SmsAccount account = new SmsAccount();
  
	public static final Parcelable.Creator<SmsAccount> CREATOR = new Parcelable.Creator<SmsAccount>() {
	    public SmsAccount createFromParcel(Parcel in) {
	      return new SmsAccount(in);
	    }

	    public SmsAccount[] newArray(int size) {
	      return new SmsAccount[size];
	    }
	};
	  
	
	private SmsAccount() {}
	
	public SmsAccount(Parcel in) {
	}

	@Override
	public String getDisplayName() {
		return "SMS";
	}

	@Override
	public MessageProvider.Type getAccountType() {
		return MessageProvider.Type.SMS;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
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

  public boolean isInternetNeededForLoad() {
    return false;
  }

  public boolean isUnique() {
    return true;
  }

  @Override
  public String toString() {
    return "SMS account";
  }

  @Override
  public boolean isThreadAccount() {
    return true;
  }
}
