package hu.rgai.android.intent.beens.account;

import hu.uszeged.inf.rgai.messagelog.MessageProvider.Type;

/**
 *
 * @author Tamas Kojedzinszky
 */
public interface AccountAndr {
  
  public int getMessageLimit();
  public Type getAccountType();
  
  /**
   * Custom equals funciton.
   * 
   * @param account
   * @return 
   */
  public boolean equals(Object account);
  
}
