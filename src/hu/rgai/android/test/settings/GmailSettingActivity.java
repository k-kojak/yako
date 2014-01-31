package hu.rgai.android.test.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import hu.rgai.android.config.Settings;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.intent.beens.account.GmailAccountAndr;
import hu.rgai.android.test.R;
import java.util.regex.Pattern;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class GmailSettingActivity extends ActionBarActivity implements TextWatcher {

  private static final String GMAIL_SETTING_ACTIVITY_BACKBUTTON_STR = "GmailSettingActivity:backbutton";
  private EditText email;
  private EditText pass;
  private Spinner messageAmount;
  private GmailAccountAndr oldAccount = null;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle); //To change body of generated methods, choose Tools | Templates.
  
    setContentView(R.layout.account_settings_gmail_layout);
    
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    
    messageAmount = (Spinner)findViewById(R.id.initial_emails_num);
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
      oldAccount = (GmailAccountAndr)b.getParcelable("account");
      email.setText(oldAccount.getEmail());
      pass.setText(oldAccount.getPassword());
      messageAmount.setSelection(AccountSettingsList.getSpinnerPosition(messageAmount.getAdapter(), oldAccount.getMessageLimit()));
    }
  }
    
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      // Inflate the menu items for use in the action bar
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.edit_account_options_menu, menu);
      return super.onCreateOptionsMenu(menu);
  }
  

  @Override
  public void onBackPressed() {
    Log.d( "willrgai", GMAIL_SETTING_ACTIVITY_BACKBUTTON_STR);
    EventLogger.INSTANCE.writeToLogFile( GMAIL_SETTING_ACTIVITY_BACKBUTTON_STR, true );
    super.onBackPressed();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      // Handle presses on the action bar items
      switch (item.getItemId()) {
          case R.id.edit_account_save:
        	  saveAccountSettings();
              return true;
          case R.id.edit_account_delete:
        	  deleteAccountSettings();
              return true;
          default:
              return super.onOptionsItemSelected(item);
      }
  }
  
  public void saveAccountSettings() {
    Log.d("rgai", "SAVE");
    
    String m = email.getText().toString();
    String p = pass.getText().toString();
    int messageLimit = Integer.parseInt((String)messageAmount.getSelectedItem());
    GmailAccountAndr newAccount = new GmailAccountAndr(messageLimit, m, p);
    
    Intent resultIntent = new Intent();
    resultIntent.putExtra("new_account", (Parcelable)newAccount);
    
    // If editing account, then old account exists
    if (oldAccount != null) {
      resultIntent.putExtra("old_account", (Parcelable)oldAccount);
      setResult(Settings.ActivityResultCodes.ACCOUNT_SETTING_MODIFY, resultIntent);
    }
    // If new account...
    else {
      resultIntent.putExtra("old_account", false);
      setResult(Settings.ActivityResultCodes.ACCOUNT_SETTING_NEW, resultIntent);
    }
    
    finish();
  }
  
  public void deleteAccountSettings() {
    Log.d("rgai", "DELETE");
    
    Intent resultIntent = new Intent();
    resultIntent.putExtra("old_account", (Parcelable)oldAccount);
    setResult(Settings.ActivityResultCodes.ACCOUNT_SETTING_DELETE, resultIntent);
    
    finish();
  }
  
  public void onTextChanged(CharSequence text, int arg1, int arg2, int arg3) {
    validateGmailField(email, text.toString());
  }

  private void validateGmailField(TextView tv, String text) {
    if (text.contains("@")) {
      AccountSettingsList.validatePatternAndShowErrorOnField(tv, text,
              Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"));
    }

  }

  public void afterTextChanged(Editable e) {}
  public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

//  public MessageProvider.Type getType() {
//    return MessageProvider.Type.GMAIL;
//  }
//
//  public AccountAndr getAccount() {
//    String m = email.getText().toString();
//    String p = pass.getText().toString();
//    int num = Integer.parseInt((String)messageAmount.getSelectedItem());
//    return new GmailAccountAndr(num, m, p);
//  }
  
}
