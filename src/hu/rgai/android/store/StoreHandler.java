package hu.rgai.android.store;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import hu.rgai.android.config.Settings;
import hu.rgai.android.intent.beens.account.AccountAndr;
import hu.rgai.android.intent.beens.account.EmailAccountAndr;
import hu.rgai.android.intent.beens.account.FacebookAccountAndr;
import hu.rgai.android.intent.beens.account.GmailAccountAndr;
import hu.rgai.android.test.R;
import hu.rgai.android.test.settings.SystemPreferences;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Tamas Kojedzinszky
 */
public class StoreHandler {
  
  private static Bitmap fbImgMe = null;
  private static final String DATE_FORMAT = "EEE MMM dd kk:mm:ss z yyyy";
  private static final String LAST_NOTIFICATION_DATES_FILENAME = "yako_lastNotDatesFile";
  
  public static class SystemSettings {
    
    public static boolean isNotificationTurnedOn(Context context) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      Boolean not = prefs.getBoolean(SystemPreferences.KEY_PREF_NOTIFICATION, true);
      return not;
    }
    
    public static boolean isNotificationSoundTurnedOn(Context context) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      Boolean not = prefs.getBoolean(SystemPreferences.KEY_PREF_NOTIFICATION_SOUND, true);
      return not;
    }
    
    public static boolean isNotificationVibrationTurnedOn(Context context) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      Boolean not = prefs.getBoolean(SystemPreferences.KEY_PREF_NOTIFICATION_VIBRATION, true);
      return not;
    }
    
  }
  
  
  public static void writeLastNotificationObject(Context context, HashMap<AccountAndr, Date> map) {
    if (map != null) {
      FileOutputStream fos = null;
      ObjectOutputStream oos = null;
      try {
        fos = context.openFileOutput(LAST_NOTIFICATION_DATES_FILENAME, Context.MODE_PRIVATE);
        oos = new ObjectOutputStream(fos);
        oos.writeObject(map);
        oos.flush();
      } catch (FileNotFoundException ex) {
        Logger.getLogger(StoreHandler.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(StoreHandler.class.getName()).log(Level.SEVERE, null, ex);
      } finally {
        try {
          oos.close();
          fos.close();
        } catch (IOException ex) {
          Logger.getLogger(StoreHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
  }
  
  public static HashMap<AccountAndr, Date> readLastNotificationObject(Context context) {
    HashMap<AccountAndr, Date> lastNotDates = null;
    ObjectInputStream ois = null;
    FileInputStream fis = null;
    if (context.getFileStreamPath(LAST_NOTIFICATION_DATES_FILENAME).exists()) {
      try {
        fis = context.openFileInput(LAST_NOTIFICATION_DATES_FILENAME);
        ois = new ObjectInputStream(fis);
        lastNotDates = (HashMap<AccountAndr, Date>)ois.readObject();
        ois.close();
        fis.close();
      } catch (FileNotFoundException ex) {
        Logger.getLogger(StoreHandler.class.getName()).log(Level.SEVERE, null, ex);
      } catch (OptionalDataException ex) {
        Logger.getLogger(StoreHandler.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(StoreHandler.class.getName()).log(Level.SEVERE, null, ex);
      } catch (ClassNotFoundException ex) {
        Logger.getLogger(StoreHandler.class.getName()).log(Level.SEVERE, null, ex);
      } finally {
        try {
          ois.close();
        } catch (IOException ex) {
          Logger.getLogger(StoreHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {
          Logger.getLogger(StoreHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
//      Log.d("rgai", "READ LAST NOT DATES: " + lastNotDates.toString());
    }
    return lastNotDates;
  }
  
  
  
  public static void saveUserFbImage(Context context, Bitmap bitmap) {
    if (bitmap != null) {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
      byte[] byteArray = stream.toByteArray();
      saveByteArray(context, byteArray, Settings.FACEBOOK_ME_IMG_FOLDER, Settings.FACEBOOK_ME_IMG_NAME);
    }
  }
  
  public static Bitmap getUserFbImage(Context context) {
    if (fbImgMe == null) {
      byte[] data = getByteArray(context, Settings.FACEBOOK_ME_IMG_FOLDER + "/" + Settings.FACEBOOK_ME_IMG_NAME);
      if (data != null) {
        fbImgMe = BitmapFactory.decodeByteArray(data, 0, data.length);
      }
    }
    
    return fbImgMe;
  }
  
  private static byte[] getByteArray(Context context, String file) {
    byte[] data = null;
    try {
      ContextWrapper cw = new ContextWrapper(context);
      File mainFilePath = cw.getDir("media", Context.MODE_PRIVATE);
      mainFilePath = new File(mainFilePath, file);
      if (mainFilePath.isFile()) {
        FileInputStream in = new FileInputStream(mainFilePath);
        data = new byte[(int)mainFilePath.length()];
        in.read(data, 0, (int)mainFilePath.length());
        in.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return data;
  }
  
  private static void saveByteArray(Context context, byte[] data, String folder, String filename) {

    try {
      ContextWrapper cw = new ContextWrapper(context);
      File mainFilePath = cw.getDir("media", Context.MODE_PRIVATE);
      mainFilePath = new File(mainFilePath, folder);
      if (!mainFilePath.isDirectory()) {
        mainFilePath.mkdirs();
      }
      Log.d("rgai", mainFilePath.getAbsolutePath());
      File file = new File(mainFilePath, filename);
      file.createNewFile();
      FileOutputStream outs = new FileOutputStream(file);
      outs.write(data);
      outs.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public static void storeFacebookAccessToken(Context context, String token, Date expirationDate) {
    SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.settings_accounts), Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(context.getString(R.string.settings_fb_access_token), token);
    
    SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
    editor.putString(context.getString(R.string.settings_fb_access_token_exp_date), df.format(expirationDate));
    Log.d("rgai", df.format(expirationDate));
    
    editor.commit();
  }
  
  public static void clearFacebookAccessToken(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.settings_accounts), Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.remove(context.getString(R.string.settings_fb_access_token));
    editor.remove(context.getString(R.string.settings_fb_access_token_exp_date));
    editor.commit();
  }
  
  public static String getFacebookAccessToken(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.settings_accounts), Context.MODE_PRIVATE);
    String token = prefs.getString(context.getString(R.string.settings_fb_access_token), null);
    return token;
  }
  
  public static Date getFacebookAccessTokenExpirationDate(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.settings_accounts), Context.MODE_PRIVATE);
    Date token = new Date();
    try {
      token = new SimpleDateFormat(DATE_FORMAT).parse(prefs.getString(context.getString(R.string.settings_fb_access_token_exp_date),
              new Date(token.getTime() + 1000L * 3600L * 24L * 365L).toString()));
    } catch (ParseException ex) {
//      Logger.getLogger(StoreHandler.class.getName()).log(Level.SEVERE, null, ex);
    }
    return token;
  }
  
  public static void modifyAccount(Context context, AccountAndr oldAccount, AccountAndr newAccount) throws Exception {
    List<AccountAndr> accounts = getAccounts(context);
    if (accounts.contains(oldAccount)) {
      accounts.remove(oldAccount);
      accounts.add(newAccount);
      saveAccounts(accounts, context);
    }
  }
  
  public static void removeAccount(Context context, AccountAndr account) throws Exception {
    Log.d("rgai", "REMOVING ACCOUNT: " + account);
    List<AccountAndr> accounts = getAccounts(context);
    if (accounts.contains(account)) {
      accounts.remove(account);
      saveAccounts(accounts, context);
    }
  }
  
  public static void addAccount(Context context, AccountAndr account) throws Exception {
    List<AccountAndr> accounts = getAccounts(context);
    if (!accounts.contains(account)) {
      accounts.add(account);
      saveAccounts(accounts, context);
    }
  }
  
  public static boolean isFacebookAccountAdded(Context context) {
    List<AccountAndr> accounts = getAccounts(context);
    
    for (AccountAndr a : accounts) {
      if (a.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
        return true;
      }
    }
    return false;
  }
  
  public static FacebookAccountAndr getFacebookAccount(Context context) {
    List<AccountAndr> accounts = getAccounts(context);
    
    for (AccountAndr a : accounts) {
      if (a.getAccountType().equals(MessageProvider.Type.FACEBOOK)) {
        return (FacebookAccountAndr)a;
      }
    }
    return null;
  }
  
  public static void saveAccounts(List<AccountAndr> accounts, Context context) throws Exception {
    removeAccountSettings(context);
    int i = 0;
    SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.settings_accounts), Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(context.getString(R.string.settings_accounts_size), accounts.size());
    for(AccountAndr a : accounts) {
      if (a.getAccountType() == MessageProvider.Type.GMAIL) {
        GmailAccountAndr ga = (GmailAccountAndr) a;
        editor.putString(context.getString(R.string.settings_accounts_item_type) + "_" + i, context.getString(R.string.account_name_gmail));
        editor.putString(context.getString(R.string.settings_accounts_item_name) + "_" + i, ga.getEmail());
        editor.putString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, ga.getPassword());
        editor.putString(context.getString(R.string.settings_accounts_item_imap) + "_" + i, ga.getImapAddress());
        editor.putString(context.getString(R.string.settings_accounts_item_smtp) + "_" + i, ga.getSmtpAddress());
//        editor.putBoolean(context.getString(R.string.settings_accounts_item_ssl) + "_" + i, ga.isSsl());
        editor.putInt(context.getString(R.string.settings_accounts_item_amount) + "_" + i, ga.getMessageLimit());
      } else if (a.getAccountType() == MessageProvider.Type.FACEBOOK) {
        FacebookAccountAndr fa = (FacebookAccountAndr) a;
        editor.putString(context.getString(R.string.settings_accounts_item_type) + "_" + i, context.getString(R.string.account_name_facebook));
        editor.putString(context.getString(R.string.settings_accounts_item_name) + "_" + i, fa.getDisplayName());
        editor.putString(context.getString(R.string.settings_accounts_item_unique_name) + "_" + i, fa.getUniqueName());
        editor.putString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, fa.getPassword());
        editor.putString(context.getString(R.string.settings_accounts_item_id) + "_" + i, fa.getId());
        editor.putInt(context.getString(R.string.settings_accounts_item_amount) + "_" + i, fa.getMessageLimit());
      } else if (a.getAccountType() == MessageProvider.Type.EMAIL) {
        EmailAccountAndr ea = (EmailAccountAndr) a;
        editor.putString(context.getString(R.string.settings_accounts_item_type) + "_" + i, context.getString(R.string.account_name_simplemail));
        editor.putString(context.getString(R.string.settings_accounts_item_name) + "_" + i, ea.getEmail());
        editor.putString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, ea.getPassword());
        editor.putString(context.getString(R.string.settings_accounts_item_imap) + "_" + i, ea.getImapAddress());
        editor.putString(context.getString(R.string.settings_accounts_item_smtp) + "_" + i, ea.getSmtpAddress());
        editor.putBoolean(context.getString(R.string.settings_accounts_item_ssl) + "_" + i, ea.isSsl());
        editor.putInt(context.getString(R.string.settings_accounts_item_amount) + "_" + i, ea.getMessageLimit());
      } else {
        throw new Exception("Unsupported account type: " + a.getAccountType());
      }
      i++;
    }
    editor.commit();
  }
  
  private static void removeAccountSettings(Context context) throws Exception {
    SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.settings_accounts), Context.MODE_PRIVATE);
    int amount = prefs.getInt(context.getString(R.string.settings_accounts_size), -1);
    SharedPreferences.Editor editor = prefs.edit();
    for (int i = 0; i < amount; i++) {
      String type = prefs.getString(context.getString(R.string.settings_accounts_item_type) + "_" + i, null);
      if (type.equals(context.getString(R.string.account_name_gmail))) {
        editor.remove(context.getString(R.string.settings_accounts_item_type) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_name) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_pass) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_imap) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_smtp) + "_" + i);
//        editor.remove(context.getString(R.string.settings_accounts_item_ssl) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_amount) + "_" + i);
      } else if (type.equals(context.getString(R.string.account_name_facebook))) {
        editor.remove(context.getString(R.string.settings_accounts_item_type) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_name) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_unique_name) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_pass) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_id) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_amount) + "_" + i);
      } else if (type.equals(context.getString(R.string.account_name_simplemail))) {
        editor.remove(context.getString(R.string.settings_accounts_item_type) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_name) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_pass) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_imap) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_smtp) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_ssl) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_amount) + "_" + i);
      } else {
        throw new Exception("Unsupported account type: " + type);
      }
    }
    editor.commit();
  }
  
  public static List<AccountAndr> getAccounts(Context context) {
    List<AccountAndr> accounts = new LinkedList<AccountAndr>();
    SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.settings_accounts), Context.MODE_PRIVATE);
    int amount = prefs.getInt(context.getString(R.string.settings_accounts_size), -1);
    for (int i = 0; i < amount; i++) {
      String type = prefs.getString(context.getString(R.string.settings_accounts_item_type) + "_" + i, "");
      if (type.equals(context.getString(R.string.account_name_gmail))) {
        String email = prefs.getString(context.getString(R.string.settings_accounts_item_name) + "_" + i, null);
        String pass = prefs.getString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, null);
//        boolean ssl = prefs.getBoolean(context.getString(R.string.settings_accounts_item_ssl) + "_" + i, true);
        int num = prefs.getInt(context.getString(R.string.settings_accounts_item_amount) + "_" + i, 5);
        accounts.add(new GmailAccountAndr(num, email, pass));
      } else if (type.equals(context.getString(R.string.account_name_facebook))) {
        String displayName = prefs.getString(context.getString(R.string.settings_accounts_item_name) + "_" + i, null);
        String uniqueName = prefs.getString(context.getString(R.string.settings_accounts_item_unique_name) + "_" + i, null);
        String pass = prefs.getString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, null);
        String id = prefs.getString(context.getString(R.string.settings_accounts_item_id) + "_" + i, null);
        int num = prefs.getInt(context.getString(R.string.settings_accounts_item_amount) + "_" + i, 5);
        accounts.add(new FacebookAccountAndr(num, displayName, uniqueName, id, pass));
      } else if (type.equals(context.getString(R.string.account_name_simplemail))) {
        String email = prefs.getString(context.getString(R.string.settings_accounts_item_name) + "_" + i, null);
        String pass = prefs.getString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, null);
        String imap = prefs.getString(context.getString(R.string.settings_accounts_item_imap) + "_" + i, null);
        String smtp = prefs.getString(context.getString(R.string.settings_accounts_item_smtp) + "_" + i, null);
        boolean ssl = prefs.getBoolean(context.getString(R.string.settings_accounts_item_ssl) + "_" + i, true);
        int num = prefs.getInt(context.getString(R.string.settings_accounts_item_amount) + "_" + i, 5);
        accounts.add(new EmailAccountAndr(email, pass, imap, smtp, ssl, num));
      } else {
        Toast.makeText(context, "Unsupported account type: " + type, Toast.LENGTH_LONG);
      }
    }
    return accounts;
  }
}
