package hu.rgai.yako.sql;

import android.content.Context;
import android.util.Log;

import java.util.List;

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
  private static final String COL_MESSAGE_ID = FullMessageDAO.TABLE_MESSAGE_CONTENT + FullMessageDAO.COL_ID;

  public static final String TABLE_CREATE = "CREATE TABLE " + TABLE_ATTACHMENTS + "("
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


  private AttachmentDAO(Context context) {
    mDbHelper = SQLHelper.getInstance(context);
  }

  public synchronized void close() {
    mDbHelper.closeDatabase();
  }



}
