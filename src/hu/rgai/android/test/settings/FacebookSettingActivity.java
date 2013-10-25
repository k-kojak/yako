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
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.test.R;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookSettingActivity extends Activity implements TextWatcher {

  private EditText email;
  private EditText pass;
  private Spinner messageAmount;
  private FacebookAccountAndr oldAccount;

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
    
    email = (EditText)findViewById(R.id.email_address);
    email.addTextChangedListener(this);
    pass = (EditText)findViewById(R.id.password);
    
    
    Bundle b = getIntent().getExtras();
    if (b != null && b.getParcelable("account") != null) {
      oldAccount = (FacebookAccountAndr)b.getParcelable("account");
      email.setText(oldAccount.getUserName());
      pass.setText(oldAccount.getPassword());
      messageAmount.setSelection(AccountSettingsList.getSpinnerPosition(messageAmount.getAdapter(), oldAccount.getMessageLimit()));
    }
    
  }
  
  public void onTextChanged(CharSequence text, int arg1, int arg2, int arg3) {
    AccountSettingsList.validateEmailField(email, text.toString());
  }
  
  public void saveAccountSettings(View v) {
    Log.d("rgai", "SAVE");
    
//    String 
    
//    FacebookAccountAndr newAccount = new FacebookAccountAndr(RESULT_OK, NFC_SERVICE, NFC_SERVICE);
    Intent resultIntent = new Intent();
//    resultIntent.putExtra("old_account", (Parcelable)oldAccount);
//    resultIntent.putExtra("new_account", (Parcelable)oldAccount);
      
    setResult(Activity.RESULT_OK, resultIntent);
    
    finish();
  }
  
  public void deleteAccountSettings(View v) {
    Log.d("rgai", "DELETE");
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
