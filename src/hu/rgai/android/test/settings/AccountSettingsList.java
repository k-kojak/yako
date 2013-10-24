
package hu.rgai.android.test.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.adapter.AccountListAdapter;
import java.util.List;

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
  
  private boolean isFacebookAccountAdded() {
    return false;
  }
  

}
