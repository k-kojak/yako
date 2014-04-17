
package hu.rgai.android.tools;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.util.Log;
import hu.rgai.android.test.R;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ProfilePhotoProvider {

  private static Map<Long, Bitmap> photos = null;
  private static Bitmap groupChatPhoto = null;
  private static Set<Long> noImageToTheseUsers;
  private static long noImageCacheTime = System.currentTimeMillis();
  
  /**
   * 
   * @param context
   * @param type type of 
   * @param contactId android contact id
   * @return 
   */
  public static Bitmap getImageToUser(Context context, long contactId) {
    Bitmap img = null;
    long noImageCacheCooldownTime = 600; // seconds
    if (noImageToTheseUsers == null || noImageCacheTime + 1000l * noImageCacheCooldownTime < System.currentTimeMillis()) {
      noImageToTheseUsers = new HashSet<Long>();
      noImageCacheTime = System.currentTimeMillis();
    }
    if (photos == null) {
      photos = new HashMap<Long, Bitmap>();
      Bitmap defaultImg = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_contact_picture);
      photos.put(-1l, defaultImg);
    }
    if (photos.containsKey(contactId)) {
      img = photos.get(contactId);
    } else if (noImageToTheseUsers.contains(contactId)) {
      img = photos.get(-1l);
    } else {
      img = getImgToUserId(context, contactId);
      if (img != null) {
        photos.put(contactId, img);
      } else {
        noImageToTheseUsers.add(contactId);
      }
    }
    if (img == null) {
      img = photos.get(-1l);
    }
    return img;
  }
  
  public static Bitmap getGroupChatPhoto(Context context) {
    if (groupChatPhoto == null) {
      groupChatPhoto = BitmapFactory.decodeResource(context.getResources(), R.drawable.group_chat);
    }
    
    return groupChatPhoto;
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
