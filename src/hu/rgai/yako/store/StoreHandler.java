package hu.rgai.yako.store;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.beens.FacebookAccount;
import hu.rgai.yako.beens.GmailAccount;
import hu.rgai.yako.beens.SmsAccount;
import hu.rgai.yako.config.Settings;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.android.test.R;
import hu.rgai.yako.YakoApp;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.view.activities.SystemPreferences;
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
import java.util.TreeSet;
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
  private static final String SELECTED_FILTER_ACCOUNT = "selected_filter_account";
  private static final String MAIN_MESSAGE_LIST = "main_message_list";
  
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
  
  
  public static TreeSet<MessageListElement> getCurrentMessageList(Context context) {
    Object o = readObject(context, MAIN_MESSAGE_LIST);
    if (o != null) {
      return (TreeSet<MessageListElement>)o;
    } else {
      return null;
    }
  }
  
  
  public static synchronized void saveCurrentMessageList(Context context, TreeSet<MessageListElement> messages) {
    writeObject(context, messages, MAIN_MESSAGE_LIST);
  }
  
  
  public static void saveSelectedFilterAccount(Context context, Account acc) {
    writeObject(context, acc, SELECTED_FILTER_ACCOUNT);
  }
  
  
  public static Account getSelectedFilterAccount(Context context) {
    Object o = readObject(context, SELECTED_FILTER_ACCOUNT);
    if (o != null) {
      return (Account)o;
    } else {
      return null;
    }
  }
  
  
  public static void writeLastNotificationObject(Context context, HashMap<Account, Date> map) {
    writeObject(context, map, LAST_NOTIFICATION_DATES_FILENAME);
  }
  
  
  public static HashMap<Account, Date> readLastNotificationObject(Context context) {
    Object o = readObject(context, LAST_NOTIFICATION_DATES_FILENAME);
//    Log.d("rgai2", "READED OBJECT: " + o);
    if (o != null) {
      return (HashMap<Account, Date>)o;
    } else {
      return null;
    }
  }
  
  
  private static void writeObject(Context context, Object object, String file) {
    // override object
    File destFile = new File(context.getCacheDir(), file);
    if (object != null) {
      FileOutputStream fos = null;
      ObjectOutputStream oos = null;
      try {
        fos = new FileOutputStream(destFile);
        oos = new ObjectOutputStream(fos);
        oos.writeObject(object);
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
    // if object is null, delete it
    else {
      if (destFile.exists()) {
        destFile.delete();
      }
    }
  }
  
  
  private static Object readObject(Context context, String file) {
    Object o = null;
    ObjectInputStream ois = null;
    FileInputStream fis = null;
    File destinationFile = new File(context.getCacheDir(), file);
//    Log.d("rgai2", "cache file exists: " + destinationFile.exists());
    if (destinationFile.exists()) {
      
      try {
        fis = new FileInputStream(destinationFile);
        ois = new ObjectInputStream(fis);
        o = ois.readObject();
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
    }
    return o;
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
  
  
  public static boolean saveByteArrayToDownloadFolder(Context context, byte[] data, String fileName) {
    return saveByteArray(context, data, null, fileName);
  }
  
  private static boolean saveByteArray(Context context, byte[] data, String folder, String filename) {

    try {
      File mainFilePath;
      if (folder == null) {
        if (isExternalStorageWritable()) {
          mainFilePath = getEmailAttachmentDownloadLocation();
        } else {
          return false;
        }
      } else {
        ContextWrapper cw = new ContextWrapper(context);
        mainFilePath = cw.getDir("media", Context.MODE_PRIVATE);
        mainFilePath = new File(mainFilePath, folder);
      }
      
      if (!mainFilePath.isDirectory()) {
        mainFilePath.mkdirs();
      }
      
      File file = new File(mainFilePath, filename);
      file.createNewFile();
      FileOutputStream outs = new FileOutputStream(file);
      outs.write(data);
      outs.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return true;
  }
  
  public static File getEmailAttachmentDownloadLocation() {
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); 
  }
  
  
  public static boolean isExternalStorageWritable() {
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state)) {
      return true;
    }
    return false;
  }

  
  public static void storeFacebookAccessToken(Context context, String token, Date expirationDate) {
    SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.settings_accounts), Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(context.getString(R.string.settings_fb_access_token), token);
    
    SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
    editor.putString(context.getString(R.string.settings_fb_access_token_exp_date), df.format(expirationDate));
//    Log.d("rgai", df.format(expirationDate));
    
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
  
  
  public static void modifyAccount(Context context, Account oldAccount, Account newAccount) throws Exception {
    TreeSet<Account> accounts = getAccounts(context);
    if (accounts.contains(oldAccount)) {
      accounts.remove(oldAccount);
      accounts.add(newAccount);
      saveAccounts(accounts, context);
    }
  }
  
  
  public static void removeAccount(Context context, Account account) throws Exception {
//    Log.d("rgai", "REMOVING ACCOUNT: " + account);
    TreeSet<Account> accounts = getAccounts(context);
    if (accounts.contains(account)) {
      accounts.remove(account);
      saveAccounts(accounts, context);
    }
  }
  
  public static void addAccount(Context context, Account account) throws Exception {
    TreeSet<Account> accounts = getAccounts(context);
    if (!accounts.contains(account)) {
      accounts.add(account);
      saveAccounts(accounts, context);
    }
  }
  

  public static void saveAccounts(TreeSet<Account> accounts, Context context) throws Exception {
    Log.d("rgai", "save accounts at store: " + accounts);
    removeAccountSettings(context);
    int i = 0;
    SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.settings_accounts), Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    int sms = 0;
    for (Account a : accounts) {
      // skip saving SMS account
      if (a.getAccountType().equals(MessageProvider.Type.SMS)) {
        sms = 1;
        continue;
      }
      
      if (a.getAccountType() == MessageProvider.Type.GMAIL) {
        GmailAccount ga = (GmailAccount) a;
        editor.putString(context.getString(R.string.settings_accounts_item_type) + "_" + i, context.getString(R.string.account_name_gmail));
        editor.putString(context.getString(R.string.settings_accounts_item_name) + "_" + i, ga.getEmail());
        editor.putString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, ga.getPassword());
        editor.putString(context.getString(R.string.settings_accounts_item_imap) + "_" + i, ga.getImapAddress());
        editor.putString(context.getString(R.string.settings_accounts_item_smtp) + "_" + i, ga.getSmtpAddress());
      } else if (a.getAccountType() == MessageProvider.Type.FACEBOOK) {
        FacebookAccount fa = (FacebookAccount) a;
        editor.putString(context.getString(R.string.settings_accounts_item_type) + "_" + i, context.getString(R.string.account_name_facebook));
        editor.putString(context.getString(R.string.settings_accounts_item_name) + "_" + i, fa.getDisplayName());
        editor.putString(context.getString(R.string.settings_accounts_item_unique_name) + "_" + i, fa.getUniqueName());
        editor.putString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, fa.getPassword());
        editor.putString(context.getString(R.string.settings_accounts_item_id) + "_" + i, fa.getId());
      } else if (a.getAccountType() == MessageProvider.Type.EMAIL) {
        EmailAccount ea = (EmailAccount) a;
        editor.putString(context.getString(R.string.settings_accounts_item_type) + "_" + i, context.getString(R.string.account_name_simplemail));
        editor.putString(context.getString(R.string.settings_accounts_item_name) + "_" + i, ea.getEmail());
        editor.putString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, ea.getPassword());
        editor.putString(context.getString(R.string.settings_accounts_item_imap) + "_" + i, ea.getImapAddress());
        editor.putString(context.getString(R.string.settings_accounts_item_smtp) + "_" + i, ea.getSmtpAddress());
        editor.putBoolean(context.getString(R.string.settings_accounts_item_ssl) + "_" + i, ea.isSsl());
      } else {
        throw new Exception("Unsupported account type: " + a.getAccountType());
      }
      i++;
    }
    editor.putInt(context.getString(R.string.settings_accounts_size), accounts.size() - sms);
    editor.commit();
    
    // reload accounts at application
    YakoApp.setAccounts(accounts);
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
      } else if (type.equals(context.getString(R.string.account_name_facebook))) {
        editor.remove(context.getString(R.string.settings_accounts_item_type) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_name) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_unique_name) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_pass) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_id) + "_" + i);
      } else if (type.equals(context.getString(R.string.account_name_simplemail))) {
        editor.remove(context.getString(R.string.settings_accounts_item_type) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_name) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_pass) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_imap) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_smtp) + "_" + i);
        editor.remove(context.getString(R.string.settings_accounts_item_ssl) + "_" + i);
      } else {
        throw new Exception("Unsupported account type: " + type);
      }
    }
    editor.commit();
  }
  
   
  public static TreeSet<Account> getAccounts(Context context) {
    TreeSet<Account> accounts = new TreeSet<Account>();
    SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.settings_accounts), Context.MODE_PRIVATE);
    int amount = prefs.getInt(context.getString(R.string.settings_accounts_size), -1);
    for (int i = 0; i < amount; i++) {
      String type = prefs.getString(context.getString(R.string.settings_accounts_item_type) + "_" + i, "");
      if (type.equals(context.getString(R.string.account_name_gmail))) {
        String email = prefs.getString(context.getString(R.string.settings_accounts_item_name) + "_" + i, null);
        String pass = prefs.getString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, null);
        accounts.add(new GmailAccount(email, pass));
      } else if (type.equals(context.getString(R.string.account_name_facebook))) {
        String displayName = prefs.getString(context.getString(R.string.settings_accounts_item_name) + "_" + i, null);
        String uniqueName = prefs.getString(context.getString(R.string.settings_accounts_item_unique_name) + "_" + i, null);
        String pass = prefs.getString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, null);
        String id = prefs.getString(context.getString(R.string.settings_accounts_item_id) + "_" + i, null);
        accounts.add(new FacebookAccount(displayName, uniqueName, id, pass));
      } else if (type.equals(context.getString(R.string.account_name_simplemail))) {
        String email = prefs.getString(context.getString(R.string.settings_accounts_item_name) + "_" + i, null);
        String pass = prefs.getString(context.getString(R.string.settings_accounts_item_pass) + "_" + i, null);
        String imap = prefs.getString(context.getString(R.string.settings_accounts_item_imap) + "_" + i, null);
        String smtp = prefs.getString(context.getString(R.string.settings_accounts_item_smtp) + "_" + i, null);
        boolean ssl = prefs.getBoolean(context.getString(R.string.settings_accounts_item_ssl) + "_" + i, true);
        accounts.add(new EmailAccount(email, pass, imap, smtp, ssl));
      } else {
        Toast.makeText(context, "Unsupported account type: " + type, Toast.LENGTH_LONG);
      }
    }
    if (YakoApp.isPhone) {
      accounts.add(SmsAccount.account);
    }
    return accounts;
  }
}
