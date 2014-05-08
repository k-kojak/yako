package hu.rgai.android.intent.beens.account;

import android.os.Parcelable;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;



/**
 *
 * @author Tamas Kojedzinszky
 */
public interface Account extends Parcelable {
  
  public String getDisplayName();
  
  public MessageProvider.Type getAccountType();
  
  @Override
  public boolean equals(Object account);
  
  public int getMessageLimit();
  
  @Override
  public String toString();
  
}
