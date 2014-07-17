package hu.rgai.yako.beens;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import hu.rgai.yako.messageproviders.MessageProvider;

// TODO: this class should be a singletone
public final class SmsAccount extends Account {
  
  private static SmsAccount instance = null;

	public static final Parcelable.Creator<SmsAccount> CREATOR = new Parcelable.Creator<SmsAccount>() {
    public SmsAccount createFromParcel(Parcel in) {
      return getInstance();
    }

    public SmsAccount[] newArray(int size) {
	      return new SmsAccount[size];
	    }
	};


  public static synchronized void setInstance(long _id) {
    Log.d("rgai2", "setting account instance with id: " + _id);
    if (instance == null) {
      instance = new SmsAccount(_id);
    } else {
      instance.m_id = _id;
    }
  }


  public static synchronized void clearInstance() {
    instance = null;
  }


  public static SmsAccount getInstance() {
    return instance;
  }


  public SmsAccount(long _id) {
    super(_id);
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


  // There is only 1 instance of SMS account, so yes, 2 SMS accounts are always the same
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
    return "SMS instance";
  }


  @Override
  public boolean isThreadAccount() {
    return true;
  }
}
