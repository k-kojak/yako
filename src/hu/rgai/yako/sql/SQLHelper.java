package hu.rgai.yako.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Message;
import android.util.Log;

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
  private static final int DATABASE_VERSION = 1;


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

    db.execSQL(MessageListDAO.CREATE_INDEX_ON_MSG_TYPE);
  }

  @Override
  public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.d("rgai", "onDowngrade");
    dropAll(db);
    onCreate(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.d("rgai", "onUpgrade");
    dropAll(db);
    onCreate(db);
  }

  private void dropAll(SQLiteDatabase db) {
    db.execSQL("DROP INDEX IF EXISTS " + MessageListDAO.INDEX_ON_MSG_TYPE);

    db.execSQL("DROP TABLE IF EXISTS " + MessageListDAO.TABLE_MESSAGES);
    db.execSQL("DROP TABLE IF EXISTS " + AccountDAO.TABLE_ACCOUNTS);


  }
}
