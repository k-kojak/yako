/*
 * TODO: if logged in via FB webview , but passwd not provided to the form (for jabber)
 * it can cause problems
 */
package hu.rgai.yako.view.activities;

import android.content.Intent;
import android.os.Bundle;
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

import hu.rgai.yako.workers.FacebookIntegratorAsyncTask;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.eventlogger.EventLogger;
import hu.rgai.yako.eventlogger.EventLogger.LogFilePaths;
import hu.rgai.yako.beens.FacebookAccount;
import hu.rgai.yako.store.StoreHandler;
import hu.rgai.android.test.R;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tamas Kojedzinszky
 
 */
public class FacebookSettingActivity extends ActionBarActivity {

  private Menu menu;
  private ProfilePictureView profilePictureView;
  private TextView name;
  private String uniqueName;
  private EditText password;
  private String id = null;
  private FacebookAccount oldAccount = null;
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
    EventLogger.INSTANCE.writeToLogFile( LogFilePaths.FILE_TO_UPLOAD_PATH, EventLogger.LOGGER_STRINGS.ACCOUNTSETTING.FACEBOOK_SETTING_ACTIVITY_BACKBUTTON_STR, true);
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
              setFieldsByAccount(gu.getName(), gu.getUsername(), null, gu.getId());
              } catch (Exception ex) {
                Log.d("rgai", "", ex);
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

    
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
   
    
    profilePictureView = (ProfilePictureView) findViewById(R.id.profilePicture);       
     
    
    name = (TextView)findViewById(R.id.display_name);
//    uniqueName = (TextView)findViewById(R.id.unique_name);
    password = (EditText)findViewById(R.id.password);
    
    Bundle b = getIntent().getExtras();
    if (b != null && b.getParcelable("instance") != null) {
      oldAccount = (FacebookAccount)b.getParcelable("instance");
      setFieldsByAccount(oldAccount);
    }
    
    LoginButton lb = (LoginButton)findViewById(R.id.authButton);
    lb.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
      @Override
      public void onUserInfoFetched(GraphUser user) {
        boolean prevUserState = FacebookSettingActivity.this.user != null;
        FacebookSettingActivity.this.user = user;
//        Log.d("rgai", "ON USER FETCH USER IS -> " + user);
//        Log.d("rgai", "PREV USER STATE -> " + prevUserState);
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

  private void setFieldsByAccount(FacebookAccount fba) {
    if (fba != null) {
      setFieldsByAccount(fba.getDisplayName(), fba.getUniqueName(), fba.getPassword(), fba.getId());
    }
  }
  
  private void setFieldsByAccount(String displayName, String uName, String pass, String id) {
    if (displayName != null) {
      name.setText(displayName);
    }
    if (pass != null) {
      password.setText(pass);
    }
    this.uniqueName = uName;
    this.id = id;
  }
  
  public void onCheckboxClicked(View v) {
    boolean checked = ((CheckBox)v).isChecked();
    AccountSettingsListActivity.showHidePassword(checked, password);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    this.menu = menu;
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.account_options_facebook_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }
  
  @Override
  public boolean onPrepareOptionsMenu (Menu menu) {
//    Log.d("rgai", "ON PREPARE OPTIONS MENU");
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
      
      findViewById(R.id.password).setVisibility(user == null ? View.GONE : View.VISIBLE);
      findViewById(R.id.show_pass).setVisibility(user == null ? View.GONE : View.VISIBLE);
      findViewById(R.id.sync_fb_contact_list).setVisibility(user == null ? View.GONE : View.VISIBLE);
      
      if (user != null && oldAccount == null) {
        ((TextView)findViewById(R.id.display_name)).setText(user.getName());
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
        FacebookIntegratorAsyncTask integrator = new FacebookIntegratorAsyncTask(FacebookSettingActivity.this);
        integrator.executeTask(this, new String[]{user.getId()});
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
    FacebookAccount newAccount = new FacebookAccount(name.getText().toString(), uniqueName, id,
            password.getText().toString());
    Log.d("rgai", "SAVING ACCOUNT -> " + newAccount.toString());
    Log.d("rgai", "id->" + id);
    
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
    deleteAccountSettings(true);
  }
  
  public void deleteAccountSettings(boolean allowLoginUI) {
    Log.d("rgai", "deleteAccountSettings...");
    
    Session.openActiveSession(this, allowLoginUI, new Session.StatusCallback() {

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
    
    StoreHandler.clearFacebookAccessToken(this);
    
    Intent resultIntent = new Intent();
    resultIntent.putExtra("old_account", (Parcelable)oldAccount);
    setResult(Settings.ActivityResultCodes.ACCOUNT_SETTING_DELETE, resultIntent);
    finish();
  }
  
  public interface ToastHelper {
    public void showToast(String msg);
  }
  
}
