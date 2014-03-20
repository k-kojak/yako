/*
 * TODO: if logged in via FB webview , but passwd not provided to the form (for jabber)
 * it can cause problems
 */
package hu.rgai.android.test.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.ProfilePictureView;
import hu.rgai.android.asynctasks.FacebookIntegratorAsyncTask;

import hu.rgai.android.config.Settings;
import hu.rgai.android.errorlog.ErrorLog;
import hu.rgai.android.eventlogger.EventLogger;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.store.StoreHandler;
import hu.rgai.android.test.R;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tamas Kojedzinszky
 
 */
public class FacebookSettingActivity extends ActionBarActivity {

private static final String FACEBOOK_SETTING_ACTIVITY_BACKBUTTON_STR = "FacebookSettingActivity:backbutton";
  private Menu menu;
  private ProfilePictureView profilePictureView;
  private TextView name;
  private String uniqueName;
  private EditText password;
  private String id = null;
  private Spinner messageAmount;
  private FacebookAccountAndr oldAccount = null;
  private UiLifecycleHelper uiHelper;
  private GraphUser user;
  private Session.StatusCallback callback = new Session.StatusCallback() {
    @Override
    public void call(Session session, SessionState state, Exception exception) {
      Log.d("rgai", "Session status CALLBACK CALLED");
      onSessionStateChange(session, state, exception);
    }
  };
  
  @Override
  public void onBackPressed() {
    Log.d( "willrgai", FACEBOOK_SETTING_ACTIVITY_BACKBUTTON_STR);
    EventLogger.INSTANCE.writeToLogFile( FACEBOOK_SETTING_ACTIVITY_BACKBUTTON_STR, true);
    super.onBackPressed();
  }
  
  private void onSessionStateChange(Session session, SessionState state, Exception exception) {
    Log.d("rgai", "FacebookSettingActivity.onSessionStateChange");
    Log.d("rgai", "state.isOpened -> " + state.isOpened());
    Log.d("rgai", "session.isOpened -> " + session.isOpened());
    if (state.isOpened()) {
      if (session.isOpened()) {
        Log.d("rgai", "STORING FB Session view storehandler");
        StoreHandler.storeFacebookAccessToken(FacebookSettingActivity.this, session.getAccessToken(), session.getExpirationDate());
        
//            ErrorLog.dumpLogcat(FacebookSettingActivity.this, ErrorLog.Reason.FB_CONTACT_SYNC, 0, null, "Session is opened after openActiveSession");
        Request.newMeRequest(session, new Request.GraphUserCallback() {
          public void onCompleted(GraphUser gu, Response rspns) {
            Log.d("rgai", "NEW ME REQUEST ON COMPLETE");
            if (gu != null) {
              Log.d("rgai", "onSessionStateChange gu != null");
              if (user == null) {
                user = gu;
              }
              syncFacebookContactList(null);
              try {
              setFieldsByAccount(gu.getName(), gu.getUsername(), null, gu.getId(), -1);
              } catch (Exception ex) {
                Logger.getLogger(FacebookSettingActivity.class.getName()).log(Level.SEVERE, null, ex);
              }
            } else {
//              ErrorLog.dumpLogcat(FacebookSettingActivity.this, ErrorLog.Reason.FB_CONTACT_SYNC, 200, null, "GraphUser IS null, getting friend list");
              Log.d("rgai", "GRAPH USER IS NULL");
            }
          }
        }).executeAsync();
      } 
      
    } else if (state.isClosed()) {
      Log.i("rgai", "Logged out...");
    }
    
    updateUI();
  
    
  }

  private void updateUI() {
    Log.d("rgai", "updateUI() ");
//      Session session = Session.getActiveSession();
    if (user != null) {
      profilePictureView.setProfileId(user.getId());
    } else {
      profilePictureView.setProfileId(null);
    }
    updateLayoutAndOptionsMenu(menu);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.account_settings_facebook_layout);
//    final String fbToken = StoreHandler.getFacebookAccessToken(this);
//    
//    if (fbToken != null) {
//      Session.openActiveSessionWithAccessToken(this,
//              AccessToken.createFromExistingAccessToken(fbToken, new Date(2014, 1, 1), new Date(2013, 1, 1), AccessTokenSource.FACEBOOK_APPLICATION_NATIVE, Settings.getFacebookPermissions()),
//              new StatusCallback() {
//        public void call(Session sn, SessionState ss, Exception excptn) {
//          Log.d("rgai", "REOPENING SESSION WITH ACCESS TOKEN -> " + fbToken);
//          Log.d("rgai", sn.toString());
//          Log.d("rgai", ss.toString());
//          
//        }
//      });
//    }
    
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
   
    profilePictureView = (ProfilePictureView) findViewById(R.id.profilePicture);       
     
    messageAmount = (Spinner)findViewById(R.id.initial_items_num);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
            R.array.initial_emails_num,
            android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // Apply the adapter to the spinner
    messageAmount.setAdapter(adapter);
    
    name = (TextView)findViewById(R.id.display_name);
//    uniqueName = (TextView)findViewById(R.id.unique_name);
    password = (EditText)findViewById(R.id.password);
    
    Bundle b = getIntent().getExtras();
    if (b != null && b.getParcelable("account") != null) {
      oldAccount = (FacebookAccountAndr)b.getParcelable("account");
      setFieldsByAccount(oldAccount);
    }
    
    LoginButton lb = (LoginButton)findViewById(R.id.authButton);
    lb.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
      @Override
      public void onUserInfoFetched(GraphUser user) {
        boolean prevUserState = FacebookSettingActivity.this.user != null;
        FacebookSettingActivity.this.user = user;
        Log.d("rgai", "ON USER FETCH USER IS -> " + user);
        Log.d("rgai", "PREV USER STATE -> " + prevUserState);
        // User previously existed and we pressed logout, so now user is null
        if (user == null && prevUserState == true) {
          deleteAccountSettings(false);
        } else {
          updateUI();
        }
      }
    });
    
    lb.setReadPermissions(Settings.getFacebookPermissions());
//    lb.performClick();
    uiHelper = new UiLifecycleHelper(this, callback);
    uiHelper.onCreate(savedInstanceState);
    

  }

  private void setFieldsByAccount(FacebookAccountAndr fba) {
    if (fba != null) {
      setFieldsByAccount(fba.getDisplayName(), fba.getUniqueName(), fba.getPassword(), fba.getId(), fba.getMessageLimit());
    }
  }
  
  private void setFieldsByAccount(String displayName, String uName, String pass, String id, int messageLimit) {
    if (displayName != null) {
      name.setText(displayName);
    }
    if (pass != null) {
      password.setText(pass);
    }
    if (messageLimit != -1) {
      messageAmount.setSelection(AccountSettingsList.getSpinnerPosition(messageAmount.getAdapter(), messageLimit));
    }
    this.uniqueName = uName;
    this.id = id;
  }
  
  public void onCheckboxClicked(View v) {
    boolean checked = ((CheckBox)v).isChecked();
    AccountSettingsList.showHidePassword(checked, password);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      // Inflate the menu items for use in the action bar
    Log.d("rgai", "ON CREATE OPTIONS MENU");
    this.menu = menu;
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.account_options_facebook_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }
  
  @Override
  public boolean onPrepareOptionsMenu (Menu menu) {
    Log.d("rgai", "ON PREPARE OPTIONS MENU");
    super.onPrepareOptionsMenu(menu);
    updateLayoutAndOptionsMenu(menu);
    return true;
  }
  
  private void updateLayoutAndOptionsMenu(Menu menu) {
    if (menu != null) {
      this.menu.findItem(R.id.edit_account_save).setVisible(user != null);
      this.menu.findItem(R.id.edit_account_delete).setVisible(oldAccount != null);
      this.menu.findItem(R.id.edit_account_cancel).setVisible(oldAccount == null);
      
      findViewById(R.id.profilePicture).setVisibility(user == null ? View.GONE : View.VISIBLE);
      findViewById(R.id.display_name).setVisibility(user == null ? View.GONE : View.VISIBLE);
//      findViewById(R.id.unique_name).setVisibility(user == null && oldAccount == null ? View.GONE : View.VISIBLE);
      findViewById(R.id.password).setVisibility(user == null ? View.GONE : View.VISIBLE);
      findViewById(R.id.show_pass).setVisibility(user == null ? View.GONE : View.VISIBLE);
      findViewById(R.id.initial_items_num).setVisibility(user == null ? View.GONE : View.VISIBLE);
      findViewById(R.id.initial_items_num_label).setVisibility(user == null ? View.GONE : View.VISIBLE);
      findViewById(R.id.sync_fb_contact_list).setVisibility(user == null ? View.GONE : View.VISIBLE);
      
      if (user != null && oldAccount == null) {
        ((TextView)findViewById(R.id.display_name)).setText(user.getName());
//        ((TextView)findViewById(R.id.unique_name)).setText(user.getUsername());
        this.id = user.getId();
      }
    }
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
      case R.id.edit_account_cancel:
        finish();
        return true;
      case android.R.id.home:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
  
  public void syncFacebookContactList(View view) {
    if (user != null) {
      if (FacebookIntegratorAsyncTask.isRunning) {
        Toast.makeText(this, "Synchronization is running", Toast.LENGTH_LONG).show();
      } else {
        Toast.makeText(FacebookSettingActivity.this, "Syncing contact list...", Toast.LENGTH_LONG).show();
        FacebookIntegratorAsyncTask integrator = new FacebookIntegratorAsyncTask(FacebookSettingActivity.this,
                new IntegrationHandler(FacebookSettingActivity.this));
        integrator.execute(user.getId());
      }
    } else {
      Toast.makeText(this, "Facebook session problem", Toast.LENGTH_LONG).show();
    }
  }
  
  
  

  @Override
  public void onResume() {
    super.onResume();
    
//    Session session = Session.getActiveSession();
//    if (session != null && (session.isOpened() || session.isClosed())) {
//      onSessionStateChange(session, session.getState(), null);
//    }
//    uiHelper.onResume();
    Log.d("rgai", "onResume");
    updateUI();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Log.d("rgai", "ON ACT. RESULT");
    
    uiHelper.onActivityResult(requestCode, resultCode, data);
    Session session = Session.getActiveSession();
    Log.d("rgai", "onActivity result session -> " + session);
    if (session != null && (session.isOpened() || session.isClosed())) {
      onSessionStateChange(session, session.getState(), null);
    }
//    uiHelper.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
//    uiHelper.onPause();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
//    uiHelper.onDestroy();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
//    uiHelper.onSaveInstanceState(outState);
  }

  public void saveAccountSettings() {
    int messageLimit = Integer.parseInt((String)messageAmount.getSelectedItem());
    FacebookAccountAndr newAccount = new FacebookAccountAndr(messageLimit,
            name.getText().toString(), uniqueName, id,
            password.getText().toString());
    Log.d("rgai", "SAVING ACCOUNT -> " + newAccount.toString());
    Log.d("rgai", "id->" + id);
    
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
    deleteAccountSettings(true);
  }
  
  public void deleteAccountSettings(boolean allowLoginUI) {
    Log.d("rgai", "deleteAccountSettings...");
    
    Session.openActiveSession(this, allowLoginUI, new Session.StatusCallback() {

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
    
    StoreHandler.clearFacebookAccessToken(this);
    
    Intent resultIntent = new Intent();
    resultIntent.putExtra("old_account", (Parcelable)oldAccount);
    setResult(Settings.ActivityResultCodes.ACCOUNT_SETTING_DELETE, resultIntent);
    finish();
  }
  
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

  public interface ToastHelper {
    public void showToast(String msg);
  }
  
}
