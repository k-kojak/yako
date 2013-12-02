package hu.rgai.android.messageproviders;

public class SmsMessageRecipient {

	private String address;

	  public SmsMessageRecipient(String address) {
	    this.address = address;
	  }

	  public String getAddress() {
	    return address;
	  }
}
