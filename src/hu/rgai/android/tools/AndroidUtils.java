
package hu.rgai.android.tools;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
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
import hu.rgai.android.test.YakoApp;
import hu.rgai.android.workers.ActiveConnectionConnector;
import hu.rgai.android.workers.TimeoutAsyncTask;
import java.util.List;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AndroidUtils {

  
//  public static void connectConnectableMessageProviders(Context context) {
//    List<Account> accounts = StoreHandler.getAccounts(context);
//    
//    for (Account a : accounts) {
//      Log.d("rgai", "try connecting account -> " + a);
//      MessageProvider mp = getMessageProviderInstanceByAccount(a, context);
//      checkAndConnectMessageProviderIfConnectable(mp, context);
//    }
//  }
  
  public static void checkAndConnectMessageProviderIfConnectable(MessageProvider mp, boolean isConnectionAlive, Context context) {
    if (mp.canBroadcastOnNewMessage() && !isConnectionAlive) {
      ActiveConnectionConnector connector = new ActiveConnectionConnector(mp, context);
      connector.executeTask(null);
    }
  }
  
  public static void stopReceiversForAccount(Account account, Context context) {
    MessageProvider provider = getMessageProviderInstanceByAccount(account, context);
    if (provider != null && provider.isConnectionAlive()) {
      Log.d("rgai", "Igen, dropping connection");
      provider.dropConnection();
    } else {
      Log.d("rgai", "connection is not alive...thats the problem");
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
  
  public static<T, U, V> void startTimeoutAsyncTask(TimeoutAsyncTask<T, U, V> at, T... args) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      at.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
    } else {
      at.execute(args);
    }
  }
  
}
