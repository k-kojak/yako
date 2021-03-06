package hu.rgai.yako.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import hu.rgai.android.test.R;
import hu.rgai.yako.beens.Person;
import hu.rgai.yako.messageproviders.MessageProvider;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by kojak on 7/2/2014.
 */
public class PersonDAO {

  private static PersonDAO instance = null;
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

  public static synchronized PersonDAO getInstance(Context context) {
    if (instance == null) {
      instance = new PersonDAO(context);
    }
    return instance;
  }


  private PersonDAO(Context context) {
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
  public long getOrInsertPerson(Context context, Person person) {
    if (person.getName() == null) {
      person.setName(context.getString(R.string.unknown_name));
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

  public List<Person> getPersonsById(List<Integer> ids) {
    List<Person> persons = new LinkedList<>();

    String inClosure = SQLHelper.Utils.getInClosure(ids);
    Cursor cursor = mDbHelper.getDatabase().query(TABLE_PERSON, allColumns,
            COL_ID + " IN " + inClosure, null, null, null, COL_NAME);
    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      Person p = new Person(-1,
              cursor.getString(1),
              cursor.getString(2),
              MessageProvider.Type.valueOf(cursor.getString(4))
              );
      persons.add(p);
      cursor.moveToNext();
    }

    return persons;
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
