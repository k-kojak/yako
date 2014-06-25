package hu.rgai.yako.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.messageproviders.MessageProvider;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;
import java.util.TreeSet;

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
          " FOREIGN KEY ("+ COL_ACCOUNT_ID +") REFERENCES "+ AccountDAO.TABLE_ACCOUNTS +"("+ AccountDAO.COL_ID +"));";

  private String[] allColumns = { COL_ID, COL_MSG_ID, COL_SEEN, COL_TITLE, COL_SUBTITLE, COL_UNREAD_CNT, COL_FROM_ID,
          COL_FROM_NAME, COL_FROM_TYPE, COL_FROM_CONTACT_ID, COL_DATE, COL_MSG_TYPE, COL_ACCOUNT_ID, COL_CONTENT};


  public static synchronized MessageListDAO getInstane(Context context) {
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


  public synchronized void clearTable() {
    mDbHelper.getDatabase().delete(TABLE_MESSAGES, null, null);
  }

  public synchronized void removeMessagesToAccount(int accountId) {
    mDbHelper.getDatabase().delete(TABLE_MESSAGES, COL_ACCOUNT_ID + " = ?", new String[] {Integer.toString(accountId)});
  }

  public synchronized void insertMessages(TreeSet<MessageListElement> messages, TreeMap<Account, Integer> accounts) {
    clearTable();
    for (MessageListElement mle : messages) {
      insertMessage(mle, accounts);
    }
  }

  private synchronized void insertMessage(MessageListElement mle, TreeMap<Account, Integer> accounts) {
    ContentValues cv = new ContentValues();

    cv.put(COL_MSG_ID, mle.getId());
    cv.put(COL_SEEN, mle.isSeen() ? 1 : 0);
    cv.put(COL_TITLE, mle.getTitle());
    cv.put(COL_SUBTITLE, mle.getSubTitle());
    cv.put(COL_UNREAD_CNT, mle.getUnreadCount());

    cv.put(COL_FROM_ID, mle.getFrom().getId());
    cv.put(COL_FROM_NAME, mle.getFrom().getName());
    cv.put(COL_FROM_TYPE, mle.getFrom().getType().toString());
    cv.put(COL_FROM_CONTACT_ID, mle.getFrom().getContactId());

    cv.put(COL_DATE, new Timestamp(mle.getDate().getTime()).toString());
    cv.put(COL_MSG_TYPE, mle.getMessageType().toString());
    cv.put(COL_ACCOUNT_ID, accounts.get(mle.getAccount()));
    if (mle.getMessageType().equals(MessageProvider.Type.EMAIL) || mle.getMessageType().equals(MessageProvider.Type.GMAIL)) {
      cv.put(COL_CONTENT, ((FullSimpleMessage)mle.getFullMessage()).getContent().getContent().toString());
    }

    if (cv != null) {
      mDbHelper.getDatabase().insert(TABLE_MESSAGES, null, cv);
    }
  }

  public TreeSet<MessageListElement> getAllMessages(TreeMap<Integer, Account> accounts) {
    TreeSet<MessageListElement> messages = new TreeSet<MessageListElement>();

    Cursor cursor = mDbHelper.getDatabase().query(TABLE_MESSAGES, allColumns, null, null, null, null, null);

    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      MessageListElement mle = cursorToMessageListElementComment(cursor, accounts);
      if (mle != null) {
        messages.add(mle);
      }
      cursor.moveToNext();
    }
    cursor.close();
    return messages;
  }

  private MessageListElement cursorToMessageListElementComment(Cursor cursor, TreeMap<Integer, Account> accounts) {
    Person from = new Person(cursor.getInt(9), cursor.getString(6), cursor.getString(7),
            MessageProvider.Type.valueOf(cursor.getString(8)));
    Date date = null;
    try {
      date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(cursor.getString(10));
    } catch (ParseException e) {
      e.printStackTrace();
    }
    MessageListElement mle = null;
    if (date != null) {
      mle = new MessageListElement(cursor.getString(1), cursor.getInt(2) == 1, cursor.getString(3),
              cursor.getString(4), from, null, date, accounts.get(cursor.getInt(12)),
              MessageProvider.Type.valueOf(cursor.getString(11)));
      if (mle.getMessageType().equals(MessageProvider.Type.EMAIL) || mle.getMessageType().equals(MessageProvider.Type.GMAIL)) {
        FullSimpleMessage fsm = new FullSimpleMessage(mle.getId(), "subject",
                new HtmlContent(cursor.getString(13), HtmlContent.ContentType.TEXT_HTML), date, from, false,
                mle.getMessageType(), null);
        mle.setFullMessage(fsm);
      }
    }
    return mle;
  }
}






