
package hu.rgai.yako.tools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.beens.FacebookAccount;
import hu.rgai.yako.beens.GmailAccount;
import hu.rgai.yako.beens.SmsAccount;
import hu.rgai.yako.messageproviders.FacebookMessageProvider;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.messageproviders.SimpleEmailMessageProvider;
import hu.rgai.yako.messageproviders.SmsMessageProvider;
import hu.rgai.yako.workers.ActiveConnectionConnector;
import hu.rgai.yako.workers.TimeoutAsyncTask;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AndroidUtils {

  
  public static String getCharCountStringForSMS(String text) {
    SmsManager smsMan = SmsManager.getDefault();
    ArrayList<String> dividedMessages = smsMan.divideMessage(text);
    int size = dividedMessages.size();
    return text.length() + "/" + size;
  }
  
  public static int getIndexOfAccount(TreeSet<Account> accounts, Account account) {
    int index = 0;
    for (Account a : accounts) {
      if (a.equals(account)) {
        return index;
      }
      index++;
    }
    return -1;
  }
  
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
  
  
  /**
   * Decides if is network available.
   * 
   * @return true if network is available, false otherwise
   */
  public static boolean isNetworkAvailable(Context c) {
    ConnectivityManager connectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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
