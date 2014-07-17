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
import hu.rgai.yako.sql.AccountDAO;
import hu.rgai.yako.sql.MessageListDAO;
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
  private static final String IS_MESSAGE_FOR_DATABASE_SORRY_DISPLAYED = "IS_MESSAGE_FOR_DATABASE_SORRY_DISPLAYED";
  
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
        Log.d("rgai", "", ex);
      } catch (IOException ex) {
        Log.d("rgai", "", ex);
      } finally {
        try {
          oos.close();
          fos.close();
        } catch (IOException ex) {
          Log.d("rgai", "", ex);
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


  private static void deleteFileIfExists(Context context, String file) {
    Log.d("rgai3", "DELETING FILE, BECAUSE WE HAD AN EXCEPTION WITH IT: " + file);
    File destFile = new File(context.getCacheDir(), file);
    if (destFile.isFile()) {
      destFile.delete();
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
        Log.d("rgai", "", ex);
      } catch (OptionalDataException ex) {
        Log.d("rgai", "", ex);
      } catch (IOException ex) {
        Log.d("rgai", "", ex);
        deleteFileIfExists(context, file);
      } catch (ClassNotFoundException ex) {
        Log.d("rgai", "", ex);
      } finally {
        try {
          ois.close();
        } catch (IOException ex) {
          Log.d("rgai", "", ex);
        } catch (NullPointerException ex) {
          Log.d("rgai", "", ex);
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
      Log.d("rgai", "", e);
    }
    
    return data;
  }


  /**
   * At some point, the storage of accounts at shared preferences was replaced by database store, at that point
   * the accounts were lost. Thats why at this version we display a message about this issue, but only once...
   *
   * @param context
   * @return
   */
  public static boolean isMessageForDatabaseSorryDisplayed(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(StoreHandler.class.getSimpleName(), Context.MODE_PRIVATE);
    boolean displayed = prefs.getBoolean(IS_MESSAGE_FOR_DATABASE_SORRY_DISPLAYED, false);
    return displayed;
  }


  public static void setIsMessageForDatabaseSorryDisplayed(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(StoreHandler.class.getSimpleName(), Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(IS_MESSAGE_FOR_DATABASE_SORRY_DISPLAYED, true);
    editor.commit();
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
      Log.d("rgai", "", e);
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
      Log.d("rgai", "", ex);
//      Logger.getLogger(StoreHandler.class.getName()).log(Level.SEVERE, null, ex);
    }
    return token;
  }

}
