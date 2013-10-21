package hu.rgai.android.tools;



import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;
import hu.rgai.android.beens.fbintegrate.FacebookIntegrateItem;
import hu.rgai.android.config.Settings;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class saves Facebook id's to contact list, or updates if exists.
 * Saves the Facebook id's as NORMAL Im types, like Skype, but of course this is a Custom type,
 * since Android does not have built in Facebook type.
 * 
 * @author Tamas Kojedzinszky
 */
public class FacebookIdSaver {

  public FacebookIdSaver() {
    
  }
  
  public void integrate(Context context, FacebookIntegrateItem fbii) {
    Long[] rawContactIdsToUpdate = this.getUserIdToUpdate(context, fbii.getName());
//    Log.d("rgai", "UPDATE COUNT -> " + rawContactIdsToUpdate.length);
    
    String facebookName = fbii.getFbAliasId().length() > 0 ? fbii.getFbAliasId() : fbii.getFbId();
    
    // Updating facebook ids
    for (long contactId : rawContactIdsToUpdate) {
    
      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
      String where = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
      String[] params = new String[]{contactId + "", ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE};
      ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
              .withSelection(where, params)
              .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE)
              .withValue(ContactsContract.Data.DATA1, facebookName)
              .withValue(ContactsContract.Data.DATA2, ContactsContract.CommonDataKinds.Im.TYPE_OTHER)
              .withValue(ContactsContract.Data.DATA5, ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM)
              .withValue(ContactsContract.Data.DATA6, Settings.Contacts.DataKinds.Facebook.CUSTOM_NAME)
              .withValue(ContactsContract.Data.DATA10, fbii.getFbId())
              .build());
      try {
        context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
      } catch (RemoteException ex) {
        Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
      } catch (OperationApplicationException ex) {
        Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    
    // If there was no update, then insert new
    if (rawContactIdsToUpdate.length == 0) {
//      Log.d("rgai", "THERE WAS NO UPDATE");
      Long[] rawContactIdsToInsert = this.getUserIdToInsert(context, fbii.getName());
//      Log.d("rgai", "INSERT COUNT -> " + rawContactIdsToInsert.length);
    
      for (long contactId : rawContactIdsToInsert) {

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
    //    int insertPos = ops.size();
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.RAW_CONTACT_ID, contactId)

                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.Data.DATA1, facebookName)
                .withValue(ContactsContract.Data.DATA2, ContactsContract.CommonDataKinds.Im.TYPE_OTHER)
                .withValue(ContactsContract.Data.DATA5, ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM)
                .withValue(ContactsContract.Data.DATA6, Settings.Contacts.DataKinds.Facebook.CUSTOM_NAME)
                .withValue(ContactsContract.Data.DATA10, fbii.getFbId())
                .build());
        try {
          context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException ex) {
          Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
        } catch (OperationApplicationException ex) {
          Logger.getLogger(FacebookIdSaver.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    
  }
  
  private Long[] getUserIdToUpdate(Context context, String name) {
    return getUserIds(context, name, true);
  }
  
  private Long[] getUserIdToInsert(Context context, String name) {
    return getUserIds(context, name, false);
  }
  
  private Long[] getUserIds(Context context, String name, boolean update) {
    ContentResolver cr = context.getContentResolver();
    name = name.toUpperCase();
    String[] projection = new String[] {
        ContactsContract.Data.RAW_CONTACT_ID,
        ContactsContract.Data.DISPLAY_NAME_PRIMARY,
        ContactsContract.Data.MIMETYPE,
    };
    
    String selection = "";
    String[] selectionArgs = null;
    if (update) {
      selection = "UPPER(" + ContactsContract.Data.DISPLAY_NAME_PRIMARY + ") LIKE ? "
            + " AND " + ContactsContract.Data.MIMETYPE + " = ? "
            + " AND " + ContactsContract.Data.DATA2 + " = ? "
            + " AND " + ContactsContract.Data.DATA5 + " = ?"
            + " AND " + ContactsContract.Data.DATA6 + " = ?";
      selectionArgs = new String[]{
              "%" + name + "%",
              ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE,
              ContactsContract.CommonDataKinds.Im.TYPE_OTHER + "",
              ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM + "",
              Settings.Contacts.DataKinds.Facebook.CUSTOM_NAME + ""
      };
    } else {
      selection = "UPPER(" + ContactsContract.Data.DISPLAY_NAME_PRIMARY + ") LIKE ? "
            + " AND " + ContactsContract.Data.MIMETYPE + " = ?";
      selectionArgs = new String[]{
              "%" + name + "%",
              ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
      };
    }
            
    Cursor cu = cr.query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, null);
    
    cu.moveToFirst();
    String rid = "-1";
    ArrayList<Long> ids = new ArrayList<Long>();
    while (!cu.isAfterLast()) {
      int ridIdx = cu.getColumnIndexOrThrow(ContactsContract.Data.RAW_CONTACT_ID);
      rid = cu.getString(ridIdx);
      ids.add(Long.parseLong(rid));
      
//      Log.d("rgai", "MIME -> " + cu.getString(cu.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)));
      
      int nameIdx = cu.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME_PRIMARY);
//      Log.d("rgai", "NAME -> " + cu.getString(nameIdx));
      cu.moveToNext();
    }
    if (cu != null) {
      cu.close();
    }
    
    return ids.toArray(new Long[ids.size()]);
    
  }
  
}
