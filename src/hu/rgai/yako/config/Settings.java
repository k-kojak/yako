package hu.rgai.yako.config;

import android.os.Build;
import android.provider.ContactsContract;
import hu.rgai.yako.beens.EmailMessageRecipient;
import hu.rgai.yako.beens.FacebookMessageRecipient;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.FullThreadMessage;
import hu.rgai.yako.beens.SmsMessageRecipient;
import hu.rgai.yako.beens.EmailAccount;
import hu.rgai.yako.beens.FacebookAccount;
import hu.rgai.yako.beens.GmailAccount;
import hu.rgai.yako.messageproviders.FacebookMessageProvider;
import hu.rgai.yako.messageproviders.MessageProvider;
import hu.rgai.yako.messageproviders.SimpleEmailMessageProvider;
import hu.rgai.yako.messageproviders.SmsMessageProvider;
import hu.rgai.yako.view.activities.MessageReplyActivity;
import hu.rgai.android.test.R;
import hu.rgai.yako.view.activities.ThreadDisplayerActivity;
import hu.rgai.yako.view.activities.FacebookSettingActivity;
import hu.rgai.yako.view.activities.GmailSettingActivity;
import hu.rgai.yako.view.activities.SimpleEmailSettingActivity;
import hu.rgai.yako.view.activities.EmailDisplayerActivity;
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

  public static final double IMPORTANT_LIMIT = 0.88;

  public static final boolean DEBUG = false;
  public static final String FACEBOOK_ME_IMG_FOLDER = "facebook_img";
  public static final String FACEBOOK_ME_IMG_NAME = "me.png";
  
  public static final int NOTIFICATION_NEW_MESSAGE_ID = 1;
  public static final int NOTIFICATION_SENT_MESSAGE_ID = 2;
  
  /**
   * The interval of automatic message refresh in seconds.
   */
  public static final int MESSAGE_LOAD_INTERVAL = 60 * 2;
  
  /**
   * Amount of messages to download for each instance per query.
   */
  public static final int MESSAGE_QUERY_LIMIT = 10;
  
  /**
   * If live established connection (where it is supported) exceeds this timelimit in seconds,
   * the conneciton will be rebuild.
   */
  public static final int ESTABLISHED_CONNECTION_TIMEOUT = 60 * 15;
  
  public static final String CONTACT_DISPLAY_NAME = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? ContactsContract.Data.DISPLAY_NAME_PRIMARY : ContactsContract.Data.DISPLAY_NAME);
  
//  public static final int MESSAGE_LIST_TITLE_LENGTH = 30;
  
  private static Map<String, Class> contactDataTypeToRecipientClass = null;
  private static Map<MessageProvider.Type, Class> accountTypeToSettingClass = null;
  private static Map<MessageProvider.Type, Class> accountTypeToMessageDisplayer = null;
  private static Map<MessageProvider.Type, Class> accountTypeToMessageReplyer = null;
  private static Map<MessageProvider.Type, Class> accountTypeToMessageProvider = null;
  private static Map<MessageProvider.Type, Class> accountTypeToAccountClass = null;
  private static Map<MessageProvider.Type, Class> accountTypeToFullParcMessageClass = null;
  private static Map<MessageProvider.Type, Class> accountTypeToFullMessageClass = null;
  private static Map<MessageProvider.Type, Integer> accountTypeToIconResource = null;
  private static Map<String, Integer> imgToMimetype = null;
  private static List<String> facebookPermissions = null;
  

  public static Map<String, Class> getContactDataTypeToRecipientClass() {
    if (contactDataTypeToRecipientClass == null) {
      contactDataTypeToRecipientClass = new HashMap<String, Class>();
      contactDataTypeToRecipientClass.put(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, EmailMessageRecipient.class);
//      contactDataTypeToRecipientClass.put(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, PhoneRecipientAndr.class);
      contactDataTypeToRecipientClass.put(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, SmsMessageRecipient.class);
      contactDataTypeToRecipientClass.put(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE, FacebookMessageRecipient.class);
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
  
  public static Map<MessageProvider.Type, Class> getAccountTypeToFullParcMessageClass() {
    if (accountTypeToFullParcMessageClass == null) {
      accountTypeToFullParcMessageClass = new EnumMap<MessageProvider.Type, Class>(MessageProvider.Type.class);
      accountTypeToFullParcMessageClass.put(MessageProvider.Type.EMAIL, FullSimpleMessage.class);
      accountTypeToFullParcMessageClass.put(MessageProvider.Type.FACEBOOK, FullThreadMessage.class);
      accountTypeToFullParcMessageClass.put(MessageProvider.Type.GMAIL, FullSimpleMessage.class);
      accountTypeToFullParcMessageClass.put(MessageProvider.Type.SMS, FullThreadMessage.class);
    }
    return accountTypeToFullParcMessageClass;
  }
  
  public static Map<MessageProvider.Type, Class> getAccountTypeToFullMessageClass() {
    if (accountTypeToFullMessageClass == null) {
      accountTypeToFullMessageClass = new EnumMap<MessageProvider.Type, Class>(MessageProvider.Type.class);
      accountTypeToFullMessageClass.put(MessageProvider.Type.EMAIL, FullSimpleMessage.class);
      accountTypeToFullMessageClass.put(MessageProvider.Type.FACEBOOK, FullThreadMessage.class);
      accountTypeToFullMessageClass.put(MessageProvider.Type.GMAIL, FullSimpleMessage.class);
      accountTypeToFullMessageClass.put(MessageProvider.Type.SMS, FullThreadMessage.class);
    }
    return accountTypeToFullMessageClass;
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
      accountTypeToMessageDisplayer.put(MessageProvider.Type.EMAIL, EmailDisplayerActivity.class);
      accountTypeToMessageDisplayer.put(MessageProvider.Type.FACEBOOK, ThreadDisplayerActivity.class);
      accountTypeToMessageDisplayer.put(MessageProvider.Type.GMAIL, EmailDisplayerActivity.class);
      accountTypeToMessageDisplayer.put(MessageProvider.Type.SMS, ThreadDisplayerActivity.class);
    }
    return accountTypeToMessageDisplayer;
  }
  
  public static Map<MessageProvider.Type, Class> getAccountTypeToMessageReplyer() {
    if (accountTypeToMessageReplyer == null) {
      accountTypeToMessageReplyer = new EnumMap<MessageProvider.Type, Class>(MessageProvider.Type.class);
      
      accountTypeToMessageReplyer.put(MessageProvider.Type.EMAIL, MessageReplyActivity.class);
      accountTypeToMessageReplyer.put(MessageProvider.Type.FACEBOOK, ThreadDisplayerActivity.class);
      accountTypeToMessageReplyer.put(MessageProvider.Type.GMAIL, MessageReplyActivity.class);
      accountTypeToMessageReplyer.put(MessageProvider.Type.SMS, ThreadDisplayerActivity.class);
    }
    return accountTypeToMessageReplyer;
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
    public static final int GOOGLE_MAPS_ACTIVITY_RESULT = 3;
    public static final int PREFERENCES_REQUEST_CODE = 4;
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
    public static final String MESSAGE_SENT_INTENT = "hu.rgai.android.message_sent";
    public static final String THREAD_SERVICE_INTENT = "hu.rgai.android.threadmsg_service_intent";
    public static final String NEW_MESSAGE_ARRIVED_BROADCAST = "hu.rgai.android.new_message_arrived_broadcast";
    public static final String NOTIFY_NEW_FB_GROUP_THREAD_MESSAGE = "hu.rgai.android.notify_new_fb_group_thread_message";
  }
  
  public static final class Alarms {
    public static final String THREAD_MSG_ALARM_START = "hu.rgai.android.alarm.thread_msg.start";
    public static final String THREAD_MSG_ALARM_STOP = "hu.rgai.android.alarm.thread_msg.stop";
  }
  
}
