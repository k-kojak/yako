package hu.rgai.yako.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.messageproviders.MessageProvider;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.*;

public class MessageListDAO  {


  private static MessageListDAO instance;
  private SQLHelper mDbHelper = null;


  // table definitions
  public static final String TABLE_MESSAGES = "messages";



  public static final String COL_ID = "_id";
  public static final String COL_MSG_ID = "msg_id";
  public static final String COL_SEEN = "seen";
  public static final String COL_TITLE = "title";
  public static final String COL_SUBTITLE = "subtitle";
  public static final String COL_UNREAD_CNT = "unread_count";
  public static final String COL_CONTENT = "content";

  public static final String COL_FROM_ID = "from_id";
  public static final String COL_FROM_NAME = "from_name";
  public static final String COL_FROM_TYPE = "from_type";
  public static final String COL_FROM_CONTACT_ID = "from_contact_id";

  public static final String COL_DATE = "date";
  public static final String COL_MSG_TYPE = "message_type";
  public static final String COL_ACCOUNT_ID = "account_id";



  public static final String TABLE_CREATE = "create table " + TABLE_MESSAGES + "("
          + COL_ID + " integer primary key autoincrement, "
          + COL_MSG_ID + " text not null, "
          + COL_SEEN + " integer not null, "
          + COL_TITLE + " text not null, "
          + COL_SUBTITLE + " text,"
          + COL_UNREAD_CNT + " integer not null, "
          + COL_CONTENT + " text, "
          + COL_FROM_ID + " text, "
          + COL_FROM_NAME + " text, "
          + COL_FROM_TYPE + " text, "
          + COL_FROM_CONTACT_ID + " integer, "
          + COL_DATE + " text, "
          + COL_MSG_TYPE + " text, "
          + COL_ACCOUNT_ID + " integer not null," +
          " FOREIGN KEY (" + COL_ACCOUNT_ID + ") REFERENCES " + AccountDAO.TABLE_ACCOUNTS + "(" + AccountDAO.COL_ID + "));";


  public static final String INDEX_ON_MSG_TYPE = TABLE_MESSAGES + "__" + COL_MSG_TYPE + "__idx";

  public static final String CREATE_INDEX_ON_MSG_TYPE = "CREATE INDEX " + INDEX_ON_MSG_TYPE + " ON " + TABLE_MESSAGES + "(" + COL_ID + ");";

  private String[] allColumns = { COL_ID, COL_MSG_ID, COL_SEEN, COL_TITLE, COL_SUBTITLE, COL_UNREAD_CNT, COL_FROM_ID,
          COL_FROM_NAME, COL_FROM_TYPE, COL_FROM_CONTACT_ID, COL_DATE, COL_MSG_TYPE, COL_ACCOUNT_ID, COL_CONTENT};


  public static synchronized MessageListDAO getInstance(Context context) {
    if (instance == null) {
      instance = new MessageListDAO(context);
    }
    return instance;
  }


  public synchronized void close() {
    mDbHelper.closeDatabase();
  }


  private MessageListDAO(Context context) {
    mDbHelper = SQLHelper.getInstance(context);
  }


  public synchronized void removeMessages(Context context, long accountId, List<Long> messageIds) {
    List<Long> fullMessageIds = FullMessageDAO.getInstance(context).getFullMessageIdsByAccountId(accountId, messageIds);

    AttachmentDAO.getInstance(context).deleteAttachments(fullMessageIds);
    FullMessageDAO.getInstance(context).removeMessagesToAccount(fullMessageIds);

    String where = COL_ACCOUNT_ID + " = ?";
    if (messageIds != null && !messageIds.isEmpty()) {
      where += " AND " + COL_MSG_ID + " IN " + SQLHelper.Utils.getInClosure(messageIds, true);
    }
    mDbHelper.getDatabase().delete(TABLE_MESSAGES, where, new String[]{Long.toString(accountId)});

  }


  public synchronized void removeMessages(Context context, long accountId) {
    removeMessages(context, accountId, null);
  }


  public synchronized void updateFrom(long messageRawId, Person from) {
    if (from != null) {
      ContentValues cv = new ContentValues();
      cv.put(COL_FROM_ID, from.getId());
      cv.put(COL_FROM_NAME, from.getName());
      cv.put(COL_FROM_TYPE, from.getType().toString());
      cv.put(COL_FROM_CONTACT_ID, from.getContactId());
      mDbHelper.getDatabase().update(TABLE_MESSAGES, cv, COL_ID + " = ?", new String[]{Long.toString(messageRawId)});
    } else {
      throw new RuntimeException("Person was NULL when updating message's getFrom value");
    }
  }


  public synchronized void updateMessage(long messageRawId, boolean isSeen, int unreadCount, Date date, String title, String subTitle) {
    ContentValues cv = new ContentValues();
    cv.put(COL_SEEN, isSeen ? 1 : 0);
    cv.put(COL_UNREAD_CNT, unreadCount);
    cv.put(COL_DATE, new Timestamp(date.getTime()).toString());
    cv.put(COL_TITLE, title);
    cv.put(COL_SUBTITLE, subTitle);
    mDbHelper.getDatabase().update(TABLE_MESSAGES, cv, COL_ID + " = ?", new String[]{Long.toString(messageRawId)});
  }


  public void updateMessageToSeen(long rawId, boolean seen) {
    ContentValues cv = new ContentValues();
    cv.put(COL_SEEN, seen ? 1 : 0);
    if (seen) {
      cv.put(COL_UNREAD_CNT, 0);
    }

    mDbHelper.getDatabase().update(TABLE_MESSAGES, cv, COL_ID + " = ?", new String[]{Long.toString(rawId)});
  }


  public synchronized void insertMessage(MessageListElement mle, TreeMap<Account, Long> accounts) {
    ContentValues cv = new ContentValues();
    try {
      cv.put(COL_MSG_ID, mle.getId());
      cv.put(COL_SEEN, mle.isSeen() ? 1 : 0);
      cv.put(COL_TITLE, mle.getTitle());
      cv.put(COL_SUBTITLE, mle.getSubTitle());
      cv.put(COL_UNREAD_CNT, mle.getUnreadCount());

      // TODO: currently we are not storing messages where getFrom is null
      // this case happens now when we have a group chat, so there is no sender, just recipients, so the getFrom is NULL
      if (mle.getFrom() == null) return;
      cv.put(COL_FROM_ID, mle.getFrom().getId());
      cv.put(COL_FROM_NAME, mle.getFrom().getName());
      cv.put(COL_FROM_TYPE, mle.getFrom().getType().toString());
      cv.put(COL_FROM_CONTACT_ID, mle.getFrom().getContactId());

      cv.put(COL_DATE, new Timestamp(mle.getDate().getTime()).toString());
      cv.put(COL_MSG_TYPE, mle.getMessageType().toString());
      cv.put(COL_ACCOUNT_ID, accounts.get(mle.getAccount()));
      if (mle.getMessageType().equals(MessageProvider.Type.EMAIL) || mle.getMessageType().equals(MessageProvider.Type.GMAIL)) {
        cv.put(COL_CONTENT, ((FullSimpleMessage) mle.getFullMessage()).getContent().getContent().toString());
      }
    } catch (NullPointerException ex) {
      Log.d("rgai", "mle.getFrom has a null value somewhere: " + mle.getFrom());
      ex.printStackTrace();
    }

    if (cv != null) {
      mDbHelper.getDatabase().insert(TABLE_MESSAGES, null, cv);
    }
  }


  public Cursor getAllMessagesCursor() {
    return getAllMessagesCursor(-1);
  }


  public Cursor getAllMessagesCursor(long accountId) {
    String selection = null;
    String[] selectionArgs = null;
    if (accountId != -1) {
      selection = COL_ACCOUNT_ID + " = ?";
      selectionArgs = new String[]{Long.toString(accountId)};
    }
    return mDbHelper.getDatabase().query(TABLE_MESSAGES, allColumns, selection, selectionArgs, null, null, COL_DATE + " DESC");
  }


  public TreeSet<MessageListElement> getAllMessages(TreeMap<Long, Account> accounts) {
    return getAllMessages(accounts, -1);
  }


  public void removeMessage(long rawId) {
    mDbHelper.getDatabase().delete(TABLE_MESSAGES, COL_ID + " = ?", new String[] {Long.toString(rawId)});
  }


  public synchronized void removeMessage(MessageListElement mle, long accountId) {
    mDbHelper.getDatabase().delete(TABLE_MESSAGES, COL_MSG_ID + " = ? AND " + COL_ACCOUNT_ID + " = ?",
            new String[] {mle.getId(), Long.toString(accountId)});
  }


  public int getAllMessagesCount() {
    return getAllMessagesCount(-1);
  }


  public int getAllMessagesCount(long accountId) {
    int count = 0;

    String sql = "SELECT COUNT(*) AS cnt FROM " + TABLE_MESSAGES;
    String[] selectionArgs = null;
    if (accountId != -1) {
      sql = "SELECT COUNT(*) AS cnt FROM " + TABLE_MESSAGES + " WHERE " + COL_ACCOUNT_ID + " = ?";
      selectionArgs = new String[]{Long.toString(accountId)};
    }
    Cursor cursor = mDbHelper.getDatabase().rawQuery(sql, selectionArgs);
    cursor.moveToFirst();
    if (!cursor.isAfterLast()) {
      count = cursor.getInt(0);
    }
    cursor.close();
    return count;
  }


  public TreeSet<MessageListElement> getAllMessagesToAccount(Account a) {
    if (a != null) {
      TreeMap<Long, Account> accounts = new TreeMap<Long, Account>();
      accounts.put(a.getDatabaseId(), a);
      return getAllMessages(accounts, a.getDatabaseId());
    } else {
      return null;
    }
  }


  public TreeSet<MessageListElement> getAllMessages(TreeMap<Long, Account> accounts, long accountId) {
    TreeSet<MessageListElement> messages = new TreeSet<MessageListElement>();

    String selection = null;
    String[] selectionArgs = null;
    if (accountId != -1) {
      selection = COL_ACCOUNT_ID + " = ?";
      selectionArgs = new String[]{Long.toString(accountId)};
    }
    Cursor cursor = mDbHelper.getDatabase().query(TABLE_MESSAGES, allColumns, selection, selectionArgs, null, null, null);

    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      MessageListElement mle = cursorToMessageListElement(cursor, accounts);
      if (mle != null) {
        messages.add(mle);
      }
      cursor.moveToNext();
    }
    cursor.close();
    return messages;
  }


  public MessageListElement getMinimalMessage(long rawId, TreeMap<Long, Account> accounts) {
    MessageListElement mle = null;
    Cursor cursor = mDbHelper.getDatabase().query(TABLE_MESSAGES, new String[] {COL_MSG_ID, COL_ACCOUNT_ID}, COL_ID + " = ?",
            new String[] {Long.toString(rawId)}, null, null, null);
    cursor.moveToFirst();
    if (!cursor.isAfterLast()) {
      mle = new MessageListElement(rawId, cursor.getString(0), accounts.get(cursor.getLong(1)));
    }
    cursor.close();
    return mle;
  }


  public MessageListElement getMessageByRawId(long messageRawId, TreeMap<Long, Account> accounts) {
    return getMessageById(Long.toString(messageRawId), accounts, true);
  }


  public MessageListElement getMessageById(String messageId, TreeMap<Long, Account> accounts) {
    return getMessageById(messageId, accounts, false);
  }


  public MessageListElement getMessageById(String messageId, Account account) {
    TreeMap<Long, Account> accounts = new TreeMap<Long, Account>();
    accounts.put(account.getDatabaseId(), account);
    return getMessageById(messageId, accounts);
  }


  /**
   *
   * @param id
   * @param accounts
   * @param rawId if true, than id is a RAW id of the database, if false the id is the message id
   * @return
   */
  private MessageListElement getMessageById(String id, TreeMap<Long, Account> accounts, boolean rawId) {
    String column = rawId ? COL_ID : COL_MSG_ID;
    Cursor cursor = mDbHelper.getDatabase().query(TABLE_MESSAGES, allColumns, column + " = ?", new String[]{id},
            null, null, null);
    cursor.moveToFirst();
    MessageListElement mle = cursorToMessageListElement(cursor, accounts);
    cursor.close();
    return mle;
  }


  public long getMessageRawId(MessageListElement mle, long accountId) {
    String query = "SELECT "+ COL_ID +
            " FROM " + TABLE_MESSAGES + "" +
            " WHERE " + COL_MSG_ID + " = ? AND " + COL_ACCOUNT_ID + " = ?";
    Cursor cursor = mDbHelper.getDatabase().rawQuery(query, new String[] {mle.getId(), Long.toString(accountId)});
    cursor.moveToFirst();
    long _id = -1;
    if (!cursor.isAfterLast()) {
      _id = cursor.getLong(0);
    }
    return _id;
  }


  public static MessageListElement cursorToMessageListElement(Cursor cursor, TreeMap<Long, Account> accounts) {
    MessageListElement mle = null;
    if (!cursor.isAfterLast()) {
      Person from = new Person(cursor.getInt(9), cursor.getString(6), cursor.getString(7),
              MessageProvider.Type.valueOf(cursor.getString(8)));
      Date date = null;
      try {
        date = SQLHelper.Utils.parseSQLdateString(cursor.getString(10));
      } catch (ParseException e) {
        e.printStackTrace();
      }

      if (date != null) {
        mle = new MessageListElement(cursor.getLong(0), cursor.getString(1), cursor.getInt(2) == 1, cursor.getString(3),
                cursor.getString(4), from, null, date, accounts.get(cursor.getLong(12)),
                MessageProvider.Type.valueOf(cursor.getString(11)));
        // TODO: content should come from fullmessage table, not like this
        if (mle.getMessageType().equals(MessageProvider.Type.EMAIL) || mle.getMessageType().equals(MessageProvider.Type.GMAIL)) {
          FullSimpleMessage fsm = new FullSimpleMessage(mle.getId(), "subject",
                  new HtmlContent(cursor.getString(13), HtmlContent.ContentType.TEXT_HTML), date, from, false,
                  mle.getMessageType(), null);
          mle.setFullMessage(fsm);
        }
      }
    }
    return mle;
  }
}






