package hu.rgai.android.test.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.Session;
import hu.rgai.android.config.Settings;
import hu.rgai.android.errorlog.ErrorLog;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.services.MainService;
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
public class AccountSettingsList extends ActionBarActivity {

  boolean fbAdded = false;
  boolean stillAddingFacebookAccount = false;
  FacebookSettingActivity fbFragment = null;
  private MainService mainService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (stillAddingFacebookAccount) {
//      stillAddingFacebookAccount = false;
      return;
    }
    Log.d("rgai", "ON RESUME");
    setContentView(R.layout.main);

    List<AccountAndr> accounts = null;
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

          AccountAndr account = (AccountAndr) av.getItemAtPosition(itemIndex);

          Class classToLoad = Settings.getAccountTypeToSettingClass().get(account.getAccountType());
          if (classToLoad == null) {
            throw new RuntimeException("Account type does not have setting class.");
          }
          Intent i = new Intent(AccountSettingsList.this, classToLoad);

          // TODO: getFull message now always converted to FullEmailMessage

          i.putExtra("account", (Parcelable) account);
          startActivityForResult(i, Settings.ActivityRequestCodes.ACCOUNT_SETTING_RESULT);

        }
      });
    }
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
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    
    // Facebook session result
//    if (requestCode == )
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
          StoreHandler.addAccount(this, (AccountAndr) data.getParcelableExtra("new_account"));
        } else if (resultCode == Settings.ActivityResultCodes.ACCOUNT_SETTING_MODIFY) {
          StoreHandler.modifyAccount(this,
                  (AccountAndr) data.getParcelableExtra("old_account"),
                  (AccountAndr) data.getParcelableExtra("new_account"));
        } else if (resultCode == Settings.ActivityResultCodes.ACCOUNT_SETTING_DELETE) {
          StoreHandler.removeAccount(this, (AccountAndr) data.getParcelableExtra("old_account"));
          removeMessagesToAccount((AccountAndr) data.getParcelableExtra("old_account"));
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
  
  private void removeMessagesToAccount(final AccountAndr acc) {
    
    ServiceConnection serviceConnection = new ServiceConnection() {
      public void onServiceConnected(ComponentName className, IBinder binder) {
        mainService = ((MainService.MyBinder) binder).getService();
        mainService.removeMessagesToAccount(acc);
        unbindService(this);
      }
      public void onServiceDisconnected(ComponentName className) {
        mainService = null;
        unbindService(this);
      }
    };
    bindService(new Intent(this, MainService.class), serviceConnection, Context.BIND_AUTO_CREATE);
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
        Class classToLoad = null;
        switch (which) {
          case 0:
            classToLoad = GmailSettingActivity.class;
            break;
          case 1:
            if (fbAdded) {
              classToLoad = SimpleEmailSettingActivity.class;
            } else {
              classToLoad = FacebookSettingActivity.class;
            }
            break;
          case 2:
            classToLoad = SimpleEmailSettingActivity.class;
            break;
          default:
            break;
        }
//        if (classToLoad == FacebookSettingActivity.class) {
//          Session.openActiveSession(AccountSettingsList.this, true, new Session.StatusCallback() {
//
//            public void call(Session sn, SessionState ss, Exception excptn) {
//              stillAddingFacebookAccount = true;
//              if (sn.isOpened()) {
//                ErrorLog.dumpLogcat(AccountSettingsList.this, ErrorLog.Reason.FB_CONTACT_SYNC, 0, null, "Session is opened after openActiveSession");
//                Request.newMeRequest(sn, new Request.GraphUserCallback() {
//                  public void onCompleted(GraphUser gu, Response rspns) {
//                    if (gu != null) {
//                      ErrorLog.dumpLogcat(AccountSettingsList.this, ErrorLog.Reason.FB_CONTACT_SYNC, 0, null, "GraphUser != null, getting friend list");
//                      Toast.makeText(AccountSettingsList.this, "Updating contacts with facebook ids.", Toast.LENGTH_LONG).show();
//                      stillAddingFacebookAccount = false;
//                      FacebookSessionAccountAndr fbsa = new FacebookSessionAccountAndr(10, gu.getName(), gu.getUsername());
//                      FacebookIntegratorAsyncTask integrator = new FacebookIntegratorAsyncTask(AccountSettingsList.this, new IntegrationHandler(AccountSettingsList.this));
//                      integrator.execute(fbsa);
//                      try {
//                        StoreHandler.addAccount(AccountSettingsList.this, fbsa);
//                        AccountSettingsList.this.onResume();
//                      } catch (Exception ex) {
//                        Logger.getLogger(AccountSettingsList.class.getName()).log(Level.SEVERE, null, ex);
//                      }
//                    } else {
//                      ErrorLog.dumpLogcat(AccountSettingsList.this, ErrorLog.Reason.FB_CONTACT_SYNC, 200, null, "GraphUser IS null, getting friend list");
//                      Log.d("rgai", "GRAPH USER IS NULL");
//                    }
//                  }
//                }).executeAsync();
//              } else {
//                ErrorLog.dumpLogcat(AccountSettingsList.this, ErrorLog.Reason.FB_CONTACT_SYNC, 200, null, "Session is NOT opened after openActiveSession");
//  //              Log.d("rgai", sn.toString());
//  //              Log.d("rgai", ss.toString());
//  //              
//  //              Log.d("rgai", "HELLOOOOOOOOOOOOOOOOOOOOOOO IS NOT OPENED");
//              }
//            }
//          });
//        } else {
//        if (classToLoad == FacebookSettingFragment.class) {
//          fbFragment = new FacebookSettingFragment();
//          getSupportFragmentManager().beginTransaction().add(android.R.id.content, fbFragment).commit();
//        } else {
          Intent i = new Intent(AccountSettingsList.this, classToLoad);
          startActivityForResult(i, Settings.ActivityRequestCodes.ACCOUNT_SETTING_RESULT);
//        }
//        }
      }
    });
    Dialog dialog = builder.create();

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
  
//  private class FacebookIntegratorAsyncTask extends AsyncTask<FacebookSessionAccount, Integer, String> {
//
//    Handler handler;
////    FacebookAccount account;
//    private Activity activity;
//    
//    public FacebookIntegratorAsyncTask(Activity activity, Handler handler) {
//      this.activity = activity;
//      this.handler = handler;
////      this.account = account;
//    }
//    
//    @Override
//    protected String doInBackground(FacebookSessionAccount... params) {
//      String content = null;
//      
//      FacebookFriendProvider fbfp = new FacebookFriendProvider();
//      fbfp.getFacebookFriends(activity);
//
//      return content;
//    }
//
//    @Override
//    protected void onPostExecute(String result) {
//      Message msg = handler.obtainMessage();
//      Bundle bundle = new Bundle();
//      bundle.putString("content", "1");
//      msg.setData(bundle);
//      handler.sendMessage(msg);
//    }
//
//
////    @Override
////    protected void onProgressUpdate(Integer... values) {
////      Log.d(Constants.LOG, "onProgressUpdate");
////      Message msg = handler.obtainMessage();
////      Bundle bundle = new Bundle();
////
////      bundle.putInt("progress", values[0]);
////      msg.setData(bundle);
////      handler.sendMessage(msg);
////    }
//  }
  
}
