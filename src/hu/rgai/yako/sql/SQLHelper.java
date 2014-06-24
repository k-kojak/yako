package hu.rgai.yako.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Message;

/**
 * Created by kojak on 6/24/2014.
 */
public class SQLHelper extends SQLiteOpenHelper {

  private static final String DATABASE_NAME = "yako_messages";
  private static final int DATABASE_VERSION = 2;

  public static class MessageListTable {

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



    private static final String TABLE_CREATE = "create table " + TABLE_MESSAGES + "("
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
            " FOREIGN KEY ("+ COL_ACCOUNT_ID +") REFERENCES "+ AccountTable.TABLE_ACCOUNTS +"("+ AccountTable.COL_ID +"));";

  }

  public static class AccountTable {
    public static final String TABLE_ACCOUNTS = "accounts";

    public static final String COL_ID = "_id";
    public static final String COL_TYPE = "account_type";
    // in case of email account this holds the email, in case of Facebook account it holds the unique number for XMPP
    public static final String COL_UNIQUE_NAME = "unique_name";
    public static final String COL_PASS = "password";
    public static final String COL_IMAP_ADDR = "imap_address";
    public static final String COL_SMTP_ADDR = "smtp_address";
    public static final String COL_IMAP_PORT = "imap_port";
    public static final String COL_SMTP_PORT = "smtp_port";
    public static final String COL_IS_SSL = "is_ssl";
    public static final String COL_FB_DISP_NAME = "fb_display_name";
    public static final String COL_FB_UNIQUE_NAME = "fb_unique_name";


    private static final String TABLE_CREATE = "CREATE TABLE " + TABLE_ACCOUNTS + "("
            + COL_ID + " integer primary key autoincrement, "
            + COL_TYPE + " text not null, "
            + COL_UNIQUE_NAME + " text, "
            + COL_PASS + " text,"
            + COL_IMAP_ADDR + " text,"
            + COL_SMTP_ADDR + " text,"
            + COL_IMAP_PORT + " integer,"
            + COL_SMTP_PORT + " integer,"
            + COL_IS_SSL + " integer,"
            + COL_FB_DISP_NAME + " text,"
            + COL_FB_UNIQUE_NAME + " text);";
  }

  public SQLHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(AccountTable.TABLE_CREATE);
    db.execSQL(MessageListTable.TABLE_CREATE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS " + MessageListTable.TABLE_MESSAGES);
    db.execSQL("DROP TABLE IF EXISTS " + AccountTable.TABLE_ACCOUNTS);
    onCreate(db);
  }
}
