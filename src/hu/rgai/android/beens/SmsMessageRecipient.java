package hu.rgai.android.beens;

import android.net.Uri;
import hu.rgai.android.messageproviders.MessageProvider;

public class SmsMessageRecipient implements MessageRecipient {

	private String displayData;
  private String phoneNum;
  private String displayName;
  private Uri imgUri;
  private int contactId;

  public SmsMessageRecipient(String displayData, String phoneNum, String displayName, Uri imgUri, int contactId) {
    this.displayData = displayData;
    this.phoneNum = phoneNum;
    this.displayName = displayName;
    this.imgUri = imgUri;
    this.contactId = contactId;
  }

  public String getDisplayData() {
    return displayData;
  }

  public String getData() {
    return phoneNum;
  }

  public String getDisplayName() {
    return displayName;
  }

  public Uri getImgUri() {
    return imgUri;
  }

  public int getContactId() {
    return contactId;
  }

  public MessageProvider.Type getType() {
    return MessageProvider.Type.SMS;
  }
}
