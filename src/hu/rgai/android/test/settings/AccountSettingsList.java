package hu.rgai.android.test.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.facebook.Session;
import hu.rgai.android.beens.Account;
import hu.rgai.android.beens.EmailAccount;
import hu.rgai.android.beens.MainServiceExtraParams;
import hu.rgai.android.config.Settings;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.services.schedulestarters.MainScheduler;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.YakoApp;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.AndroidUtils;
import hu.rgai.android.tools.ParamStrings;
import hu.rgai.android.tools.adapter.AccountListAdapter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AccountSettingsList extends ActionBarActivity {

  boolean fbAdded = false;
  boolean stillAddingFacebookAccount = false;
  FacebookSettingActivity fbFragment = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onBackPressed() {
    EventLogger.INSTANCE.writeToLogFile( EventLogger.LOGGER_STRINGS.ACCOUNTSETTING.ACCOUNT_SETTINGS_LIST_BACKBUTTON_STR, true);
    super.onBackPressed();
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    if (stillAddingFacebookAccount) {
//      stillAddingFacebookAccount = false;
      return;
    }
//    Log.d("rgai", "ON RESUME");
    setContentView(R.layout.main);
    
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);

    List<Account> accounts = null;
    try {
      accounts = StoreHandler.getAccounts(this);
      Log.d("rgai", accounts.toString());
    } catch (Exception ex) {
      // TODO: handle exception
      ex.printStackTrace();
      Log.d("rgai", "TODO: handle exception");
    }

    if (accounts == null || accounts.isEmpty()) {
      showAccountTypeChooser();
    } else {
      ListView lv = (ListView) findViewById(R.id.list);
      AccountListAdapter adapter = new AccountListAdapter(this, accounts);
      lv.setAdapter(adapter);
      lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View arg1, int itemIndex, long arg3) {

          Account account = (Account) av.getItemAtPosition(itemIndex);

          Class classToLoad = Settings.getAccountTypeToSettingClass().get(account.getAccountType());
          if (classToLoad == null) {
            throw new RuntimeException("Account type does not have setting class.");
          }
          if (isInfMail(account)) {
            classToLoad = InfEmailSettingActivity.class;
          }
          Intent i = new Intent(AccountSettingsList.this, classToLoad);

          i.putExtra("account", (Parcelable) account);
          startActivityForResult(i, Settings.ActivityRequestCodes.ACCOUNT_SETTING_RESULT);

        }
      });
    }
  }
  
  private boolean isInfMail(Account acc) {
    if (acc instanceof EmailAccount) {
      EmailAccount ea = (EmailAccount)acc;
      if (ea.getImapAddress().equals(InfEmailSettingActivity.IMAP_ADDRESS)) {
        return true;
      }
    }
    return false;
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.account_options_menu, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    switch (item.getItemId()) {
      case R.id.add_account:
        showAccountTypeChooser();
        return true;
      case android.R.id.home:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    
    // Facebook session result
    super.onActivityResult(requestCode, resultCode, data);
    try {
      Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
      // thrown when someone else returns here, and not facebook
    } catch (RuntimeException ex) {
      Log.d("rgai", "catching FB exception");
      ex.printStackTrace();
    }
    
    if (requestCode == Settings.ActivityRequestCodes.ACCOUNT_SETTING_RESULT) {
      stillAddingFacebookAccount = false;
      try {
        if (resultCode == Settings.ActivityResultCodes.ACCOUNT_SETTING_NEW) {
          Account newAccount = (Account) data.getParcelableExtra("new_account");
          StoreHandler.addAccount(this, newAccount);
          getMessagesToNewAccount(newAccount, this);
        } else if (resultCode == Settings.ActivityResultCodes.ACCOUNT_SETTING_MODIFY) {
          Account oldAccount = (Account) data.getParcelableExtra("old_account");
          Account newAccount = (Account) data.getParcelableExtra("new_account");
          
          StoreHandler.modifyAccount(this, oldAccount, newAccount);
          
          getMessagesToNewAccount(newAccount, this);
          AndroidUtils.stopReceiversForAccount(oldAccount, this);
        } else if (resultCode == Settings.ActivityResultCodes.ACCOUNT_SETTING_DELETE) {
          Account oldAccount = (Account) data.getParcelableExtra("old_account");
          StoreHandler.removeAccount(this, (Account) data.getParcelableExtra("old_account"));
          YakoApp.removeMessagesToAccount(oldAccount);
          
          AndroidUtils.stopReceiversForAccount(oldAccount, this);
        } else if (resultCode == Settings.ActivityResultCodes.ACCOUNT_SETTING_CANCEL) {
          // do nothing
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        // TODO: handle exception
        Log.d("rgai", "TODO: handle exception");
      }
    }
  }
  
  private void getMessagesToNewAccount(Account account, Context context) {
    Intent service = new Intent(context, MainScheduler.class);
    service.setAction(Context.ALARM_SERVICE);
    
    MainServiceExtraParams eParams = new MainServiceExtraParams();
    eParams.setAccount(account);
    eParams.setForceQuery(true);
    service.putExtra(ParamStrings.EXTRA_PARAMS, eParams);
    
    this.sendBroadcast(service);
  }
  
  private void showAccountTypeChooser() {

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Choose account type");

    String[] items;
    fbAdded = isFacebookAccountAdded();
    if (fbAdded) {
      items = new String[]{getString(R.string.account_name_gmail), getString(R.string.account_name_infemail),
        getString(R.string.account_name_simplemail)};
    } else {
      items = new String[]{getString(R.string.account_name_gmail), getString(R.string.account_name_facebook),
        getString(R.string.account_name_infemail), getString(R.string.account_name_simplemail)};
    }

    builder.setItems(items, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        Class classToLoad = null;
        switch (which) {
          case 0:
            classToLoad = GmailSettingActivity.class;
            break;
          case 1:
            if (fbAdded) {
              classToLoad = InfEmailSettingActivity.class;
            } else {
              classToLoad = FacebookSettingActivity.class;
            }
            break;
          case 2:
            if (fbAdded) {
              classToLoad = SimpleEmailSettingActivity.class;
            } else {
              classToLoad = InfEmailSettingActivity.class;
            }
            break;
          case 3:
            classToLoad = SimpleEmailSettingActivity.class;
            break;
          default:
            break;
        }
        Intent i = new Intent(AccountSettingsList.this, classToLoad);
        startActivityForResult(i, Settings.ActivityRequestCodes.ACCOUNT_SETTING_RESULT);
      }
    });
    Dialog dialog = builder.create();

    dialog.show();

  }

  public static void validateEmailField(TextView tv, String text) {
    validatePatternAndShowErrorOnField(tv, text,
            Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9\\-]+)*(\\.[A-Za-z]{2,})$"));

  }

  public static void validateUriField(TextView tv, String text) {
    validatePatternAndShowErrorOnField(tv, text,
            Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$"));
  }

  protected static void validatePatternAndShowErrorOnField(TextView tv, String text, Pattern p) {
    Matcher matcher = p.matcher(text);
    if (!matcher.matches()) {
      tv.setError("Invalid email address");
    } else {
      tv.setError(null);
    }
  }

  public static void showHidePassword(boolean showPass, EditText passField) {
    int s, e;
    s = passField.getSelectionStart();;
    e = passField.getSelectionEnd();
    if (showPass) {
      passField.setTransformationMethod(null);
    } else {
      passField.setTransformationMethod(new PasswordTransformationMethod());
    }
    passField.setSelection(s, e);
  }
  
  public static int getSpinnerPosition(SpinnerAdapter adapter, int value) {
    int ind = 0;
    for (int i = 0; i < adapter.getCount(); i++) {
      if (adapter.getItem(i).toString().equals(value + "")) {
        return i;
      }
    }

    return ind;
  }

  public static int getSpinnerPosition(SpinnerAdapter adapter, String value) {
    int ind = 0;
    for (int i = 0; i < adapter.getCount(); i++) {
      if (adapter.getItem(i).toString().equals(value + "")) {
        return i;
      }
    }

    return ind;
  }

  private boolean isFacebookAccountAdded() {
    return StoreHandler.isFacebookAccountAdded(this);
  }
  
}
