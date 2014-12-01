package hu.rgai.yako.view.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.beens.GmailAccount;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.EventLogger.LogFilePaths;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.tools.AndroidUtils;
import hu.rgai.yako.view.extensions.ZoneDisplayActionBarActivity;
import hu.rgai.yako.workers.TimeoutAsyncTask;

import javax.mail.MessagingException;
import java.util.regex.Pattern;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class GmailSettingActivity extends ZoneDisplayActionBarActivity implements TextWatcher {

  private EditText email;
  private EditText pass;
  private GmailAccount oldAccount = null;
  private ProgressDialog mProgDialog = null;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle, true, false, false);
  
    setContentView(R.layout.account_settings_gmail_layout);
    
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    
    
    email = (EditText)findViewById(R.id.email_address);
    email.addTextChangedListener(this);
    pass = (EditText)findViewById(R.id.password);
    
    Bundle b = getIntent().getExtras();
    if (b != null && b.getParcelable("instance") != null) {
      oldAccount = b.getParcelable("instance");
      email.setText(oldAccount.getEmail());
      pass.setText(oldAccount.getPassword());
    }
  }
  
  public void onCheckboxClicked(View v) {
    boolean checked = ((CheckBox)v).isChecked();
    AccountSettingsListActivity.showHidePassword(checked, pass);
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
    EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, EventLogger.LOGGER_STRINGS.ACCOUNTSETTING.GMAIL_SETTING_ACTIVITY_BACKBUTTON_STR, true );
    super.onBackPressed();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
      // Handle presses on the action bar items
    switch (item.getItemId()) {
      case R.id.edit_account_save:
        saveAccountPressed();
        return true;
      case R.id.edit_account_delete:
        deleteAccountSettings();
        return true;
      case android.R.id.home:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void saveAccountPressed() {
    String m = email.getText().toString();
    String p = pass.getText().toString();
    GmailAccount newAccount = new GmailAccount(m, p);

    MessageProvider mp = AndroidUtils.getMessageProviderInstanceByAccount(newAccount, this);

    ConnectionTester ct = new ConnectionTester(mp, newAccount);
    AndroidUtils.startTimeoutAsyncTask(ct, new Void[]{});

    if (mProgDialog == null) {
      mProgDialog = new ProgressDialog(this);
      mProgDialog.setCancelable(false);
    }
    mProgDialog.show();
    mProgDialog.setContentView(R.layout.progress_dialog);
  }

  private void showLessSecureDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
      }
    });
    builder.setPositiveButton("Change settings", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/settings/security/lesssecureapps"));
        startActivity(browserIntent);
      }
    });
    builder.setTitle("Error");
    builder.setMessage("Connection blocked by Google.\n" +
            "You have to turn off less secure app protection at:\n"
            +"https://www.google.com/settings/security/lesssecureapps").show();
  }
  
  private void saveAccountSettings(EmailAccount newAccount) {
    
    Intent resultIntent = new Intent();
    resultIntent.putExtra("new_account", (Parcelable)newAccount);
    
    // If editing instance, then old instance exists
    if (oldAccount != null) {
      resultIntent.putExtra("old_account", (Parcelable)oldAccount);
      setResult(Settings.ActivityResultCodes.ACCOUNT_SETTING_MODIFY, resultIntent);
    }
    // If new instance...
    else {
      resultIntent.putExtra("old_account", false);
      setResult(Settings.ActivityResultCodes.ACCOUNT_SETTING_NEW, resultIntent);
    }
    
    finish();
  }
  
  public void deleteAccountSettings() {
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
      AccountSettingsListActivity.validatePatternAndShowErrorOnField(tv, text,
              Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"));
    }

  }

  public void afterTextChanged(Editable e) {}
  public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

  private class ConnectionTester extends TimeoutAsyncTask<Void, Void, String> {

    private final MessageProvider mMessageProvider;
    private final EmailAccount mAccount;

    public ConnectionTester(MessageProvider mp, EmailAccount account) {
      super(null);
      mMessageProvider = mp;
      mAccount = account;
    }

    @Override
    protected String doInBackground(Void... params) {
      String errorString = null;
      try {
        mMessageProvider.testConnection();
      } catch (MessagingException e) {
        errorString = e.getMessage();
        Log.d("yako", "", e);
      }
      return errorString;
    }

    @Override
    protected void onPostExecute(String errorMsg) {
      mProgDialog.dismiss();
      if (errorMsg == null) {
        saveAccountSettings(mAccount);
      } else {
        errorMsg = errorMsg.toLowerCase();
        if (errorMsg.contains("[alert] please log in via your web browser:")) {
          showLessSecureDialog();
        }
      }
    }
  }

}
