
package hu.rgai.android.beens;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class RunningSetup {

    private final Account mAccount;
    private final int offset;
    private final int limit;

    public RunningSetup(Account mAccount, int offset, int limit) {
      this.mAccount = mAccount;
      this.offset = offset;
      this.limit = limit;
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 11 * hash + (this.mAccount != null ? this.mAccount.hashCode() : 0);
      hash = 11 * hash + this.offset;
      hash = 11 * hash + this.limit;
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final RunningSetup other = (RunningSetup) obj;
      if (this.mAccount != other.mAccount && (this.mAccount == null || !this.mAccount.equals(other.mAccount))) {
        return false;
      }
      if (this.offset != other.offset) {
        return false;
      }
      if (this.limit != other.limit) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "RunningSetup{" + "mAccount=" + mAccount + ", offset=" + offset + ", limit=" + limit + '}';
    }
    
  }
