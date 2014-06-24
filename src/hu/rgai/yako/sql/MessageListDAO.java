package hu.rgai.yako.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.messageproviders.MessageProvider;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by kojak on 6/23/2014.
 */
public class MessageListDAO  {

  private SQLiteDatabase database;
  private SQLHelper dbHelper;
  private String[] allColumns = { SQLHelper.MessageListTable.COL_ID, SQLHelper.MessageListTable.COL_MSG_ID,
          SQLHelper.MessageListTable.COL_SEEN, SQLHelper.MessageListTable.COL_TITLE, SQLHelper.MessageListTable.COL_SUBTITLE,
          SQLHelper.MessageListTable.COL_UNREAD_CNT, SQLHelper.MessageListTable.COL_FROM_ID,
          SQLHelper.MessageListTable.COL_FROM_NAME, SQLHelper.MessageListTable.COL_FROM_TYPE,
          SQLHelper.MessageListTable.COL_FROM_CONTACT_ID, SQLHelper.MessageListTable.COL_DATE,
          SQLHelper.MessageListTable.COL_MSG_TYPE, SQLHelper.MessageListTable.COL_ACCOUNT_ID,
          SQLHelper.MessageListTable.COL_CONTENT};

  public MessageListDAO(Context context) {
    dbHelper = new SQLHelper(context);
    database = dbHelper.getWritableDatabase();
  }

  public void close() {
    database.close();
    dbHelper.close();
  }

  public void clearTable() {
    database.delete(SQLHelper.MessageListTable.TABLE_MESSAGES, null, null);
  }

  public void insertMessages(TreeSet<MessageListElement> messages, TreeMap<Account, Integer> accounts) {
    clearTable();
    for (MessageListElement mle : messages) {
      insertMessage(mle, accounts);
    }
  }

  private void insertMessage(MessageListElement mle, TreeMap<Account, Integer> accounts) {
    ContentValues cv = new ContentValues();

    cv.put(SQLHelper.MessageListTable.COL_MSG_ID, mle.getId());
    cv.put(SQLHelper.MessageListTable.COL_SEEN, mle.isSeen() ? 1 : 0);
    cv.put(SQLHelper.MessageListTable.COL_TITLE, mle.getTitle());
    cv.put(SQLHelper.MessageListTable.COL_SUBTITLE, mle.getSubTitle());
    cv.put(SQLHelper.MessageListTable.COL_UNREAD_CNT, mle.getUnreadCount());

    cv.put(SQLHelper.MessageListTable.COL_FROM_ID, mle.getFrom().getId());
    cv.put(SQLHelper.MessageListTable.COL_FROM_NAME, mle.getFrom().getName());
    cv.put(SQLHelper.MessageListTable.COL_FROM_TYPE, mle.getFrom().getType().toString());
    cv.put(SQLHelper.MessageListTable.COL_FROM_CONTACT_ID, mle.getFrom().getContactId());

    cv.put(SQLHelper.MessageListTable.COL_DATE, new Timestamp(mle.getDate().getTime()).toString());
    cv.put(SQLHelper.MessageListTable.COL_MSG_TYPE, mle.getMessageType().toString());
    cv.put(SQLHelper.MessageListTable.COL_ACCOUNT_ID, accounts.get(mle.getAccount()));
    if (mle.getMessageType().equals(MessageProvider.Type.EMAIL) || mle.getMessageType().equals(MessageProvider.Type.GMAIL)) {
      cv.put(SQLHelper.MessageListTable.COL_CONTENT, ((FullSimpleMessage)mle.getFullMessage()).getContent().getContent().toString());
    }

    if (cv != null) {
      database.insert(SQLHelper.MessageListTable.TABLE_MESSAGES, null, cv);
    }
  }

  public TreeSet<MessageListElement> getAllMessages(TreeMap<Integer, Account> accounts) {
    TreeSet<MessageListElement> messages = new TreeSet<MessageListElement>();

    Cursor cursor = database.query(SQLHelper.MessageListTable.TABLE_MESSAGES, allColumns, null, null, null, null, null);

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
      Log.d("rgai", "date for message: " + date);

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






