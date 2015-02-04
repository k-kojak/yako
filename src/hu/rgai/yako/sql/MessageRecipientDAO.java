package hu.rgai.yako.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import hu.rgai.yako.beens.MessageListElement;
import hu.rgai.yako.beens.Person;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by kojak on 11/21/2014.
 */
public class MessageRecipientDAO {

  private static MessageRecipientDAO instance = null;
  private SQLHelper mDbHelper = null;


  // table definitions
  public static final String TABLE_MESSAGE_RECIPIENT = "message_recipient";

  public static final String COL_MSG_ID = "msg_id";
  public static final String COL_PERSON_ID = "person_id";


  public static final String TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGE_RECIPIENT + "("
          + COL_MSG_ID + " integer not null, "
          + COL_PERSON_ID + " integer not null, "

          + " UNIQUE ("+ COL_MSG_ID +", "+ COL_PERSON_ID +"),"

          + " FOREIGN KEY ("+ COL_MSG_ID +")"
            + " REFERENCES " + MessageListDAO.TABLE_MESSAGES + "(" + MessageListDAO.COL_ID +"),"
          + " FOREIGN KEY ("+ COL_PERSON_ID +")"
            + " REFERENCES " + PersonDAO.TABLE_PERSON + "(" + PersonDAO.COL_ID +")"
          + ");";


  public static final String INDEX_ON_MSG_ID = TABLE_MESSAGE_RECIPIENT + "__" + COL_MSG_ID + "__idx";

  public static final String CREATE_INDEX_ON_MSG_ID = "CREATE INDEX " + INDEX_ON_MSG_ID
          + " ON " + TABLE_MESSAGE_RECIPIENT + "(" + COL_MSG_ID + ");";

  private String[] allColumns = { COL_MSG_ID, COL_PERSON_ID };

  public static synchronized MessageRecipientDAO getInstance(Context context) {
    if (instance == null) {
      instance = new MessageRecipientDAO(context);
    }
    return instance;
  }


  private MessageRecipientDAO(Context context) {
    mDbHelper = SQLHelper.getInstance(context);
  }

  public synchronized void close() {
    mDbHelper.closeDatabase();
  }

  public synchronized void removeRecipients(List<Long> messageRawIds) {
    String inClosure = SQLHelper.Utils.getInClosure(messageRawIds);
    String where = COL_MSG_ID + " IN " + inClosure;
    mDbHelper.getDatabase().delete(TABLE_MESSAGE_RECIPIENT, where, null);
  }

  public synchronized List<Person> getRecipientsToMessageId(Context context, MessageListElement mle) {
    if (mle.getRawId() == -1) {
      return null;
    }

    String[] selectionArgs = new String[]{String.valueOf(mle.getRawId())};
    String query = "SELECT " + COL_PERSON_ID
            + " FROM " + TABLE_MESSAGE_RECIPIENT
            + " WHERE " + COL_MSG_ID + " = ?";

    Cursor cursor = mDbHelper.getDatabase().rawQuery(query, selectionArgs);
    cursor.moveToFirst();
    List<Integer> personIds = new LinkedList<>();
    while (!cursor.isAfterLast()) {
      int _id = cursor.getInt(0);
      personIds.add(_id);
      cursor.moveToNext();
    }

    return PersonDAO.getInstance(context).getPersonsById(personIds);
  }

  public synchronized void insertRecipients(Context context, MessageListElement mle) {
    if (mle == null) {
      throw new NullPointerException("MessageListElement is null when trying to insert recipient.");
    }

    if (mle.getRawId() == -1 || mle.getRecipientsList() == null) {
      return;
    }

    for (Person p : mle.getRecipientsList()) {
      long personRawId = PersonDAO.getInstance(context).getOrInsertPerson(context, p);

      ContentValues cv = new ContentValues();
      cv.put(COL_MSG_ID, mle.getRawId());
      cv.put(COL_PERSON_ID, personRawId);

      mDbHelper.getDatabase().insert(TABLE_MESSAGE_RECIPIENT, null, cv);
    }
  }

}
