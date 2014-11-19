package hu.rgai.yako.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import hu.rgai.yako.beens.Person;

/**
 * Created by kojak on 7/2/2014.
 */
public class PersonSenderDAO {

  private static PersonSenderDAO instance = null;
  private SQLHelper mDbHelper = null;



  // table definitions
  public static final String TABLE_PERSON = "person_sender";

  public static final String COL_ID = "_id";
  // this column holds the email, phone number, facebook id of the user
  public static final String COL_KEY = "person_key";
  // name of the user if exists
  public static final String COL_NAME = "name";
  // a store for a secondary name, i.e. Facebook user's have a nice, unique name for the users
  public static final String COL_SECONDARY_NAME = "secondary_name";
  public static final String COL_TYPE = "type";



  public static final String TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_PERSON + "("
          + COL_ID + " integer primary key autoincrement, "
          + COL_KEY + " text not null, "
          + COL_NAME + " text, "
          + COL_SECONDARY_NAME + " text, "
          + COL_TYPE + " text not null,"
          + " UNIQUE ("+ COL_KEY +", "+ COL_TYPE +", "+ COL_NAME +")" +
          ");";


  public static final String INDEX_ON_KEY_TYPE = TABLE_PERSON + "__" + COL_KEY + "_" + COL_TYPE + "__idx";

  public static final String CREATE_INDEX_ON_KEY_TYPE = "CREATE INDEX " + INDEX_ON_KEY_TYPE
          + " ON " + TABLE_PERSON + "(" + COL_KEY + "," + COL_TYPE + ","+ COL_NAME +");";

  private String[] allColumns = { COL_ID, COL_KEY, COL_NAME, COL_SECONDARY_NAME, COL_TYPE };

  public static synchronized PersonSenderDAO getInstance(Context context) {
    if (instance == null) {
      instance = new PersonSenderDAO(context);
    }
    return instance;
  }


  private PersonSenderDAO(Context context) {
    mDbHelper = SQLHelper.getInstance(context);
  }


  public synchronized void close() {
    mDbHelper.closeDatabase();
  }


  /**
   * Returns the person's raw database id if person already in database, otherwise inserts the Person and returns the
   * id after insertion.
   * @param person
   * @return
   */
  public long getOrInsertPerson(Person person) {
    if (person.getName() == null) {
      person.setName("<Unknown name>");
    }
    SQLiteDatabase db = mDbHelper.getDatabase();
    db.beginTransaction();
    long _id = getPersonRawId(person);
    if (_id == -1) {
      _id = insertPerson(person);
    }
    db.setTransactionSuccessful();
    db.endTransaction();
    return _id;
  }


  public long insertPerson(Person person) {
    ContentValues cv = new ContentValues();
    cv.put(COL_KEY, person.getId());
    cv.put(COL_NAME, person.getName());
    cv.put(COL_SECONDARY_NAME, person.getSecondaryName());
    cv.put(COL_TYPE, person.getType().toString());
    return mDbHelper.getDatabase().insert(TABLE_PERSON, null, cv);
  }


  public long getPersonRawId(Person person) {
    long _id = -1;
    Cursor cursor = mDbHelper.getDatabase().query(TABLE_PERSON, new String[] {COL_ID},
            COL_KEY + " = ? AND " + COL_TYPE + " = ? AND " + COL_NAME + " = ?",
            new String[]{person.getId(), person.getType().toString(), person.getName()},
            null, null, null);
    cursor.moveToFirst();
    if (!cursor.isAfterLast()) {
      _id = cursor.getLong(0);
    }

    return _id;
  }

}
