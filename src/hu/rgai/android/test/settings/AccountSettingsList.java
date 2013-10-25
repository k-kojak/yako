
package hu.rgai.android.test.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import hu.rgai.android.config.Settings;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.adapter.AccountListAdapter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class AccountSettingsList extends Activity {

  boolean fbAdded = false;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.main);
    
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
      ListView lv = (ListView) findViewById(R.id.list);
      AccountListAdapter adapter = new AccountListAdapter(this, accounts);
      lv.setAdapter(adapter);
      lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          public void onItemClick(AdapterView<?> av, View arg1, int itemIndex, long arg3) {

            AccountAndr account = (AccountAndr) av.getItemAtPosition(itemIndex);

            Class classToLoad = Settings.getAccountTypeToSettingClass().get(account.getAccountType());
            if (classToLoad == null) {
              throw new RuntimeException("Account type does not have setting class.");
            }
            Intent i = new Intent(AccountSettingsList.this, classToLoad);

            // TODO: getFull message now always converted to FullEmailMessage

            i.putExtra("account", (Parcelable)account);
//            i.pu
            startActivity(i);
//            startActivityForResult(i, EMAIL_CONTENT_RESULT);
            
          }
        });
    }
    
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
//            addGmailAccountSettingTab(null);
            break;
          case 1:
            if (!fbAdded) {
//              addFacebookAccountSettingTab(null);
            } else {
//              addSimpleMailAccountSettingTab(null);
            }
            break;
          case 2:
//            addSimpleMailAccountSettingTab(null);
            break;
          default:
            break;
        }
      }
    });
    Dialog dialog = builder.create();
    
//    if (accountFragments.isEmpty()) {
//      dialog.setCancelable(false);
//    }
    
//    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//      public void onCancel(DialogInterface arg0) {
//        if (accountFragments.isEmpty()) {
//          showAccountTypeChooser();
//        }
//      }
//    });
    
    dialog.show();
    
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
  
  private boolean isFacebookAccountAdded() {
    return false;
  }
  

}
