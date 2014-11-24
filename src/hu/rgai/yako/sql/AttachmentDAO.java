package hu.rgai.yako.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import hu.rgai.yako.beens.Attachment;
import hu.rgai.yako.tools.Utils;

import java.util.*;

/**
 * Created by kojak on 7/2/2014.
 */
public class AttachmentDAO {

  private static AttachmentDAO instance = null;
  private SQLHelper mDbHelper = null;

  // table definitions
  public static final String TABLE_ATTACHMENTS = "attachments";

  public static final String COL_ID = "_id";
  private static final String COL_FILENAME = "filename";
  private static final String COL_SIZE = "size";
  public static final String COL_MESSAGE_ID = FullMessageDAO.TABLE_MESSAGE_CONTENT + FullMessageDAO.COL_ID;

  public static final String TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_ATTACHMENTS + "("
          + COL_ID + " integer primary key autoincrement, "
          + COL_FILENAME + " text not null, "
          + COL_SIZE + " integer, "
          + COL_MESSAGE_ID + " integer not null, "
          + " FOREIGN KEY (" + COL_MESSAGE_ID + ") REFERENCES "
            + FullMessageDAO.TABLE_MESSAGE_CONTENT + "(" + FullMessageDAO.COL_ID + "));";

  public static final String INDEX_ON_FILENAME = TABLE_ATTACHMENTS + "__" + COL_FILENAME + "__idx";

  public static final String CREATE_INDEX_ON_FILENAME = "CREATE INDEX " + INDEX_ON_FILENAME
          + " ON " + TABLE_ATTACHMENTS + "(" + COL_FILENAME + ");";

  private String[] allColumns = { COL_ID, COL_FILENAME, COL_SIZE, COL_MESSAGE_ID };


  public static synchronized AttachmentDAO getInstance(Context context) {
    if (instance == null) {
      instance = new AttachmentDAO(context);
    }
    return instance;
  }


  public int deleteAttachments(List<Long> fullSimpleMessageIds) {
    if (!fullSimpleMessageIds.isEmpty()) {
      String inClosure = SQLHelper.Utils.getInClosure(fullSimpleMessageIds);
//      Log.d("rgai", inClosure);
      return mDbHelper.getDatabase().delete(TABLE_ATTACHMENTS, COL_MESSAGE_ID + " IN " + inClosure, null);
    } else {
      return 0;
    }
  }

  public void insertAttachmentInfo(long fullMessageRawId, Attachment attachment) {
    List<Attachment> collection = new ArrayList<Attachment>(1);
    collection.add(attachment);
    insertAttachmentInfo(fullMessageRawId, collection);
  }

  public void insertAttachmentInfo(long fullMessageRawId, Collection<Attachment> attachments) {
    if (attachments != null) {
      for (Attachment a : attachments) {
        ContentValues cv = new ContentValues();
        cv.put(COL_FILENAME, a.getFileName());
        cv.put(COL_SIZE, a.getSize());
        cv.put(COL_MESSAGE_ID, fullMessageRawId);
        mDbHelper.getDatabase().insert(TABLE_ATTACHMENTS, null, cv);
        Log.d("rgai", "inserting attachment to db....");
      }
    }
  }


  public Map<Long, List<Attachment>> getAttachments(long fullMessageRawId) {
    List<Long> ids = new ArrayList<>(1);
    ids.add(fullMessageRawId);
    return getAttachments(ids);
  }


//  public Cursor getAttachmentsCursorToMessage(long fullMessageRawId) {
//    Cursor cursor = mDbHelper.getDatabase().query(TABLE_ATTACHMENTS, allColumns, COL_MESSAGE_ID + " = ?",
//            new String[]{Long.toString(fullMessageRawId)}, null, null, COL_FILENAME);
//    Log.d("rgai4", "attachment count: " + cursor.getCount() + " @ msg id " + fullMessageRawId);
//    return cursor;
//  }


  public Map<Long, List<Attachment>> getAttachments(Collection<Long> fullMessageRawIds) {
    HashMap<Long, List<Attachment>> attachments = new HashMap<Long, List<Attachment>>();
    if (fullMessageRawIds != null && !fullMessageRawIds.isEmpty()) {
      String cols = Utils.joinString(allColumns, ",");
      String ids = SQLHelper.Utils.getInClosure(fullMessageRawIds);
      String[] selectionArgs = null;
      String query = "SELECT " + cols
              + " FROM " + TABLE_ATTACHMENTS
              + " WHERE " + COL_MESSAGE_ID + " IN " + ids;

      Cursor cursor = mDbHelper.getDatabase().rawQuery(query, selectionArgs);
      cursor.moveToFirst();
      while (!cursor.isAfterLast()) {
        long _id = cursor.getLong(cursor.getColumnIndex(COL_ID));
        long msg_id = cursor.getLong(cursor.getColumnIndex(COL_MESSAGE_ID));
        String fileName = cursor.getString(cursor.getColumnIndex(COL_FILENAME));
        long fileSize = cursor.getLong(cursor.getColumnIndex(COL_SIZE));
        Attachment att = new Attachment(_id, fileName, fileSize, 0);
        if (!attachments.containsKey(msg_id)) {
          attachments.put(msg_id, new LinkedList<Attachment>());
        }
        attachments.get(msg_id).add(att);
        cursor.moveToNext();
      }
    }

    return attachments;
  }


  private AttachmentDAO(Context context) {
    mDbHelper = SQLHelper.getInstance(context);
  }

  public synchronized void close() {
    mDbHelper.closeDatabase();
  }

  public static Attachment cursorToAttachment(Cursor cursor) {
    Attachment attachment = null;
    if (cursor != null && !cursor.isAfterLast()) {
      long _id = cursor.getLong(cursor.getColumnIndex(COL_ID));
      String fileName = cursor.getString(cursor.getColumnIndex(COL_FILENAME));
      long fileSize = cursor.getLong(cursor.getColumnIndex(COL_SIZE));
      attachment = new Attachment(_id, fileName, fileSize, 0);
    }
    return attachment;
  }


}
