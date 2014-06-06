
package hu.rgai.yako.tools;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents.Insert;
import android.util.Log;
import hu.rgai.yako.YakoApp;
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
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.yako.workers.ActiveConnectionConnector;
import hu.rgai.yako.workers.TimeoutAsyncTask;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

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

	public static void addToContact(Type type, Context mContext, ArrayList<String> contactDatas){

  	  /**
  	   * ArrayList elements:
  	   * 
  	   * for Phone
  	   * 1.Number
  	   * 
  	   * for Email 
  	   * 1.E-mail
  	   * 
	   * for Gmail 
  	   * 1.E-mail
  	   * 
  	   * etc.
  	   */

		Intent i = new Intent(Intent.ACTION_INSERT_OR_EDIT);
		i.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		if(MessageProvider.Type.SMS==type){

			i.putExtra(Insert.PHONE,contactDatas.get(0));

		}else if(MessageProvider.Type.GMAIL==type){


			i.putExtra(Insert.EMAIL,contactDatas.get(0));

		}else if(MessageProvider.Type.EMAIL==type){
			System.out.println("3");

			i.putExtra(Insert.EMAIL,contactDatas.get(0));
		}

		
		mContext.startActivity(i);

	}


}
