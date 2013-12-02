/*
 * TODO: if logged in via FB webview , but passwd not provided to the form (for jabber)
 * it can cause problems
 */
package hu.rgai.android.test.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.Builder;
import com.facebook.Session.OpenRequest;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import hu.rgai.android.config.Settings;
import hu.rgai.android.errorlog.ErrorLog;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.test.R;
import hu.rgai.android.tools.FacebookFriendProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookSessionAccount;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tamas Kojedzinszky
 * @deprecated
 */
public class FacebookSettingActivity_depr extends Activity {

//  boolean stillAddingFacebookAccount = false;
  private TextView name;
  private TextView uniqueName;
  private EditText password;
  private String id = null;
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

    name = (TextView)findViewById(R.id.display_name);
    uniqueName = (TextView)findViewById(R.id.unique_name);
    password = (EditText)findViewById(R.id.password);
    name.setKeyListener(null);
    uniqueName.setKeyListener(null);

    
    Bundle b = getIntent().getExtras();
    if (b != null && b.getParcelable("account") != null) {
      oldAccount = (FacebookAccountAndr)b.getParcelable("account");
      setFieldsByAccount(oldAccount);
//      name.setText(oldAccount.getDisplayName());
//      uniqueName.setText(oldAccount.getUniqueName());
//      messageAmount.setSelection(AccountSettingsList.getSpinnerPosition(messageAmount.getAdapter(), oldAccount.getMessageLimit()));
    } else {
      Session.openActiveSession(this, true, new Session.StatusCallback() {
        public void call(Session sn, SessionState ss, Exception excptn) {
          Log.d("rgai", "SESSION -> " + (sn != null ? sn.toString() : "NULL"));
          Log.d("rgai", "SESSIONSTATE -> " + (ss != null ? ss.toString() : "NULL"));
          Log.d("rgai", "EXCEPTION -> " + (excptn != null ? excptn.getMessage() : "NULL"));
//          stillAddingFacebookAccount = true;
          if (sn.isOpened()) {
            ErrorLog.dumpLogcat(FacebookSettingActivity_depr.this, ErrorLog.Reason.FB_CONTACT_SYNC, 0, null, "Session is opened after openActiveSession");
            Request.newMeRequest(sn, new Request.GraphUserCallback() {
              public void onCompleted(GraphUser gu, Response rspns) {
                if (gu != null) {
                  ErrorLog.dumpLogcat(FacebookSettingActivity_depr.this, ErrorLog.Reason.FB_CONTACT_SYNC, 0, null, "GraphUser != null, getting friend list");
                  Toast.makeText(FacebookSettingActivity_depr.this, "Updating contacts with facebook ids.", Toast.LENGTH_LONG).show();
//                  stillAddingFacebookAccount = false;
//                  FacebookSessionAccountAndr fbsa = new FacebookSessionAccountAndr(10, gu.getName(), gu.getUsername());
                  FacebookIntegratorAsyncTask integrator = new FacebookIntegratorAsyncTask(FacebookSettingActivity_depr.this,
                          new IntegrationHandler(FacebookSettingActivity_depr.this));
                  integrator.execute();
                  try {
//                    StoreHandler.addAccount(FacebookSettingActivity.this, fbsa);
                    setFieldsByAccount(gu.getName(), gu.getUsername(), null, -1);
//                    FacebookSettingActivity.this.onResume();
                  } catch (Exception ex) {
                    Logger.getLogger(FacebookSettingActivity_depr.class.getName()).log(Level.SEVERE, null, ex);
                  }
                } else {
                  ErrorLog.dumpLogcat(FacebookSettingActivity_depr.this, ErrorLog.Reason.FB_CONTACT_SYNC, 200, null, "GraphUser IS null, getting friend list");
                  Log.d("rgai", "GRAPH USER IS NULL");
                }
              }
            }).executeAsync();
          } else {
            ErrorLog.dumpLogcat(FacebookSettingActivity_depr.this, ErrorLog.Reason.FB_CONTACT_SYNC, 200, null, "Session is NOT opened after openActiveSession");
//              Log.d("rgai", sn.toString());
//              Log.d("rgai", ss.toString());
//              
//              Log.d("rgai", "HELLOOOOOOOOOOOOOOOOOOOOOOO IS NOT OPENED");
          }
        }
      });
    }
  }
  
//  private static Session openActiveSession(Activity activity, boolean allowLoginUI, StatusCallback callback, List<String> permissions) {
//    OpenRequest openRequest = new OpenRequest(activity).setPermissions(permissions).setCallback(callback);
//    Session session = new Builder(activity).build();
//    if (SessionState.CREATED_TOKEN_LOADED.equals(session.getState()) || allowLoginUI) {
//        Session.setActiveSession(session);
//        session.openForRead(openRequest);
//        return session;
//    }
//    return null;
//  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data); //To change body of generated methods, choose Tools | Templates.
//    Log.d("rgai", "ON ACTIVITY RESULT -> " + requestCode + ", " + resultCode);
    if (resultCode == Activity.RESULT_OK && Session.getActiveSession() != null) {
      try {
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        // thrown when someone else returns here, and not facebook
      } catch (RuntimeException ex) {
        Log.d("rgai", "catching FB exception");
        ex.printStackTrace();
      }
    } else {
      setResult(Settings.ActivityResultCodes.ACCOUNT_SETTING_CANCEL);
      finish();
    }
  }
  
  public void saveAccountSettings(View v) {
    int messageLimit = Integer.parseInt((String)messageAmount.getSelectedItem());
    FacebookAccountAndr newAccount = new FacebookAccountAndr(messageLimit,
            name.getText().toString(), uniqueName.getText().toString(), id,
            password.getText().toString());
    
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
  
  private void setFieldsByAccount(FacebookAccountAndr fba) {
    if (fba != null) {
      setFieldsByAccount(fba.getDisplayName(), fba.getUniqueName(), fba.getPassword(), fba.getMessageLimit());
    }
  }
  
  private void setFieldsByAccount(String displayName, String uName, String p, int messageLimit) {
    if (displayName != null) {
      name.setText(displayName);
    }
    if (uniqueName != null) {
      uniqueName.setText(uName);
    }
    if (p != null) {
      password.setText(p);
    }
    if (messageLimit != -1) {
      messageAmount.setSelection(AccountSettingsList.getSpinnerPosition(messageAmount.getAdapter(), messageLimit));
    }
  }

  public void deleteAccountSettings(View v) {
    Log.d("rgai", "DELETE");
    
    Session.openActiveSession(this, true, new Session.StatusCallback() {

      public void call(Session sn, SessionState ss, Exception excptn) {
        if (sn.isOpened()) {
          Log.d("rgai", "Closing session and clearing token information");
          sn.closeAndClearTokenInformation();
//          sn.
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

  private class IntegrationHandler extends Handler {
    
    private Context c;
    
    public IntegrationHandler(Context c) {
      this.c = c;
    }
    
    @Override
    public void handleMessage(Message msg) {
      Bundle bundle = msg.getData();
      if (bundle != null) {
        if (bundle.get("content") != null) {
          Toast.makeText(c, "Update complete.", Toast.LENGTH_LONG).show();
          ErrorLog.dumpLogcat(c, ErrorLog.Reason.FB_CONTACT_SYNC, 0, null, "Updating contact list done");
        }
      }
    }
  }
  
  private class FacebookIntegratorAsyncTask extends AsyncTask<FacebookSessionAccount, Integer, String> {

    Handler handler;
//    FacebookAccount account;
    private Activity activity;
    
    public FacebookIntegratorAsyncTask(Activity activity, Handler handler) {
      this.activity = activity;
      this.handler = handler;
//      this.account = account;
    }
    
    @Override
    protected String doInBackground(FacebookSessionAccount... params) {
      String content = null;
      
      FacebookFriendProvider fbfp = new FacebookFriendProvider();
//      fbfp.getFacebookFriends(activity);

      return content;
    }

    @Override
    protected void onPostExecute(String result) {
      Message msg = handler.obtainMessage();
      Bundle bundle = new Bundle();
      bundle.putString("content", "1");
      msg.setData(bundle);
      handler.sendMessage(msg);
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
