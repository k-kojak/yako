package hu.rgai.android.intent.beens;

import android.net.Uri;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.MessageRecipient;

public class SmsMessageRecipientAndr implements MessageRecipient, RecipientItem {

	private String displayData;
  private String phoneNum;
  private String displayName;
  private Uri imgUri;
  private int contactId;

  public SmsMessageRecipientAndr(String displayData, String phoneNum, String displayName, Uri imgUri, int contactId) {
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
