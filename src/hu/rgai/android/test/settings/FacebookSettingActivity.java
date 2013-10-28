/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.rgai.android.test.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import com.facebook.Session;
import com.facebook.SessionState;
import hu.rgai.android.config.Settings;
import hu.rgai.android.intent.beens.account.FacebookSessionAccountAndr;
import hu.rgai.android.test.R;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookSettingActivity extends Activity {

  private EditText name;
  private EditText uniqueName;
  private Spinner messageAmount;
  private FacebookSessionAccountAndr oldAccount;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle); //To change body of generated methods, choose Tools | Templates.
    
    setContentView(R.layout.account_settings_facebook_layout);
    
    messageAmount = (Spinner)findViewById(R.id.initial_items_num);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
            R.array.initial_emails_num,
            android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // Apply the adapter to the spinner
    messageAmount.setAdapter(adapter);
    
    name = (EditText)findViewById(R.id.display_name);
    uniqueName = (EditText)findViewById(R.id.unique_name);
    name.setKeyListener(null);
    uniqueName.setKeyListener(null);
    
    
    Bundle b = getIntent().getExtras();
    if (b != null && b.getParcelable("account") != null) {
      oldAccount = (FacebookSessionAccountAndr)b.getParcelable("account");
      name.setText(oldAccount.getDisplayName());
      uniqueName.setText(oldAccount.getUniqueName());
      messageAmount.setSelection(AccountSettingsList.getSpinnerPosition(messageAmount.getAdapter(), oldAccount.getMessageLimit()));
    }
    
  }
  
  public void deleteAccountSettings(View v) {
    Log.d("rgai", "DELETE");
    
    Session.openActiveSession(this, true, new Session.StatusCallback() {

      public void call(Session sn, SessionState ss, Exception excptn) {
        if (sn.isOpened()) {
          Log.d("rgai", "Closing session and clearing token information");
          sn.closeAndClearTokenInformation();
        } else {
          Log.d("rgai", "Session was not opened...");
          sn.closeAndClearTokenInformation();
        }
      }
    });
    
    Intent resultIntent = new Intent();
    resultIntent.putExtra("old_account", (Parcelable)oldAccount);
    setResult(Settings.ActivityResultCodes.ACCOUNT_SETTING_DELETE, resultIntent);
    finish();
  }

  public void afterTextChanged(Editable e) {}
  public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

//  public MessageProvider.Type getType() {
//    return MessageProvider.Type.FACEBOOK;
//  }
//  
//  public FacebookAccountAndr getAccount() {
//    String m = email.getText().toString();
//    String p = pass.getText().toString();
//    int num = Integer.parseInt((String)messageAmount.getSelectedItem());
//    return new FacebookAccountAndr(num, m, p);
//  }
  
}
