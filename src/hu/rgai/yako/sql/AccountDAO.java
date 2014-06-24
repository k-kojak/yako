package hu.rgai.yako.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import hu.rgai.yako.beens.*;
import hu.rgai.yako.messageproviders.MessageProvider;

import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by kojak on 6/23/2014.
 */
public class AccountDAO  {

  private SQLiteDatabase database;
  private SQLHelper dbHelper;
  private String[] allColumns = { SQLHelper.AccountTable.COL_ID, SQLHelper.AccountTable.COL_TYPE,
          SQLHelper.AccountTable.COL_UNIQUE_NAME, SQLHelper.AccountTable.COL_PASS, SQLHelper.AccountTable.COL_IMAP_ADDR,
          SQLHelper.AccountTable.COL_SMTP_ADDR, SQLHelper.AccountTable.COL_IMAP_PORT, SQLHelper.AccountTable.COL_SMTP_PORT,
          SQLHelper.AccountTable.COL_IS_SSL, SQLHelper.AccountTable.COL_FB_DISP_NAME, SQLHelper.AccountTable.COL_FB_UNIQUE_NAME };

  public AccountDAO(Context context) {
    dbHelper = new SQLHelper(context);
    database = dbHelper.getWritableDatabase();
  }

  public void close() {
    database.close();
    dbHelper.close();
  }

  public void insertAccounts(TreeSet<Account> accounts) {
    clearTable();
    for (Account a : accounts) {
      insertAccount(a);
    }
  }

  public TreeMap<Account, Integer> getAccountToIdMap() {
    TreeMap<Account, Integer> accounts = new TreeMap<Account, Integer>();
    Cursor cursor = database.query(SQLHelper.AccountTable.TABLE_ACCOUNTS, allColumns, null, null, null, null, null);

    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      Account account = cursorToAccount(cursor);
      accounts.put(account, cursor.getInt(0));
      cursor.moveToNext();
    }
    cursor.close();
    return accounts;
  }

  public TreeMap<Integer, Account> getIdToAccountsMap() {
    TreeMap<Integer, Account> accounts = new TreeMap<Integer, Account>();
    Cursor cursor = database.query(SQLHelper.AccountTable.TABLE_ACCOUNTS, allColumns, null, null, null, null, null);

    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      Account account = cursorToAccount(cursor);
      accounts.put(cursor.getInt(0), account);
      cursor.moveToNext();
    }
    cursor.close();
    return accounts;
  }

  private void insertAccount(Account account) {
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
      database.insert(SQLHelper.AccountTable.TABLE_ACCOUNTS, null, values);
    }
  }

  private ContentValues buildEmailContentValues(EmailAccount a) {
    ContentValues cv = new ContentValues();
    cv.put(SQLHelper.AccountTable.COL_TYPE, a.getAccountType().toString());
    cv.put(SQLHelper.AccountTable.COL_UNIQUE_NAME, a.getEmail());
    cv.put(SQLHelper.AccountTable.COL_PASS, a.getPassword());
    cv.put(SQLHelper.AccountTable.COL_IMAP_ADDR, a.getImapAddress());
    cv.put(SQLHelper.AccountTable.COL_SMTP_ADDR, a.getSmtpAddress());
    cv.put(SQLHelper.AccountTable.COL_IMAP_PORT, a.getImapPort());
    cv.put(SQLHelper.AccountTable.COL_SMTP_PORT, a.getSmtpPort());
    cv.put(SQLHelper.AccountTable.COL_IS_SSL, a.isSsl() ? 1 : 0);
    return cv;
  }

  private ContentValues buildGmailContentValues(GmailAccount a) {
    ContentValues cv = new ContentValues();
    cv.put(SQLHelper.AccountTable.COL_TYPE, a.getAccountType().toString());
    cv.put(SQLHelper.AccountTable.COL_UNIQUE_NAME, a.getEmail());
    cv.put(SQLHelper.AccountTable.COL_PASS, a.getPassword());
    return cv;
  }

  private ContentValues buildSMSContentValues() {
    ContentValues cv = new ContentValues();
    cv.put(SQLHelper.AccountTable.COL_TYPE, MessageProvider.Type.SMS.toString());
    return cv;
  }

  private ContentValues buildFacebookContentValues(FacebookAccount a) {
    ContentValues cv = new ContentValues();
    cv.put(SQLHelper.AccountTable.COL_TYPE, a.getAccountType().toString());
    cv.put(SQLHelper.AccountTable.COL_FB_DISP_NAME, a.getDisplayName());
    cv.put(SQLHelper.AccountTable.COL_FB_UNIQUE_NAME, a.getUniqueName());
    cv.put(SQLHelper.AccountTable.COL_UNIQUE_NAME, a.getId());
    cv.put(SQLHelper.AccountTable.COL_PASS, a.getPassword());
    return cv;
  }

  public void clearTable() {
    database.delete(SQLHelper.AccountTable.TABLE_ACCOUNTS, null, null);
  }

  public TreeSet<Account> getAllAccounts() {
    TreeSet<Account> accounts = new TreeSet<Account>();

    Cursor cursor = database.query(SQLHelper.AccountTable.TABLE_ACCOUNTS,
            null, null, null, null, null, null);

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

  private Account cursorToAccount(Cursor cursor) {
    MessageProvider.Type type = MessageProvider.Type.valueOf(cursor.getString(1));
    switch (type) {
      case EMAIL:
        return cursorToEmailAccount(cursor);
      case GMAIL:
        return cursorToGmailAccount(cursor);
      case FACEBOOK:
        return cursorToFacebookAccount(cursor);
      case SMS:
        return SmsAccount.account;
      default:
        break;
    }
    return null;
  }

  private Account cursorToEmailAccount(Cursor cursor) {
    EmailAccount account = new EmailAccount(cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5),
            cursor.getInt(6), cursor.getInt(7), cursor.getInt(8) == 1);
    return account;
  }

  private Account cursorToGmailAccount(Cursor cursor) {
    GmailAccount account = new GmailAccount(cursor.getString(2), cursor.getString(3));
    return account;
  }

  private Account cursorToFacebookAccount(Cursor cursor) {
    FacebookAccount account = new FacebookAccount(cursor.getString(9), cursor.getString(10), cursor.getString(2),
            cursor.getString(3));
    return account;
  }

}
