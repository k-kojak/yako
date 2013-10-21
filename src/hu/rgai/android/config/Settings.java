package hu.rgai.android.config;

import android.provider.ContactsContract;
import hu.rgai.android.intent.beens.EmailRecipientAndr;
import hu.rgai.android.intent.beens.FacebookRecipientAndr;
import hu.rgai.android.intent.beens.PhoneRecipientAndr;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Tamas Kojedzinszky
 */
public final class Settings {
  
  private static Map<String, Class> contactDataTypeToRecipientClass = null;
//  private static String 

  public static Map<String, Class> getContactDataTypeToRecipientClass() {
    if (contactDataTypeToRecipientClass == null) {
      contactDataTypeToRecipientClass = new HashMap<String, Class>();
      contactDataTypeToRecipientClass.put(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, EmailRecipientAndr.class);
      contactDataTypeToRecipientClass.put(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, PhoneRecipientAndr.class);
      contactDataTypeToRecipientClass.put(Contacts.DataTypes.FACEBOOK, FacebookRecipientAndr.class);
    }
    return contactDataTypeToRecipientClass;
  }
  
  public static final class Contacts {
    
    public static final class DataTypes {
      public static final String FACEBOOK = "hu.rgai.android.cursor.item/facebook";
    }
    
  }
  
}
