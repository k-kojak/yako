
package hu.rgai.yako.tools;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import hu.rgai.yako.beens.BitmapResult;
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
   * @param contactId android contact id
   * @return 
   */
  public static BitmapResult getImageToUser(Context context, long contactId) {
    
    boolean isDefaultImage;
    Bitmap img = null;
    long noImageCacheCooldownTime = 600; // seconds
    if (noImageToTheseUsers == null || noImageCacheTime + 1000l * noImageCacheCooldownTime < System.currentTimeMillis()) {
      noImageToTheseUsers = new HashSet<Long>();
      noImageCacheTime = System.currentTimeMillis();
    }
    if (photos == null) {
      initPhotosMap(context);
    }
    if (photos.containsKey(contactId)) {
      img = photos.get(contactId);
      if (contactId == -1) {
        isDefaultImage = true;
      } else {
        isDefaultImage = false;
      }
    } else if (noImageToTheseUsers.contains(contactId)) {
      img = photos.get(-1l);
      isDefaultImage = true;
    } else {
      img = getImgToUserId(context, contactId);
      if (img != null) {
        photos.put(contactId, img);
        isDefaultImage = false;
      } else {
        noImageToTheseUsers.add(contactId);
        isDefaultImage = true;
      }
    }
    if (img == null) {
      img = photos.get(-1l);
    }
    
    
    return new BitmapResult(img, isDefaultImage);
  }
  
  public static Bitmap getDefaultBitmap(Context c) {
    if (photos == null) {
      initPhotosMap(c);
    }
    return photos.get(-1l);
  }
  
  private static void initPhotosMap(Context c) {
    photos = new HashMap<Long, Bitmap>();
    Bitmap defaultImg = BitmapFactory.decodeResource(c.getResources(), R.drawable.ic_contact_picture);
    photos.put(-1l, defaultImg);
  }
  
  public static boolean isImageToUserInCache(long id) {
    if (photos == null) {
      return false;
    } else if (photos.containsKey(id)) {
      return true;
    } else if (noImageToTheseUsers != null && noImageToTheseUsers.contains(id)) {
      return true;
    }
    
    return false;
  }
  
  public static BitmapResult getGroupChatPhoto(Context context) {
    if (groupChatPhoto == null) {
      groupChatPhoto = BitmapFactory.decodeResource(context.getResources(), R.drawable.group_chat);
    }
    
    return new BitmapResult(groupChatPhoto, true);
  }
  
  public static boolean isGroupChatPhotoLoaded() {
    return groupChatPhoto != null;
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
