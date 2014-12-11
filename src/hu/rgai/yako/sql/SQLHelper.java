package hu.rgai.yako.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import hu.rgai.yako.beens.MessageListElement;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class SQLHelper extends SQLiteOpenHelper {

  private AtomicInteger mOpenCounter = new AtomicInteger();
  private static SQLHelper instance = null;
  private SQLiteDatabase mDatabase;

  private static final String DATABASE_NAME = "yako_messages";
  private static final int DATABASE_VERSION = 4;


  public static synchronized SQLHelper getInstance(Context context) {
    if (instance == null) {
      instance = new SQLHelper(context);
    }
    return instance;
  }


  private SQLHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }


  public synchronized SQLiteDatabase getDatabase() {
    if (mOpenCounter.incrementAndGet() == 1) {
      mDatabase = instance.getWritableDatabase();
    }
    return mDatabase;
  }


  public synchronized void closeDatabase() {
    if (mOpenCounter.decrementAndGet() == 0) {
      mDatabase.close();
    }
  }


  @Override
  public void onCreate(SQLiteDatabase db) {
    Log.d("rgai", "CREATING TABLES AND INDICES");
    db.execSQL(AccountDAO.TABLE_CREATE);
    createAllExceptAccounts(db);
  }

  @Override
  public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.d("rgai", "onDowngrade");
    dropAll(db);
    onCreate(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion == 1 || oldVersion == 2 || oldVersion == 3) {
      dropAllAndCreateExceptAccounts(db);
    } else {
      dropAll(db);
      onCreate(db);
    }
  }

  private void dropAllExceptAccounts(SQLiteDatabase db) {
    // dropping indexes, tables...
    db.execSQL("DROP INDEX IF EXISTS " + MessageListDAO.INDEX_ON_MSG_TYPE);
    db.execSQL("DROP INDEX IF EXISTS " + PersonDAO.INDEX_ON_KEY_TYPE);
    db.execSQL("DROP INDEX IF EXISTS " + AttachmentDAO.INDEX_ON_FILENAME);
    db.execSQL("DROP INDEX IF EXISTS " + MessageRecipientDAO.INDEX_ON_MSG_ID);

    db.execSQL("DROP TABLE IF EXISTS " + ZoneNotificationDAO.TABLE_ZONE_NOTIFICATIONS);
    db.execSQL("DROP TABLE IF EXISTS " + MessageRecipientDAO.TABLE_MESSAGE_RECIPIENT);
    db.execSQL("DROP TABLE IF EXISTS " + AttachmentDAO.TABLE_ATTACHMENTS);
    db.execSQL("DROP TABLE IF EXISTS " + FullMessageDAO.TABLE_MESSAGE_CONTENT);
    db.execSQL("DROP TABLE IF EXISTS " + MessageListDAO.TABLE_MESSAGES);
    db.execSQL("DROP TABLE IF EXISTS " + PersonDAO.TABLE_PERSON);
    db.execSQL("DROP TABLE IF EXISTS " + GpsZoneDAO.TABLE_GPS_ZONES);
  }

  private void createAllExceptAccounts(SQLiteDatabase db) {
    // recreating them...
    db.execSQL(PersonDAO.TABLE_CREATE);
    db.execSQL(FullMessageDAO.TABLE_CREATE);
    db.execSQL(MessageListDAO.TABLE_CREATE);
    db.execSQL(AttachmentDAO.TABLE_CREATE);
    db.execSQL(MessageRecipientDAO.TABLE_CREATE);
    db.execSQL(GpsZoneDAO.TABLE_CREATE);
    db.execSQL(ZoneNotificationDAO.TABLE_CREATE);

    db.execSQL(MessageListDAO.CREATE_INDEX_ON_MSG_TYPE);
    db.execSQL(PersonDAO.CREATE_INDEX_ON_KEY_TYPE);
    db.execSQL(AttachmentDAO.CREATE_INDEX_ON_FILENAME);
    db.execSQL(MessageRecipientDAO.CREATE_INDEX_ON_MSG_ID);

  }


//  private void createTableMapZones(SQLiteDatabase db) {
//    db.execSQL(GpsZoneDAO.TABLE_CREATE);
//    db.execSQL(ZoneNotificationDAO.TABLE_CREATE);
//  }

  private void dropAllAndCreateExceptAccounts(SQLiteDatabase db) {
    dropAllExceptAccounts(db);
    createAllExceptAccounts(db);
  }


  private void dropAll(SQLiteDatabase db) {
    dropAllExceptAccounts(db);
    db.execSQL("DROP TABLE IF EXISTS " + AccountDAO.TABLE_ACCOUNTS);
  }

  public static class Utils {

    public static String getInClosureFromListElement(Collection<MessageListElement> collection, boolean isString) {
      StringBuilder sb = new StringBuilder();
      sb.append("(");
      int i = 0;
      for (MessageListElement s : collection) {
        if (i > 0) {
          sb.append(",");
        }
        if (isString) {
          sb.append("\"");
        }
        sb.append(s.getRawId());
        if (isString) {
          sb.append("\"");
        }
        i++;
      }
      sb.append(")");
      return sb.toString();
    }
    
    public static<T> String getInClosure(Collection<T> collection, boolean isString) {
      StringBuilder sb = new StringBuilder();
      sb.append("(");
      int i = 0;
      for (T s : collection) {
        if (i > 0) {
          sb.append(",");
        }
        if (isString) {
          sb.append("\"");
        }
        sb.append(s);
        if (isString) {
          sb.append("\"");
        }
        i++;
      }
      sb.append(")");
      return sb.toString();
    }

    public static String getInClosureFromListElement(Collection<MessageListElement> collection) {
      return getInClosureFromListElement(collection, false);
    }
    
    public static<T> String getInClosure(Collection<T> collection) {
      return getInClosure(collection, false);
    }

    public static Date parseSQLdateString(String sqlDate) throws ParseException {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(sqlDate);
    }

  }
}

