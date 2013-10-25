package hu.rgai.android.test.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import hu.rgai.android.beens.fbintegrate.FacebookIntegrateItem;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.EmailAccountAndr;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.intent.beens.account.GmailAccountAndr;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.MyService;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.FacebookFriendProvider;
import hu.rgai.android.tools.FacebookIdSaver;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Tamas Kojedzinszky
 * @deprecated 
 */
public class AccountSettings extends SherlockFragmentActivity {

  Map<Tab, TabListener> tabToTablistener = null;
  List<SherlockFragment> accountFragments = null;
  Tab tab;
  ActionBar actionBar;
  int i = 1;
  boolean fbAdded = false;
  
  private MyService s;
  private ServiceConnection serviceConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder binder) {
      s = ((MyService.MyBinder) binder).getService();
    }

    public void onServiceDisconnected(ComponentName className) {
      s = null;
    }
  };
  
  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    
    // bindig main service
    bindService(new Intent(this, MyService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    
    accountFragments = new LinkedList<SherlockFragment>();
    tabToTablistener = new HashMap<Tab, TabListener>();
    // Create the actionbar
    actionBar = getSupportActionBar();
    Log.d("rgai", actionBar.toString());
 
    // Hide Actionbar Icon
    actionBar.setDisplayShowHomeEnabled(false);

    // Hide Actionbar Title
    actionBar.setDisplayShowTitleEnabled(false);

    // Create Actionbar Tabs
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    List<AccountAndr> accounts = null;
    try {
      accounts = StoreHandler.getAccounts(this);
      Log.d("rgai", accounts.toString());
    } catch (Exception ex) {
      // TODO: handle exception
      Log.d("rgai", "TODO: handle exception");
    }
    
    if (accounts == null || accounts.isEmpty()) {
      showAccountTypeChooser();
    } else {
      for (AccountAndr a : accounts) {
        if (a instanceof GmailAccountAndr) {
          addGmailAccountSettingTab((GmailAccountAndr)a);
        } else if (a instanceof FacebookAccountAndr) {
          addFacebookAccountSettingTab((FacebookAccountAndr)a);
        } else if (a instanceof EmailAccountAndr) {
          addSimpleMailAccountSettingTab((EmailAccountAndr)a);
        }
        // select first tab after loading all setting tabs
        actionBar.getTabAt(0).select();
      }
    }

    setContentView(R.layout.account_settings_layout);
    
//    // setting up spinner
//    
//    SharedPreferences sharedPref = getSharedPreferences(getString(R.string.settings_email_file_key), Context.MODE_PRIVATE);
//    String email = sharedPref.getString(getString(R.string.settings_saved_email), "");
//    String pass = sharedPref.getString(getString(R.string.settings_saved_pass), "");
//    String imap = sharedPref.getString(getString(R.string.settings_saved_imap), "");
//    int initial_num = sharedPref.getInt(getString(R.string.settings_saved_initial_email_num), 10);
//    
//    ((EditText)findViewById(R.id.email_address)).setText(email);
//    ((EditText)findViewById(R.id.password)).setText(pass);
//    ((EditText)findViewById(R.id.imap_server)).setText(imap);
//    ((Spinner)findViewById(R.id.initial_emails_num)).setSelection(adapter.getPosition("" + initial_num));
    
    // ToDo add your GUI initialization code here        
  }

  @Override
  protected void onPause() {
    super.onPause();
    unbindService(serviceConnection);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getSupportMenuInflater();
    inflater.inflate(R.menu.account_options_menu, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.add_account:
        showAccountTypeChooser();
        break;
      case R.id.remove_account:
        Tab act = actionBar.getSelectedTab();
        if (act != null) {
          TabListener tl = tabToTablistener.remove(act);
          removeAccount(tl.mFragment);
          actionBar.removeTab(act);
        }
        break;
      default:
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  private void showAccountTypeChooser() {
    
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Choose account type");
    
    String[] items;
    fbAdded = isFacebookAccountAdded();
    if (fbAdded) {
      items = new String[]{getString(R.string.account_name_gmail), getString(R.string.account_name_simplemail)};
    } else {
      items = new String[]{getString(R.string.account_name_gmail), getString(R.string.account_name_facebook), getString(R.string.account_name_simplemail)};
    }
    
    builder.setItems(items, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        
        switch(which) {
          case 0:
            addGmailAccountSettingTab(null);
            break;
          case 1:
            if (!fbAdded) {
              addFacebookAccountSettingTab(null);
            } else {
              addSimpleMailAccountSettingTab(null);
            }
            break;
          case 2:
            addSimpleMailAccountSettingTab(null);
            break;
          default:
            break;
        }
      }
    });
    Dialog dialog = builder.create();
    
    if (accountFragments.isEmpty()) {
      dialog.setCancelable(false);
    }
    
//    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//      public void onCancel(DialogInterface arg0) {
//        if (accountFragments.isEmpty()) {
//          showAccountTypeChooser();
//        }
//      }
//    });
    
    dialog.show();
    
  }
  
  private boolean isFacebookAccountAdded() {
    boolean fbPresent = false;
    for (Tab t : tabToTablistener.keySet()) {
      if (t.getText().equals(getString(R.string.account_name_facebook))) {
        fbPresent = true;
        break;
      }
    }
    return fbPresent;
  }
  
  private void addGmailAccountSettingTab(GmailAccountAndr ga) {
    // TODO: implement special gmail account type
    TabListener tl = new TabListener<GmailSettingFragment>(this, getString(R.string.account_name_gmail), GmailSettingFragment.class, ga);
    tab = actionBar.newTab().setTabListener(tl);
    tab.setText(R.string.account_name_gmail);
//    tab.setIcon(R.drawable.simple_mail_inactive);
    actionBar.addTab(tab);
    // select tab even if just loading account,
    // because if selected, the tab listener adds the tab to the account storing, so
    // it will be saved
    tab.select();
    addListenerToTab(tab, tl);
  }
  
  private void addSimpleMailAccountSettingTab(EmailAccountAndr ea) {
    TabListener tl = new TabListener(this, getString(R.string.account_name_simplemail), SimpleEmailSettingFragment.class, ea);
    tab = actionBar.newTab().setTabListener(tl);
    tab.setText(R.string.account_name_simplemail);
//    tab.setIcon(R.drawable.simple_mail_inactive);
    actionBar.addTab(tab);
    tab.select();
    addListenerToTab(tab, tl);
  }
  
  private void addFacebookAccountSettingTab(FacebookAccountAndr fa) {
    TabListener tl = new TabListener<FacebookSettingFragment>(this, getString(R.string.account_name_facebook), FacebookSettingFragment.class, fa);
    tab = actionBar.newTab().setTabListener(tl);
    tab.setText(R.string.account_name_facebook);
//    tab.setIcon(R.drawable.simple_mail_inactive);
    actionBar.addTab(tab);
    tab.select();
    addListenerToTab(tab, tl);
  }
  
  private void addListenerToTab(Tab tab, TabListener tabListener) {
    tabToTablistener.put(tab, tabListener);
    Log.d("rgai", "tabToTablistener map size -> " + tabToTablistener.size());
    Log.d("rgai", "tabToTablistener keys -> " + tabToTablistener.keySet());
  }
  
  protected synchronized void addAccount(SherlockFragment acc) {
    accountFragments.add(acc);
  }
  
  @Override
  public void finish() {
    setResult(Activity.RESULT_OK);
    super.finish();
  }
  
  protected synchronized void removeAccount(SherlockFragment acc) {
    int ind = accountFragments.indexOf(acc);
    if (ind != -1) {
      SettingFragment sf = (SettingFragment)accountFragments.get(ind);
      AccountAndr andrAcc = sf.getAccount();
      s.removeElementsFromList(andrAcc);
//      s.re
      accountFragments.remove(ind);
      if (accountFragments.isEmpty()) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.detach(acc);
        ft.commit();
        showAccountTypeChooser();
      }
    }
    saveAccountSettings(null);
  }
  
  /**
   * Save button pressed at preferences.
   * 
   * @param view 
   */
//  public void saveEmailSettings(View view) {
//    switch(view.getId()) {
//      case R.id.save_account_button:
//        // saving email, pass, imap settings
//        String email = ((EditText)findViewById(R.id.email_address)).getText().toString();
//        String pass = ((EditText)findViewById(R.id.password)).getText().toString();
//        String imap = ((EditText)findViewById(R.id.imap_server)).getText().toString();
//        int initial_num = Integer.parseInt((String)(((Spinner)findViewById(R.id.initial_emails_num)).getSelectedItem()));
//        
//        Context context = this;
//        SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.settings_email_file_key), Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPref.edit();
//        editor.putString(getString(R.string.settings_saved_email), email);
//        editor.putString(getString(R.string.settings_saved_pass), pass);
//        editor.putString(getString(R.string.settings_saved_imap), imap);
//        editor.putInt(getString(R.string.settings_saved_initial_email_num), initial_num);
//        editor.commit();
//        
//        finish();
//        break;
//    }
//  }

  public static int getSpinnerPosition(SpinnerAdapter adapter, int value) {
    int ind = 0;
    for (int i = 0; i < adapter.getCount(); i++) {
//      Log.d("rgai", adapter.getItem(i).toString() + " vs " + value);
      if (adapter.getItem(i).toString().equals(value+"")) {
        return i;
      }
    }
    
    return ind;
  }
  
  public static int getSpinnerPosition(SpinnerAdapter adapter, String value) {
    int ind = 0;
    for (int i = 0; i < adapter.getCount(); i++) {
      if (adapter.getItem(i).toString().equals(value+"")) {
        return i;
      }
    }
    
    return ind;
  }
  
  public static void validateEmailField(TextView tv, String text) {
    validatePatternAndShowErrorOnField(tv, text,
            Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"));
    
  }
  
  public static void validateUriField(TextView tv, String text) {
    validatePatternAndShowErrorOnField(tv, text,
            Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$"));
  }
  
  private static void validatePatternAndShowErrorOnField(TextView tv, String text, Pattern p) {
    Matcher matcher = p.matcher(text);
    if (!matcher.matches()) {
      tv.setError("Invalid email address");
    } else {
      tv.setError(null);
    }
  }
  
  public void saveAccountSettings(View v) {
    Log.d("rgai", v == null ? "v is null" : "v is NOT null");
    List<AccountAndr> accounts = new LinkedList<AccountAndr>();
    for (SherlockFragment sf : accountFragments) {
      AccountAndr a = ((SettingFragment)sf).getAccount();
      if (a != null) {
        accounts.add(a);
        if (a.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
          // Downloading facebook friends and integrating them to contact list
//          handler = new IntegrationHandler();
          FacebookIntegratorAsyncTask integrator = new FacebookIntegratorAsyncTask(this, null);
          integrator.execute((FacebookAccount)a);
        }
      }
    }
    try {
      StoreHandler.saveAccounts(accounts, this);
    } catch (Exception ex) {
      ex.printStackTrace();
      Log.d("rgai", "TODO: handle exception");
    }
    if (v != null) {
      finish();
    }
  }
  
  private class IntegrationHandler extends Handler {
    
    @Override
    public void handleMessage(Message msg) {
      Bundle bundle = msg.getData();
      if (bundle != null) {
        if (bundle.get("content") != null) {
//          content = bundle.getString("content");
//          webView.loadData(content, "text/html", mailCharCode);
//          webView.loadDataWithBaseURL(null, content, "text/html", mailCharCode, null);
//          displayMessage(content);
//          if (pd != null) {
//            pd.dismiss();
//          }
        }
      }
    }
  }
  
  private class FacebookIntegratorAsyncTask extends AsyncTask<FacebookAccount, Integer, String> {

//    Handler handler;
//    FacebookAccount account;
    private Context context;
    
    public FacebookIntegratorAsyncTask(Context context, Handler handler) {
      this.context = context;
//      this.handler = handler;
//      this.account = account;
    }
    
    @Override
    protected String doInBackground(FacebookAccount... params) {
      String content = null;
      
      FacebookFriendProvider fbfp = new FacebookFriendProvider(params[0]);
      List<FacebookIntegrateItem> fbi = fbfp.getFacebookFriends();

      FacebookIdSaver fbs = new FacebookIdSaver();
      for (FacebookIntegrateItem fbii : fbi) {
        fbs.integrate(context, fbii);
      }
      
      return content;
    }

    @Override
    protected void onPostExecute(String result) {
//      Message msg = handler.obtainMessage();
//      Bundle bundle = new Bundle();
//      bundle.putString("content", result);
//      msg.setData(bundle);
//      handler.sendMessage(msg);
    }


//    @Override
//    protected void onProgressUpdate(Integer... values) {
//      Log.d(Constants.LOG, "onProgressUpdate");
//      Message msg = handler.obtainMessage();
//      Bundle bundle = new Bundle();
//
//      bundle.putInt("progress", values[0]);
//      msg.setData(bundle);
//      handler.sendMessage(msg);
//    }
  }
  
}
