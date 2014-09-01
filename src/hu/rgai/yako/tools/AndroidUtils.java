package hu.rgai.yako.tools;

import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.beens.FacebookAccount;
import hu.rgai.yako.beens.GmailAccount;
import hu.rgai.yako.beens.SmsAccount;
import hu.rgai.yako.messageproviders.FacebookMessageProvider;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.messageproviders.MessageProvider.Type;
import hu.rgai.yako.messageproviders.SimpleEmailMessageProvider;
import hu.rgai.yako.messageproviders.SmsMessageProvider;
import hu.rgai.yako.workers.ActiveConnectionConnector;
import hu.rgai.yako.workers.TimeoutAsyncTask;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents.Insert;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.QuickContactBadge;

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

  public static LinkedList<Integer> getIndexOfAccount(TreeSet<Account> accounts, LinkedList<Account> selectedAccounts) {
    
    LinkedList<Integer> indexs = new LinkedList<Integer>();
    int index = 0;
    
    for (Account a : accounts) {
      for(Account b : selectedAccounts) {
        if (a.equals(b)) {
          indexs.add(index);
          break;
          //return index;
        }
      }
      index++;
    }
    return indexs;
  }

  public static void checkAndConnectMessageProviderIfConnectable(MessageProvider mp, boolean isConnectionAlive, Context context) {
    if (mp.canBroadcastOnNewMessage() && !isConnectionAlive) {
      ActiveConnectionConnector connector = new ActiveConnectionConnector(mp, context);
      connector.executeTask(context, null);
    }
  }

  public static void stopReceiversForAccount(Account account, Context context) {
    MessageProvider provider = getMessageProviderInstanceByAccount(account,
        context);
    if (provider != null && provider.isConnectionAlive()) {
      Log.d("rgai", "Igen, dropping connection");
      provider.dropConnection(context);
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
    ConnectivityManager connectivityManager = (ConnectivityManager) c
        .getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }

  public static MessageProvider getMessageProviderInstanceByAccount(Account account, Context context) {
    MessageProvider mp = null;
    if (account instanceof GmailAccount) {
      mp = SimpleEmailMessageProvider.getInstance((GmailAccount)account);
    } else if (account instanceof EmailAccount) {
      mp = SimpleEmailMessageProvider.getInstance((EmailAccount) account);
    } else if (account instanceof FacebookAccount) {
      mp = new FacebookMessageProvider((FacebookAccount) account);
    } else if (account instanceof SmsAccount) {
      mp = new SmsMessageProvider(context);
    }
    return mp;
  }

  public static <T, U, V> void startAsyncTask(AsyncTask<T, U, V> at, T... args) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      at.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
    } else {
      at.execute(args);
    }
  }

  public static <T, U, V> void startTimeoutAsyncTask(TimeoutAsyncTask<T, U, V> at, T... args) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      at.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
    } else {
      at.execute(args);
    }
  }

  public static QuickContactBadge addToContact(Type type, Context mContext, ArrayList<String> contactDatas) {

    /**
     * ArrayList elements:
     * 
     * for Phone 1.Number
     * 
     * for Email 1.E-mail
     * 
     * for Gmail 1.E-mail
     * 
     * 
     * etc.
     */

    QuickContactBadge contactBadge = new QuickContactBadge(mContext);

    if (MessageProvider.Type.SMS == type) {

      contactBadge.assignContactFromPhone(contactDatas.get(0), true);

    } else if (MessageProvider.Type.GMAIL == type) {

      contactBadge.assignContactFromEmail(contactDatas.get(0), true);

    } else if (MessageProvider.Type.EMAIL == type) {

      contactBadge.assignContactFromEmail(contactDatas.get(0), true);

    }

    contactBadge.setMode(ContactsContract.QuickContact.MODE_MEDIUM);

    return contactBadge;

  }

  public static void addToFacebookContact(Context mContext, ArrayList<String> contactDatas) {

    /**
     * ArrayList elements:
     * 
     * 1. Name 2. Userid 3.Username
     */

    Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
    i.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);

    if (contactDatas.get(2) == null) {
      contactDatas.set(2, "Facebook name");
    }

    ArrayList<ContentValues> data = new ArrayList<ContentValues>();
    ContentValues row1 = new ContentValues();
    row1.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE);
    row1.put(ContactsContract.Data.DATA1, contactDatas.get(2));
    row1.put(ContactsContract.Data.DATA2, ContactsContract.CommonDataKinds.Im.TYPE_OTHER);
    row1.put(ContactsContract.Data.DATA5, ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM);
    row1.put(ContactsContract.Data.DATA6, "Facebook");
    row1.put(ContactsContract.Data.DATA10, contactDatas.get(1));
    data.add(row1);

    i.putExtra(Insert.NAME, contactDatas.get(0));
    i.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);

    mContext.startActivity(i);
  }

}
