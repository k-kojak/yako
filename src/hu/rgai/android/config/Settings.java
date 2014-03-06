package hu.rgai.android.config;

import android.content.Context;
import android.os.Build;
import android.provider.ContactsContract;
import hu.rgai.android.intent.beens.EmailRecipientAndr;
import hu.rgai.android.intent.beens.FacebookRecipientAndr;
import hu.rgai.android.intent.beens.SmsMessageRecipientAndr;
import hu.rgai.android.messageproviders.FacebookMessageProvider;
import hu.rgai.android.messageproviders.SmsMessageProvider;
import hu.rgai.android.test.EmailDisplayer;
import hu.rgai.android.test.R;
import hu.rgai.android.test.ThreadDisplayer;
import hu.rgai.android.test.settings.FacebookSettingActivity;
import hu.rgai.android.test.settings.GmailSettingActivity;
import hu.rgai.android.test.settings.SimpleEmailSettingActivity;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import hu.uszeged.inf.rgai.messagelog.SimpleEmailMessageProvider;
import hu.uszeged.inf.rgai.messagelog.beans.account.EmailAccount;
import hu.uszeged.inf.rgai.messagelog.beans.account.FacebookAccount;
import hu.uszeged.inf.rgai.messagelog.beans.account.GmailAccount;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Tamas Kojedzinszky
 */
public final class Settings {
  
  public static final boolean DEBUG = false;
  public static final String FACEBOOK_ME_IMG_FOLDER = "facebook_img";
  public static final String FACEBOOK_ME_IMG_NAME = "me.png";
  
  public static final int NOTIFICATION_NEW_MESSAGE_ID = 1;
  
  public static final String CONTACT_DISPLAY_NAME = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? ContactsContract.Data.DISPLAY_NAME_PRIMARY : ContactsContract.Data.DISPLAY_NAME);
  
//  public static final int MESSAGE_LIST_TITLE_LENGTH = 30;
  
  private static Map<String, Class> contactDataTypeToRecipientClass = null;
  private static Map<MessageProvider.Type, Class> accountTypeToSettingClass = null;
  private static Map<MessageProvider.Type, Class> accountTypeToMessageDisplayer = null;
  private static Map<MessageProvider.Type, Class> accountTypeToMessageProvider = null;
  private static Map<MessageProvider.Type, Class> accountTypeToAccountClass = null;
  private static Map<MessageProvider.Type, Integer> accountTypeToIconResource = null;
  private static Map<String, Integer> imgToMimetype = null;
  private static List<String> facebookPermissions = null;
  
  public static final int MAX_SNIPPET_LENGTH = 30;


  public static Map<String, Class> getContactDataTypeToRecipientClass() {
    if (contactDataTypeToRecipientClass == null) {
      contactDataTypeToRecipientClass = new HashMap<String, Class>();
      contactDataTypeToRecipientClass.put(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, EmailRecipientAndr.class);
//      contactDataTypeToRecipientClass.put(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, PhoneRecipientAndr.class);
      contactDataTypeToRecipientClass.put(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, SmsMessageRecipientAndr.class);
      contactDataTypeToRecipientClass.put(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE, FacebookRecipientAndr.class);
    }
    return contactDataTypeToRecipientClass;
  }
  
  public static Map<MessageProvider.Type, Class> getAccountTypeToAccountClass() {
    if (accountTypeToAccountClass == null) {
      accountTypeToAccountClass = new EnumMap<MessageProvider.Type, Class>(MessageProvider.Type.class);
      accountTypeToAccountClass.put(MessageProvider.Type.EMAIL, EmailAccount.class);
      accountTypeToAccountClass.put(MessageProvider.Type.FACEBOOK, FacebookAccount.class);
      accountTypeToAccountClass.put(MessageProvider.Type.GMAIL, GmailAccount.class);
    }
    return accountTypeToAccountClass;
  }
  
  public static Map<MessageProvider.Type, Integer> getAccountTypeToIconResource() {
    if (accountTypeToIconResource == null) {
      accountTypeToIconResource = new EnumMap<MessageProvider.Type, Integer>(MessageProvider.Type.class);
      accountTypeToIconResource.put(MessageProvider.Type.EMAIL, R.drawable.ic_email);
      accountTypeToIconResource.put(MessageProvider.Type.FACEBOOK, R.drawable.fb);
      accountTypeToIconResource.put(MessageProvider.Type.GMAIL, R.drawable.gmail_icon);
      accountTypeToIconResource.put(MessageProvider.Type.SMS, R.drawable.ic_sms3);
    }
    return accountTypeToIconResource;
  }
  
  public static Map<MessageProvider.Type, Class> getAccountTypeToMessageDisplayer() {
    if (accountTypeToMessageDisplayer == null) {
      accountTypeToMessageDisplayer = new EnumMap<MessageProvider.Type, Class>(MessageProvider.Type.class);
      accountTypeToMessageDisplayer.put(MessageProvider.Type.EMAIL, EmailDisplayer.class);
      accountTypeToMessageDisplayer.put(MessageProvider.Type.FACEBOOK, ThreadDisplayer.class);
      accountTypeToMessageDisplayer.put(MessageProvider.Type.GMAIL, EmailDisplayer.class);
      accountTypeToMessageDisplayer.put(MessageProvider.Type.SMS, ThreadDisplayer.class);
    }
    return accountTypeToMessageDisplayer;
  }
  
  public static Map<MessageProvider.Type, Class> getAccountTypeToMessageProvider() {
    if (accountTypeToMessageProvider == null) {
      accountTypeToMessageProvider = new EnumMap<MessageProvider.Type, Class>(MessageProvider.Type.class);
      accountTypeToMessageProvider.put(MessageProvider.Type.EMAIL, SimpleEmailMessageProvider.class);
      accountTypeToMessageProvider.put(MessageProvider.Type.FACEBOOK, FacebookMessageProvider.class);
      accountTypeToMessageProvider.put(MessageProvider.Type.GMAIL, SimpleEmailMessageProvider.class);
      accountTypeToMessageProvider.put(MessageProvider.Type.SMS, SmsMessageProvider.class);
    }
    return accountTypeToMessageProvider;
  }

  public static Map<MessageProvider.Type, Class> getAccountTypeToSettingClass() {
    if (accountTypeToSettingClass == null) {
      accountTypeToSettingClass = new EnumMap<MessageProvider.Type, Class>(MessageProvider.Type.class);
      accountTypeToSettingClass.put(MessageProvider.Type.EMAIL, SimpleEmailSettingActivity.class);
      accountTypeToSettingClass.put(MessageProvider.Type.GMAIL, GmailSettingActivity.class);
      accountTypeToSettingClass.put(MessageProvider.Type.FACEBOOK, FacebookSettingActivity.class);
    }
    return accountTypeToSettingClass;
  }
  
  public static Map<String, Integer> getImgToMimetype() {
    if (imgToMimetype == null) {
      imgToMimetype = new HashMap<String, Integer>();
      imgToMimetype.put("phone_v2", R.drawable.ic_sms3);
      imgToMimetype.put("email_v2", R.drawable.ic_email);
      imgToMimetype.put("im", R.drawable.ic_fb_messenger);
    }
    return imgToMimetype;
  }
  
  public static List<String> getFacebookPermissions() {
    if (facebookPermissions == null) {
      facebookPermissions = new LinkedList<String>();
      facebookPermissions.add("email");
      facebookPermissions.add("read_mailbox");
    }
    return facebookPermissions;
  }
  
  public static final class EmailUtils {
    
    private static Map<String, Integer> resToString = null;
    
    public static int getResourceIdToEmailDomain(String domain) {
      Integer rid = null;
      if (resToString == null) {
        fillResourceIdToEmalDomain();
      }
      rid = resToString.get(domain);
      if (rid == null) {
        return R.drawable.ic_email;
      } else {
        return rid;
      }
    }
    
    private static void fillResourceIdToEmalDomain() {
      resToString = new HashMap<String, Integer>();
      resToString.put("indamail", R.drawable.ic_indamail);
      resToString.put("vipmail", R.drawable.ic_indamail);
      resToString.put("csinibaba", R.drawable.ic_indamail);
      resToString.put("totalcar", R.drawable.ic_indamail);
      resToString.put("index", R.drawable.ic_indamail);
      resToString.put("velvet", R.drawable.ic_indamail);
      resToString.put("torzsasztal", R.drawable.ic_indamail);
      resToString.put("lamer", R.drawable.ic_indamail);
      
      resToString.put("yahoo", R.drawable.ic_yahoo);
      
      resToString.put("citromail", R.drawable.ic_citromail);
      
      resToString.put("outlook", R.drawable.ic_hotmail);
    }
  }
  
  public static final class ActivityRequestCodes {
    public static final int ACCOUNT_SETTING_RESULT = 1;
    public static final int FULL_MESSAGE_RESULT = 2;
  }
  
  public static final class ActivityResultCodes {
    public static final int ACCOUNT_SETTING_NEW = 1;
    public static final int ACCOUNT_SETTING_MODIFY = 2;
    public static final int ACCOUNT_SETTING_DELETE = 3;
    public static final int ACCOUNT_SETTING_CANCEL = 4;
  }
  
  public static final class Contacts {
    
    public static final class DataKinds {
      
      public static final class Facebook {
        
        public static final String CUSTOM_NAME = "Facebook";
        
      }
      
    }
    
  }
  
  public static final class Intents {
    public static final String THREAD_SERVICE_INTENT = "hu.rgai.android.threadmsg_service_intent";
    public static final String NEW_MESSAGE_ARRIVED_BROADCAST = "hu.rgai.android.new_message_arrived_broadcast";
    public static final String NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE = "hu.rgai.android.notify_new_fb_group_thread_message";
  }
  
  public static final class Alarms {
    public static final String THREAD_MSG_ALARM_START = "hu.rgai.android.alarm.thread_msg.start";
    public static final String THREAD_MSG_ALARM_STOP = "hu.rgai.android.alarm.thread_msg.stop";
  }
  
}
