
package hu.rgai.android.tools;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.EmailAccount;
import hu.rgai.android.beens.FacebookAccount;
import hu.rgai.android.beens.GmailAccount;
import hu.rgai.android.beens.SmsAccount;
import hu.rgai.android.messageproviders.FacebookMessageProvider;
import hu.rgai.android.messageproviders.MessageProvider;
import hu.rgai.android.messageproviders.SimpleEmailMessageProvider;
import hu.rgai.android.messageproviders.SmsMessageProvider;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.workers.ActiveConnectionConnector;
import java.util.List;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AndroidUtils {

  
  public static void connectConnectableMessageProviders(Context context) {
    
    List<Account> accounts = StoreHandler.getAccounts(context);
    
    for (Account a : accounts) {
      Log.d("rgai", "try connecting account -> " + a);
      MessageProvider mp = getMessageProviderInstanceByAccount(a, context);
      if (mp.canBroadcastOnNewMessage() && !mp.isConnectionAlive()) {
        Log.d("rgai", "yes, connectable account -> " + a);
        ActiveConnectionConnector connector = new ActiveConnectionConnector(mp, context);
        AndroidUtils.<String, Integer, Boolean>startAsyncTask(connector);
      }
    }
  }
  
  public static MessageProvider getMessageProviderInstanceByAccount(Account account, Context context) {
    MessageProvider mp = null;
    if (account instanceof GmailAccount) {
      mp = new SimpleEmailMessageProvider((GmailAccount) account);
    } else if (account instanceof EmailAccount) {
      mp = new SimpleEmailMessageProvider((EmailAccount) account);
    } else if (account instanceof FacebookAccount) {
      mp = new FacebookMessageProvider((FacebookAccount) account);
    } else if (account instanceof SmsAccount) {
      mp = new SmsMessageProvider(context);
    }
    return mp;
  }
  
  public static<T, U, V> void startAsyncTask(AsyncTask<T, U, V> at, T... args) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      at.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
    } else {
      at.execute(args);
    }
  }
  
}
