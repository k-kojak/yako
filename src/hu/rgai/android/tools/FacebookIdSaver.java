package hu.rgai.android.tools;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.provider.ContactsContract;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class FacebookIdSaver {

  // one item of a data has 2 elements: name, fbid
  public FacebookIdSaver(Context context, List<String> data) {
    ContentResolver cr = context.getContentResolver();
//    String[] arr = {"DISPLAY_NAME", "MIMETYPE", "TYPE"};

    String where = ContactsContract.Data.DISPLAY_NAME + " = ?";
    
    for (String s : data) {
      String[] row = s.split(",");
      String name = row[0];
      String fbID = row[1];
      
      String[] params = new String[] {name};
      
      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();


        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                .withSelection(where, params)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, "5657")
               // .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, "Sample Name 21")
                .build());
        
//        String where1 = ContactsContract.Data.DISPLAY_NAME + " = ? AND " + 
//        ContactsContract.Data.MIMETYPE + " = ?";
//        String[] params1 = new String[] {name,"vnd.android.cursor.item/name"};
//        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
//                .withSelection(where1, params1)
//                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, "Sample Name")
//                .build());
//
//
//        String[] params2 = new String[] {name,"vnd.android.cursor.item/email_v2"};
//        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
//                .withSelection(where1, params2)
//                .withValue(ContactsContract.CommonDataKinds.Email.DATA, "Hi There")
//                .build());
   // phoneCur.close();

    try {
        cr.applyBatch(ContactsContract.AUTHORITY, ops);
    } catch (RemoteException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    } catch (OperationApplicationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
      
    }
  }
  
}
