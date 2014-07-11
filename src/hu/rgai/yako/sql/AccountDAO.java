package hu.rgai.yako.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.messageproviders.MessageProvider;

import java.util.TreeMap;
import java.util.TreeSet;


public class AccountDAO  {

  private static AccountDAO instance = null;
  private SQLHelper mDbHelper = null;


  // table definitions
  public static final String TABLE_ACCOUNTS = "accounts";

  public static final String COL_ID = "_id";
  private static final String COL_TYPE = "account_type";
  // in case of email instance this holds the email, in case of Facebook instance it holds the unique number for XMPP
  private static final String COL_UNIQUE_NAME = "unique_name";
  private static final String COL_PASS = "password";
  private static final String COL_IMAP_ADDR = "imap_address";
  private static final String COL_SMTP_ADDR = "smtp_address";
  private static final String COL_IMAP_PORT = "imap_port";
  private static final String COL_SMTP_PORT = "smtp_port";
  private static final String COL_IS_SSL = "is_ssl";
  private static final String COL_FB_DISP_NAME = "fb_display_name";
  private static final String COL_FB_UNIQUE_NAME = "fb_unique_name";


  public static final String TABLE_CREATE = "CREATE TABLE " + TABLE_ACCOUNTS + "("
          + COL_ID + " integer primary key autoincrement, "
          + COL_TYPE + " text not null, "
          + COL_UNIQUE_NAME + " text, "
          + COL_PASS + " text, "
          + COL_IMAP_ADDR + " text, "
          + COL_SMTP_ADDR + " text, "
          + COL_IMAP_PORT + " integer, "
          + COL_SMTP_PORT + " integer, "
          + COL_IS_SSL + " integer, "
          + COL_FB_DISP_NAME + " text, "
          + COL_FB_UNIQUE_NAME + " text, "
          + "UNIQUE ("+ COL_UNIQUE_NAME +","+ COL_TYPE +"));";

  private String[] allColumns = { COL_ID, COL_TYPE, COL_UNIQUE_NAME, COL_PASS, COL_IMAP_ADDR, COL_SMTP_ADDR,
          COL_IMAP_PORT, COL_SMTP_PORT, COL_IS_SSL, COL_FB_DISP_NAME, COL_FB_UNIQUE_NAME };


  public static synchronized AccountDAO getInstance(Context context) {
    if (instance == null) {
      instance = new AccountDAO(context);
    }
    return instance;
  }


  private AccountDAO(Context context) {
    mDbHelper = SQLHelper.getInstance(context);
  }


  public synchronized void close() {
    mDbHelper.closeDatabase();
  }


  public synchronized void checkSMSAccount(Context context, boolean readyForSms) {
    int _id = -1;
    Cursor cursor = mDbHelper.getDatabase().query(TABLE_ACCOUNTS, new String[] {COL_ID}, COL_TYPE + " = ?",
            new String[]{MessageProvider.Type.SMS.toString()}, null, null, null);

    cursor.moveToFirst();

    while (!cursor.isAfterLast()) {
      _id = cursor.getInt(0);
      // there should be only 1 sms instance in the db, so break if we found that one
      break;
    }
    cursor.close();

    Log.d("rgai", "_id = " + _id);
    // do nothing, the sms instance is in the db and the device is a phone
    // OR instance is not in the db and device is not a phone
    if (_id != -1 && readyForSms || _id == -1 && !readyForSms) {
      Log.d("rgai", "case 1");
      SmsAccount.setInstance(_id);
    }
    // sms instance is in the db, but this is not a phone anymore (SIM card is removed?) so we have to remove that
    // instance and the messages to it
    else if (_id != -1 && !readyForSms) {
      removeAccountWithCascade(context, _id);
    }
    // SMS instance is not in DB, but the device is a phone, let's put SMS instance to DB
    else if (_id == -1 && readyForSms) {
      Log.d("rgai", "case 3");
      SmsAccount.setInstance(-1);
      long rawId = addAccount(SmsAccount.getInstance());
      SmsAccount.setInstance(rawId);
    }
  }


  public TreeMap<Account, Long> getAccountToIdMap() {
    TreeMap<Account, Long> accounts = new TreeMap<Account, Long>();
    Cursor cursor = mDbHelper.getDatabase().query(TABLE_ACCOUNTS, allColumns, null, null, null, null, null);

    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      Account account = cursorToAccount(cursor);
      accounts.put(account, cursor.getLong(0));
      cursor.moveToNext();
    }
    cursor.close();
    return accounts;
  }


  public TreeMap<Long, Account> getIdToAccountsMap() {
    TreeMap<Long, Account> accounts = new TreeMap<Long, Account>();
    Cursor cursor = mDbHelper.getDatabase().query(TABLE_ACCOUNTS, allColumns, null, null, null, null, null);

    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      Account account = cursorToAccount(cursor);
      accounts.put(cursor.getLong(0), account);
      cursor.moveToNext();
    }
    cursor.close();
    return accounts;
  }


  public synchronized void modifyAccount(Context context, Account oldAccount, Account newAccount) {
    removeAccountWithCascade(context, oldAccount.getDatabaseId());
    addAccount(newAccount);
  }


  public synchronized long addAccount(Account account) {
    ContentValues values = null;
    MessageProvider.Type accountType = account.getAccountType();
    switch (accountType) {
      case EMAIL:
        values = buildEmailContentValues((EmailAccount)account);
        break;
      case GMAIL:
        values = buildGmailContentValues((GmailAccount) account);
        break;
      case FACEBOOK:
        values = buildFacebookContentValues((FacebookAccount) account);
        break;
      case SMS:
        values = buildSMSContentValues();
        break;
      default:
        break;
    }
    if (values != null) {
      return mDbHelper.getDatabase().insert(TABLE_ACCOUNTS, null, values);
    } else {
      return -1;
    }
  }


  private ContentValues buildEmailContentValues(EmailAccount a) {
    ContentValues cv = new ContentValues();
    cv.put(COL_TYPE, a.getAccountType().toString());
    cv.put(COL_UNIQUE_NAME, a.getEmail());
    cv.put(COL_PASS, a.getPassword());
    cv.put(COL_IMAP_ADDR, a.getImapAddress());
    cv.put(COL_SMTP_ADDR, a.getSmtpAddress());
    cv.put(COL_IMAP_PORT, a.getImapPort());
    cv.put(COL_SMTP_PORT, a.getSmtpPort());
    cv.put(COL_IS_SSL, a.isSsl() ? 1 : 0);
    return cv;
  }


  private ContentValues buildGmailContentValues(GmailAccount a) {
    ContentValues cv = new ContentValues();
    cv.put(COL_TYPE, a.getAccountType().toString());
    cv.put(COL_UNIQUE_NAME, a.getEmail());
    cv.put(COL_PASS, a.getPassword());
    return cv;
  }


  private ContentValues buildSMSContentValues() {
    ContentValues cv = new ContentValues();
    cv.put(COL_TYPE, MessageProvider.Type.SMS.toString());
    return cv;
  }


  private ContentValues buildFacebookContentValues(FacebookAccount a) {
    ContentValues cv = new ContentValues();
    cv.put(COL_TYPE, a.getAccountType().toString());
    cv.put(COL_FB_DISP_NAME, a.getDisplayName());
    cv.put(COL_FB_UNIQUE_NAME, a.getUniqueName());
    cv.put(COL_UNIQUE_NAME, a.getId());
    cv.put(COL_PASS, a.getPassword());
    return cv;
  }


  private void removeAccount(long accountId) {
    mDbHelper.getDatabase().delete(TABLE_ACCOUNTS, COL_ID + " = ?", new String[] {Long.toString(accountId)});
  }


  /**
   * Deletes the given instance and the MessageListElements which belongs to this instance.
   *
   * @param context
   * @param accountId the database _id of the instance
   */
  public void removeAccountWithCascade(Context context, long accountId) {
    try {
      MessageListDAO.getInstance(context).removeMessages(context, accountId);
    } catch (Exception e) {
      Log.d("rgai", "", e);
    }
    removeAccount(accountId);
  }


  /**
   * Returns the number of accounts available in the system.
   * @return
   */
  public int getAccountCount() {
    Cursor cursor = mDbHelper.getDatabase().query(TABLE_ACCOUNTS, new String[] {"COUNT(*)"}, null, null, null, null, null);
    cursor.moveToFirst();
    int count = cursor.getInt(0);
    cursor.close();

    return count;
  }


  /**
   * Returns true if Facebook instance is present, false otherwise.
   * @return
   */
  public boolean isFacebookAccountAdded() {
    Cursor cursor = mDbHelper.getDatabase().query(TABLE_ACCOUNTS, new String[] {"COUNT(*)"}, COL_TYPE + " = ?",
            new String[] {MessageProvider.Type.FACEBOOK.toString()}, null, null, null);
    cursor.moveToFirst();
    int count = cursor.getInt(0);
    cursor.close();

    return count != 0;
  }


  /**
   * Returns all stored accounts.
   * @return
   */
  public TreeSet<Account> getAllAccounts() {
    TreeSet<Account> accounts = new TreeSet<Account>();

    Cursor cursor = getAllAccountsCursor();

    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      Account account = cursorToAccount(cursor);
      accounts.add(account);
      cursor.moveToNext();
    }
    // make sure to close the cursor
    cursor.close();
    return accounts;
  }


  /**
   * Returns a cursor for the all instance query.
   * @return
   */
  public Cursor getAllAccountsCursor() {
    return mDbHelper.getDatabase().query(TABLE_ACCOUNTS, allColumns, null, null, null, null,
            COL_TYPE + " ASC, " + COL_UNIQUE_NAME + " ASC");
  }


  /**
   * Returns an instance to a cursor.
   * @param cursor
   * @return
   */
  public static Account cursorToAccount(Cursor cursor) {
    MessageProvider.Type type = MessageProvider.Type.valueOf(cursor.getString(1));
    switch (type) {
      case EMAIL:
        return cursorToEmailAccount(cursor);
      case GMAIL:
        return cursorToGmailAccount(cursor);
      case FACEBOOK:
        return cursorToFacebookAccount(cursor);
      case SMS:
        return SmsAccount.getInstance();
      default:
        break;
    }
    return null;
  }


  private static Account cursorToEmailAccount(Cursor cursor) {
    EmailAccount account = new EmailAccount(cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5),
            cursor.getInt(6), cursor.getInt(7), cursor.getInt(8) == 1, cursor.getInt(0));
    return account;
  }


  private static Account cursorToGmailAccount(Cursor cursor) {
    GmailAccount account = new GmailAccount(cursor.getString(2), cursor.getString(3), cursor.getInt(0));
    return account;
  }


  private static Account cursorToFacebookAccount(Cursor cursor) {
    FacebookAccount account = new FacebookAccount(cursor.getString(9), cursor.getString(10), cursor.getString(2),
            cursor.getString(3), cursor.getInt(0));
    return account;
  }

}
