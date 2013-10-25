package hu.rgai.android.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.EmailAccountAndr;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.intent.beens.account.GmailAccountAndr;
import hu.rgai.android.test.R;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;

import java.util.LinkedList;
import java.util.List;


/**
 *
 * @author Tamas Kojedzinszky
 */
public class StoreHandler {
  
  public static void modifyAccount(Context context, AccountAndr oldAccount, AccountAndr newAccount) throws Exception {
    List<AccountAndr> accounts = getAccounts(context);
    if (accounts.contains(oldAccount)) {
      accounts.remove(oldAccount);
      accounts.add(newAccount);
      saveAccounts(accounts, context);
    }
  }
  
  public static void removeAccount(Context context, AccountAndr account) throws Exception {
    List<AccountAndr> accounts = getAccounts(context);
    if (accounts.contains(account)) {
      accounts.remove(account);
      saveAccounts(accounts, context);
    }
  }
  
  public static void addAccount(Context context, AccountAndr account) throws Exception {
    List<AccountAndr> accounts = getAccounts(context);
    if (!accounts.contains(account)) {
      accounts.add(account);
      saveAccounts(accounts, context);
    }
  }
  
  public static boolean isFacebookAccountAdded(Context context) {
    List<AccountAndr> accounts = getAccounts(context);
    
    for (AccountAndr a : accounts) {
      if (a.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
        return true;
      }
    }
    return false;
  }
  
  public static void saveAccounts(List<AccountAndr> accounts, Context context) throws Exception {
    removeAccountSettings(context);
    int i = 0;
    SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.settings_accounts), Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(context.getString(R.string.settings_accounts_size), accounts.size());
    for(AccountAndr a : accounts) {
      if (a.getAccountType() == MessageProvider.Type.GMAIL) {
        GmailAccountAndr ga = (GmailAccountAndr) a;
        editor.putString(context.getString(R.string.settings_accounts_item_type) + "_" + i, context.getString(R.string.account_name_gmail));
        editor.putString(context.getString(R.string.settings_accounts_item_name) + "_" + i, ga.getEmail());
        editor.putString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, ga.getPassword());
        editor.putString(context.getString(R.string.settings_accounts_item_imap) + "_" + i, ga.getImapAddress());
        editor.putString(context.getString(R.string.settings_accounts_item_smtp) + "_" + i, ga.getSmtpAddress());
//        editor.putBoolean(context.getString(R.string.settings_accounts_item_ssl) + "_" + i, ga.isSsl());
        editor.putInt(context.getString(R.string.settings_accounts_item_amount) + "_" + i, ga.getMessageLimit());
      } else if (a.getAccountType() == MessageProvider.Type.FACEBOOK) {
        FacebookAccountAndr fa = (FacebookAccountAndr) a;
        editor.putString(context.getString(R.string.settings_accounts_item_type) + "_" + i, context.getString(R.string.account_name_facebook));
        editor.putString(context.getString(R.string.settings_accounts_item_name) + "_" + i, fa.getUserName());
        editor.putString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, fa.getPassword());
        editor.putInt(context.getString(R.string.settings_accounts_item_amount) + "_" + i, fa.getMessageLimit());
      } else if (a.getAccountType() == MessageProvider.Type.EMAIL) {
        EmailAccountAndr ea = (EmailAccountAndr) a;
        editor.putString(context.getString(R.string.settings_accounts_item_type) + "_" + i, context.getString(R.string.account_name_simplemail));
        editor.putString(context.getString(R.string.settings_accounts_item_name) + "_" + i, ea.getEmail());
        editor.putString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, ea.getPassword());
        editor.putString(context.getString(R.string.settings_accounts_item_imap) + "_" + i, ea.getImapAddress());
        editor.putString(context.getString(R.string.settings_accounts_item_smtp) + "_" + i, ea.getSmtpAddress());
        editor.putBoolean(context.getString(R.string.settings_accounts_item_ssl) + "_" + i, ea.isSsl());
        editor.putInt(context.getString(R.string.settings_accounts_item_amount) + "_" + i, ea.getMessageLimit());
      } else {
        throw new Exception("Unsupported account type: " + a.getAccountType());
      }
      i++;
    }
    editor.commit();
  }
  
  private static void removeAccountSettings(Context context) throws Exception {
    SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.settings_accounts), Context.MODE_PRIVATE);
    int amount = prefs.getInt(context.getString(R.string.settings_accounts_size), -1);
    SharedPreferences.Editor editor = prefs.edit();
    for (int i = 0; i < amount; i++) {
      String type = prefs.getString(context.getString(R.string.settings_accounts_item_type) + "_" + i, null);
      if (type.equals(context.getString(R.string.account_name_gmail))) {
        editor.remove(context.getString(R.string.settings_accounts_item_type) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_name) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_pass) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_imap) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_smtp) + "_" + i);
//        editor.remove(context.getString(R.string.settings_accounts_item_ssl) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_amount) + "_" + i);
      } else if (type.equals(context.getString(R.string.account_name_facebook))) {
        editor.remove(context.getString(R.string.settings_accounts_item_type) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_name) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_pass) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_amount) + "_" + i);
      } else if (type.equals(context.getString(R.string.account_name_simplemail))) {
        editor.remove(context.getString(R.string.settings_accounts_item_type) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_name) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_pass) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_imap) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_smtp) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_ssl) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_amount) + "_" + i);
      } else {
        throw new Exception("Unsupported account type: " + type);
      }
    }
    editor.commit();
  }
  
  public static List<AccountAndr> getAccounts(Context context) {
    List<AccountAndr> accounts = new LinkedList<AccountAndr>();
    SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.settings_accounts), Context.MODE_PRIVATE);
    int amount = prefs.getInt(context.getString(R.string.settings_accounts_size), -1);
    for (int i = 0; i < amount; i++) {
      String type = prefs.getString(context.getString(R.string.settings_accounts_item_type) + "_" + i, "");
      if (type.equals(context.getString(R.string.account_name_gmail))) {
        String email = prefs.getString(context.getString(R.string.settings_accounts_item_name) + "_" + i, null);
        String pass = prefs.getString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, null);
//        boolean ssl = prefs.getBoolean(context.getString(R.string.settings_accounts_item_ssl) + "_" + i, true);
        int num = prefs.getInt(context.getString(R.string.settings_accounts_item_amount) + "_" + i, 5);
        accounts.add(new GmailAccountAndr(num, email, pass));
      } else if (type.equals(context.getString(R.string.account_name_facebook))) {
        String email = prefs.getString(context.getString(R.string.settings_accounts_item_name) + "_" + i, null);
        String pass = prefs.getString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, null);
        int num = prefs.getInt(context.getString(R.string.settings_accounts_item_amount) + "_" + i, 5);
        accounts.add(new FacebookAccountAndr(num, email, pass));
      } else if (type.equals(context.getString(R.string.account_name_simplemail))) {
        String email = prefs.getString(context.getString(R.string.settings_accounts_item_name) + "_" + i, null);
        String pass = prefs.getString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, null);
        String imap = prefs.getString(context.getString(R.string.settings_accounts_item_imap) + "_" + i, null);
        String smtp = prefs.getString(context.getString(R.string.settings_accounts_item_smtp) + "_" + i, null);
        boolean ssl = prefs.getBoolean(context.getString(R.string.settings_accounts_item_ssl) + "_" + i, true);
        int num = prefs.getInt(context.getString(R.string.settings_accounts_item_amount) + "_" + i, 5);
        accounts.add(new EmailAccountAndr(email, pass, imap, smtp, ssl, num));
      } else {
        Toast.makeText(context, "Unsupported account type: " + type, Toast.LENGTH_LONG);
      }
    }
    return accounts;
  }
}
