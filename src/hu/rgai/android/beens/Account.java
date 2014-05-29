package hu.rgai.android.beens;

import android.os.Parcelable;
import hu.rgai.android.messageproviders.MessageProvider;
import java.io.Serializable;


/**
 *
 * @author Tamas Kojedzinszky
 */
public abstract class Account implements Parcelable, Serializable, Comparable<Account> {
  
  public abstract String getDisplayName();
  
  public abstract MessageProvider.Type getAccountType();
  
  @Override
  public abstract boolean equals(Object account);
  
  public abstract int getMessageLimit();
  
  @Override
  public abstract String toString();
  
  public abstract boolean isInternetNeededForLoad();
  
  /**
   * True if this kind of account can have only one instance, false otherwise.
   * 
   * I.e. SMS and Facebook account is unique now, but email accounts are not.
   * @return 
   */
  public abstract boolean isUnique();
  
  public int compareTo(Account a) {
    
    if (this.getAccountType().equals(a.getAccountType())) {
      return getDisplayName().compareTo(a.getDisplayName());
    } else {
      if (this.getAccountType().equals(MessageProvider.Type.SMS)) {
        return 1;
      } else {
        return this.getAccountType().toString().compareTo(a.getAccountType().toString());
      }
    }
  }
  
}
