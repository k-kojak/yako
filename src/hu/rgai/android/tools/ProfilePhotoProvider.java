
package hu.rgai.android.tools;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.util.Log;
import hu.rgai.android.test.R;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ProfilePhotoProvider {

  private static Map<Long, Bitmap> photos = null;
  
  /**
   * 
   * @param context
   * @param type type of 
   * @param contactId android contact id
   * @return 
   */
  public static Bitmap getImageToUser(Context context, long contactId) {
    Bitmap img = null;
    if (photos == null) {
      photos = new HashMap<Long, Bitmap>();
      Bitmap defaultImg = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_picture);
      photos.put(-1l, defaultImg);
    }
    if (photos.containsKey(contactId)) {
      img = photos.get(contactId);
    } else {
      img = getImgToUserId(context, contactId);
      if (img != null) {
        photos.put(contactId, img);
      }
    }
    if (img == null) {
      img = photos.get(-1l);
    }
    return img;
  }
  
  private static Bitmap getImgToUserId(Context context, long uid) {
    Bitmap bm = null;
    
    Cursor cursor = context.getContentResolver().query(
            ContactsContract.Data.CONTENT_URI,
            new String[] {ContactsContract.CommonDataKinds.Photo.PHOTO},
            ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
            new String[]{uid + "", ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE},
            null);
    if (cursor != null) {
      while (cursor.moveToNext()) {
            
        int photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Photo.PHOTO);
        byte[] data = cursor.getBlob(photoIdx);
        if (data != null) {
          bm = BitmapFactory.decodeByteArray(data, 0, data.length);
        }
      }
      cursor.close();
    }
    
    return bm;
  }
}
