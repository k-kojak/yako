package hu.rgai.yako.beens;

import android.os.Parcelable;
import hu.rgai.yako.messageproviders.MessageProvider;
import java.io.Serializable;


/**
 *
 * @author Tamas Kojedzinszky
 */
public abstract class Account implements Parcelable, Serializable, Comparable<Account> {

  protected long m_id;

  public Account(long _id) {
    m_id = _id;
  }

  public long getDatabaseId() {
    return m_id;
  }

  public abstract String getDisplayName();
  
  public abstract MessageProvider.Type getAccountType();
  
  @Override
  public abstract boolean equals(Object account);
  
  @Override
  public abstract String toString();
  
  public abstract boolean isInternetNeededForLoad();
  
  public abstract boolean isThreadAccount();
  
  /**
   * True if this kind of instance can have only one instance, false otherwise.
   * 
   * I.e. SMS and Facebook instance is unique now, but email accounts are not.
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

  @Override
  public int hashCode() {
    int hash = 3;
    return hash;
  }
  
}
