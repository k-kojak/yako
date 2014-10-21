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
    db.execSQL(MessageListDAO.TABLE_CREATE);
    db.execSQL(PersonSenderDAO.TABLE_CREATE);
    db.execSQL(FullMessageDAO.TABLE_CREATE);
    db.execSQL(AttachmentDAO.TABLE_CREATE);


    db.execSQL(MessageListDAO.CREATE_INDEX_ON_MSG_TYPE);
    db.execSQL(AttachmentDAO.CREATE_INDEX_ON_FILENAME);
    db.execSQL(PersonSenderDAO.CREATE_INDEX_ON_KEY_TYPE);

    createTableMapZones(db);
  }

  @Override
  public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.d("rgai", "onDowngrade");
    dropAll(db);
    onCreate(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion == 1 || oldVersion == 2) {
      dropAllAndCreateExceptAccounts(db);
      createTableMapZones(db);

      // we dont need this step here, because tables are recreated anyway...
      // alterTableMessageListElement_predictionVal(db);
    } else if (oldVersion == 3) {
      createTableMapZones(db);
      alterTableMessageListElement_predictionVal(db);
    } else {
      dropAll(db);
      onCreate(db);
    }
  }

  private void dropAllAndCreateExceptAccounts(SQLiteDatabase db) {
    // dropping indexes, tables...
    db.execSQL("DROP INDEX IF EXISTS " + MessageListDAO.INDEX_ON_MSG_TYPE);
    db.execSQL("DROP INDEX IF EXISTS " + PersonSenderDAO.INDEX_ON_KEY_TYPE);
    db.execSQL("DROP INDEX IF EXISTS " + AttachmentDAO.INDEX_ON_FILENAME);

    db.execSQL("DROP TABLE IF EXISTS " + AttachmentDAO.TABLE_ATTACHMENTS);
    db.execSQL("DROP TABLE IF EXISTS " + FullMessageDAO.TABLE_MESSAGE_CONTENT);
    db.execSQL("DROP TABLE IF EXISTS " + MessageListDAO.TABLE_MESSAGES);
    db.execSQL("DROP TABLE IF EXISTS " + PersonSenderDAO.TABLE_PERSON);



    // recreating them...
    db.execSQL(PersonSenderDAO.TABLE_CREATE);
    db.execSQL(FullMessageDAO.TABLE_CREATE);
    db.execSQL(MessageListDAO.TABLE_CREATE);
    db.execSQL(AttachmentDAO.TABLE_CREATE);

    db.execSQL(MessageListDAO.CREATE_INDEX_ON_MSG_TYPE);
    db.execSQL(PersonSenderDAO.CREATE_INDEX_ON_KEY_TYPE);
    db.execSQL(AttachmentDAO.CREATE_INDEX_ON_FILENAME);
  }



  private void createTableMapZones(SQLiteDatabase db) {
    db.execSQL(GpsZoneDAO.TABLE_CREATE);
  }

  private void alterTableMessageListElement_predictionVal(SQLiteDatabase db) {
    Log.d("rgai", "alterTableMessageListElement_predictionVal");
    db.execSQL(MessageListDAO.ALTER_TABLE_PREDICTION);
  }

  private void dropAll(SQLiteDatabase db) {
    db.execSQL("DROP INDEX IF EXISTS " + MessageListDAO.INDEX_ON_MSG_TYPE);
    db.execSQL("DROP INDEX IF EXISTS " + AttachmentDAO.INDEX_ON_FILENAME);
    db.execSQL("DROP INDEX IF EXISTS " + PersonSenderDAO.INDEX_ON_KEY_TYPE);


    db.execSQL("DROP TABLE IF EXISTS " + AttachmentDAO.TABLE_ATTACHMENTS);
    db.execSQL("DROP TABLE IF EXISTS " + FullMessageDAO.TABLE_MESSAGE_CONTENT);
    db.execSQL("DROP TABLE IF EXISTS " + PersonSenderDAO.TABLE_PERSON);
    db.execSQL("DROP TABLE IF EXISTS " + MessageListDAO.TABLE_MESSAGES);
    db.execSQL("DROP TABLE IF EXISTS " + AccountDAO.TABLE_ACCOUNTS);
    db.execSQL("DROP TABLE IF EXISTS " + GpsZoneDAO.TABLE_GPS_ZONES);
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
    
    public static String getInClosure(Collection<Long> collection, boolean isString) {
      StringBuilder sb = new StringBuilder();
      sb.append("(");
      int i = 0;
      for (Long s : collection) {
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
    
    public static String getInClosure(Collection<Long> collection) {
      return getInClosure(collection, false);
    }

    public static Date parseSQLdateString(String sqlDate) throws ParseException {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(sqlDate);
    }

  }
}

