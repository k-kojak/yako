package hu.rgai.yako.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import hu.rgai.yako.beens.FullSimpleMessage;
import hu.rgai.yako.beens.FullThreadMessage;
import hu.rgai.yako.beens.HtmlContent;
import hu.rgai.yako.beens.Person;
import hu.rgai.yako.messageproviders.MessageProvider;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by kojak on 7/2/2014.
 */
public class FullMessageDAO {

  private static FullMessageDAO instance = null;
  private SQLHelper mDbHelper = null;


  // table definitions
  public static final String TABLE_MESSAGE_CONTENT = "message_contents";

  public static final String COL_ID = "_id";
  private static final String COL_MSG_ID = "msg_id";
  private static final String COL_SUBJECT = "subject";
  private static final String COL_CONTENT_TYPE = "content_type";
  private static final String COL_CONTENT_TEXT = "content_text";
  private static final String COL_DATE = "date";
  private static final String COL_FROM_ID = PersonSenderDAO.TABLE_PERSON + PersonSenderDAO.COL_ID;
  private static final String COL_IS_ME = "is_me";
  public static final String COL_MESSAGE_LIST_ID = MessageListDAO.TABLE_MESSAGES + MessageListDAO.COL_ID;
  private static final String COL_MSG_TYPE = "message_type";

  public static final String TABLE_CREATE = "create table " + TABLE_MESSAGE_CONTENT + "("
          + COL_ID + " integer primary key autoincrement, "
          + COL_MSG_ID + " text NOT NULL, "
          + COL_SUBJECT + " text, "
          + COL_CONTENT_TYPE + " text, "
          + COL_CONTENT_TEXT + " text,"
          + COL_DATE + " text, "
          + COL_FROM_ID + " integer NOT NULL, "
          + COL_IS_ME + " integer, "
          + COL_MESSAGE_LIST_ID + " integer NOT NULL,"
          + COL_MSG_TYPE + " text NOT NULL, "
          + " UNIQUE ("+ COL_MSG_ID +", "+ COL_MESSAGE_LIST_ID +"),"
          + " FOREIGN KEY (" + COL_FROM_ID + ") REFERENCES "
            + PersonSenderDAO.TABLE_PERSON + "(" + PersonSenderDAO.COL_ID + "),"
          + " FOREIGN KEY (" + COL_MESSAGE_LIST_ID + ") REFERENCES "
            + MessageListDAO.TABLE_MESSAGES + "(" + MessageListDAO.COL_ID + ")"
          + ");";

  private String[] allColumns = { COL_ID, COL_MSG_ID, COL_SUBJECT, COL_CONTENT_TYPE, COL_CONTENT_TEXT, COL_DATE,
          COL_FROM_ID, COL_IS_ME, COL_MSG_TYPE };


  public static synchronized FullMessageDAO getInstance(Context context) {
    if (instance == null) {
      instance = new FullMessageDAO(context);
    }
    return instance;
  }


  private FullMessageDAO(Context context) {
    mDbHelper = SQLHelper.getInstance(context);
  }


  public void insertMessages(Context context, long messageListRawId, FullThreadMessage threadMessage) {
    Set<String> addedFullMessageIds = getFullMessageIdsByMessageRawId(messageListRawId);
    for (FullSimpleMessage fsm : threadMessage.getMessages()) {
      if (!addedFullMessageIds.contains(fsm.getId())) {
        insertMessage(context, messageListRawId, fsm);
      }
    }
  }


  public long insertMessage(Context context, long messageListRawId, FullSimpleMessage simpleMessage) {
    long fromId = PersonSenderDAO.getInstance(context).getOrInsertPerson(simpleMessage.getFrom());

    ContentValues cv = new ContentValues();
    cv.put(COL_MSG_ID, simpleMessage.getId());
    cv.put(COL_SUBJECT, simpleMessage.getSubject());
    cv.put(COL_CONTENT_TYPE, simpleMessage.getContent().getContentType().toString());
    cv.put(COL_CONTENT_TEXT, simpleMessage.getContent().getContent().toString());
    cv.put(COL_DATE, new Timestamp(simpleMessage.getDate().getTime()).toString());
    cv.put(COL_FROM_ID, fromId);
    cv.put(COL_IS_ME, simpleMessage.isIsMe() ? 1 : 0);
    cv.put(COL_MESSAGE_LIST_ID, messageListRawId);
    cv.put(COL_MSG_TYPE, simpleMessage.getMessageType().toString());

    return mDbHelper.getDatabase().insert(TABLE_MESSAGE_CONTENT, null, cv);
  }


  /**
   * Returns the ids of the FullSimpleMessages elements based on the MessageListElement's account id.
   * @param accountId  the id of the MessageListElement account id
   * @return
   */
  public List<Long> getFullMessageIdsByAccountId(long accountId, List<Long> messageListRawIds) {
    String idClause = null;
    if (messageListRawIds != null && !messageListRawIds.isEmpty()) {
      idClause = SQLHelper.Utils.getInClosure(messageListRawIds);
    }
    List<Long> ids = new LinkedList<Long>();
    String q = "SELECT c." + COL_ID
            + " FROM " + TABLE_MESSAGE_CONTENT + " AS c, " + MessageListDAO.TABLE_MESSAGES + " AS m"
            + " WHERE c." + COL_MESSAGE_LIST_ID + " = m." + MessageListDAO.COL_ID;

    if (accountId != -1) {
      q += " AND m." + MessageListDAO.COL_ACCOUNT_ID + " = " + accountId;
    }

    if (idClause != null) {
      q += " AND m." + MessageListDAO.COL_ID + " IN " + idClause;
    }
//    Log.d("rgai", "getFullMessageIdsByAccountId query : " + q);
    Cursor c = mDbHelper.getDatabase().rawQuery(q, null);
    c.moveToFirst();
    while (!c.isAfterLast()) {
      ids.add(c.getLong(0));
      c.moveToNext();
    }
    return ids;
  }


  /**
   * Returns a set of simple messages.
   * This method can be used for simple message requests like email or thread requests as well.
   * In the first case the set size will be 1, otherwise the size of the set could be bigger than 1.
   * @param rawMessageListId the mRawId of the message list element
   * @return
   */
  public TreeSet<FullSimpleMessage> getFullSimpleMessages(long rawMessageListId) {
    TreeSet<FullSimpleMessage> messages = new TreeSet<FullSimpleMessage>();
    String q = "SELECT " + COL_MSG_ID + ", " + COL_SUBJECT + ", " + COL_CONTENT_TYPE + ", " + COL_CONTENT_TEXT
            + ", " + COL_DATE + ", " + COL_IS_ME + ", " + COL_MSG_TYPE + ", " + PersonSenderDAO.COL_KEY
            + ", " + PersonSenderDAO.COL_NAME + ", " + PersonSenderDAO.COL_SECONDARY_NAME + ", " + PersonSenderDAO.COL_TYPE
            + " FROM " + TABLE_MESSAGE_CONTENT + " AS m, " + PersonSenderDAO.TABLE_PERSON + " AS p"
            + " WHERE m." + COL_FROM_ID + " = p." + PersonSenderDAO.COL_ID
              + " AND " + COL_MESSAGE_LIST_ID + " = ?";

    Cursor cursor = mDbHelper.getDatabase().rawQuery(q, new String[] {Long.toString(rawMessageListId)});
    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      Person p = new Person(cursor.getString(cursor.getColumnIndex(PersonSenderDAO.COL_KEY)),
              cursor.getString(cursor.getColumnIndex(PersonSenderDAO.COL_NAME)),
              MessageProvider.Type.valueOf(cursor.getString(cursor.getColumnIndex(PersonSenderDAO.COL_TYPE))));
      HtmlContent content = new HtmlContent(cursor.getString(cursor.getColumnIndex(COL_CONTENT_TEXT)),
              HtmlContent.ContentType.valueOf(cursor.getString(cursor.getColumnIndex(COL_CONTENT_TYPE))));

      FullSimpleMessage fsm = null;
      try {
        fsm = new FullSimpleMessage(cursor.getString(cursor.getColumnIndex(COL_MSG_ID)),
                cursor.getString(cursor.getColumnIndex(COL_SUBJECT)), content,
                SQLHelper.Utils.parseSQLdateString(cursor.getString(cursor.getColumnIndex(COL_DATE))),
                p, cursor.getInt(cursor.getColumnIndex(COL_IS_ME)) == 1,
                MessageProvider.Type.valueOf(cursor.getString(cursor.getColumnIndex(COL_MSG_TYPE))), null);
      } catch (ParseException e) {
        Log.d("rgai", "", e);
      }
      messages.add(fsm);
      cursor.moveToNext();
    }

    return messages;
  }


  public int getFullSimpleMessagesCount(long rawMessageListId) {
    String q = "SELECT COUNT(*) AS cnt"
            + " FROM " + TABLE_MESSAGE_CONTENT
            + " WHERE " + COL_MESSAGE_LIST_ID + " = ?";

    Cursor cursor = mDbHelper.getDatabase().rawQuery(q, new String[] {Long.toString(rawMessageListId)});
    cursor.moveToFirst();
    if (!cursor.isAfterLast()) {
      return cursor.getInt(0);
    }
    return 0;
  }



  public void removeMessage(String simpleMessageId, long messageListRawId) {
    mDbHelper.getDatabase().delete(TABLE_MESSAGE_CONTENT, COL_MSG_ID + " = ? AND " + COL_MESSAGE_LIST_ID + " = ?",
            new String[] {simpleMessageId, Long.toString(messageListRawId)});
  }


  public int removeMessagesToAccount(List<Long> fullSimpleMessageIds) {
    if (!fullSimpleMessageIds.isEmpty()) {
      String inClosure = SQLHelper.Utils.getInClosure(fullSimpleMessageIds);
      return mDbHelper.getDatabase().delete(TABLE_MESSAGE_CONTENT, COL_ID + " IN " + inClosure, null);
    } else {
      return 0;
    }
  }


  private Set<String> getFullMessageIdsByMessageRawId(long messageListRawId) {
    Set<String> ids = new TreeSet<String>();
    Cursor cursor = mDbHelper.getDatabase().query(TABLE_MESSAGE_CONTENT, new String[] {COL_MSG_ID},
            COL_MESSAGE_LIST_ID + " = ?", new String[]{Long.toString(messageListRawId)}, null, null, null);
    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      ids.add(cursor.getString(0));
      cursor.moveToNext();
    }
    return ids;
  }


  public synchronized void close() {
    mDbHelper.closeDatabase();
  }
}
