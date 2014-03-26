package hu.rgai.android.intent.beens.account;

import hu.uszeged.inf.rgai.messagelog.beans.account.Account;
import java.io.Serializable;



/**
 *
 * @author Tamas Kojedzinszky
 */
public interface AccountAndr extends Account, Serializable {
  
  public int getMessageLimit();
  @Override
  public String toString();
  
}
