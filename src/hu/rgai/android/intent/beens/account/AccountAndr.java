package hu.rgai.android.intent.beens.account;

import hu.uszeged.inf.rgai.messagelog.beans.account.Account;



/**
 *
 * @author Tamas Kojedzinszky
 */
public interface AccountAndr extends Account {
  
  public int getMessageLimit();
  @Override
  public String toString();
  
}
