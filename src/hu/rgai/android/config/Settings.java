package hu.rgai.android.config;

import android.provider.ContactsContract;
import hu.rgai.android.intent.beens.EmailRecipientAndr;
import hu.rgai.android.intent.beens.FacebookRecipientAndr;
import hu.rgai.android.intent.beens.PhoneRecipientAndr;
import hu.rgai.android.test.settings.FacebookSettingActivity;
import hu.rgai.android.test.settings.GmailSettingActivity;
import hu.rgai.android.test.settings.SimpleEmailSettingActivity;
import hu.uszeged.inf.rgai.messagelog.MessageProvider;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Tamas Kojedzinszky
 */
public final class Settings {
  
  private static Map<String, Class> contactDataTypeToRecipientClass = null;
  private static Map<MessageProvider.Type, Class> accountTypeToSettingClass = null;
//  private static String 

  public static Map<String, Class> getContactDataTypeToRecipientClass() {
    if (contactDataTypeToRecipientClass == null) {
      contactDataTypeToRecipientClass = new HashMap<String, Class>();
      contactDataTypeToRecipientClass.put(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, EmailRecipientAndr.class);
      contactDataTypeToRecipientClass.put(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, PhoneRecipientAndr.class);
      contactDataTypeToRecipientClass.put(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE, FacebookRecipientAndr.class);
    }
    return contactDataTypeToRecipientClass;
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
  
  
  
  public static final class Contacts {
    
    public static final class DataKinds {
      
      public static final class Facebook {
        
//        public static final String CONTENT_ITEM_TYPE = "hu.rgai.android.cursor.item/facebook";
        public static final String CUSTOM_NAME = "Facebook";
        
      }
      
    }
    
  }
  
}
