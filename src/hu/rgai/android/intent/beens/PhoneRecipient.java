package hu.rgai.android.intent.beens;

import hu.uszeged.inf.rgai.messagelog.beans.MessageRecipient;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class PhoneRecipient implements MessageRecipient {
  private String number;

  public PhoneRecipient(String number) {
    this.number = number;
  }

  public String getNumber() {
    return number;
  }

}
