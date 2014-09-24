package hu.rgai.yako.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import hu.rgai.yako.beens.GpsZone;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by kojak on 9/23/2014.
 */
public class GpsZoneDAO {

  private static GpsZoneDAO instance = null;
  private SQLHelper mDbHelper = null;


  // table definitions
  public static final String TABLE_GPS_ZONES = "gps_zones";

  public static final String COL_ID = "_id";
  private static final String COL_ALIAS = "alias";
  private static final String COL_LAT = "lat";
  private static final String COL_LONG = "long";
  private static final String COL_RADIUS = "radius";


  public static final String TABLE_CREATE = "CREATE TABLE " + TABLE_GPS_ZONES + "("
          + COL_ID + " INTEGER primary key autoincrement, "
          + COL_ALIAS + " TEXT not null, "
          + COL_LAT + " REAL not null, "
          + COL_LONG + " REAL not null, "
          + COL_RADIUS + " INTEGER not null, "
          + "UNIQUE ("+ COL_ALIAS +"));";

  private String[] allColumns = { COL_ID, COL_ALIAS, COL_LAT, COL_LONG, COL_RADIUS };


  public static synchronized GpsZoneDAO getInstance(Context context) {
    if (instance == null) {
      instance = new GpsZoneDAO(context);
    }
    return instance;
  }

  public boolean isZoneAliasExists(String alias) {
    Cursor cursor = mDbHelper.getDatabase().query(TABLE_GPS_ZONES, new String[] {"COUNT(*)"}, COL_ALIAS + " = ?",
            new String[] {alias}, null, null, null);
    cursor.moveToFirst();
    int count = cursor.getInt(0);
    cursor.close();

    return count != 0;
  }

  public void saveZone(GpsZone zone) {
    ContentValues cv = buildContentValues(zone);
    mDbHelper.getDatabase().insert(TABLE_GPS_ZONES, null, cv);
  }

  public void updateZone(String aliasToUpdate, GpsZone zone) {
    ContentValues cv = buildContentValues(zone);
    mDbHelper.getDatabase().update(TABLE_GPS_ZONES, cv, COL_ALIAS + " = ?", new String[]{aliasToUpdate});
  }

  public void removeZoneByAlias(String alias) {
    mDbHelper.getDatabase().delete(TABLE_GPS_ZONES, COL_ALIAS + " = ?", new String[] {alias});
  }

  public Cursor getAllZonesCursor() {
    return mDbHelper.getDatabase().query(TABLE_GPS_ZONES, allColumns, null, null, null, null,
            COL_ALIAS + " ASC");
  }

  public List<GpsZone> getAllZones() {
    List<GpsZone> zones = new LinkedList<GpsZone>();
    Cursor c = getAllZonesCursor();
    c.moveToFirst();
    while (!c.isAfterLast()) {
      zones.add(cursorToGpsZone(c));
      c.moveToNext();
    }
    c.close();

    return zones;
  }

  private static ContentValues buildContentValues(GpsZone zone) {
    ContentValues cv = new ContentValues();
    cv.put(COL_ALIAS, zone.getAlias());
    cv.put(COL_LAT, zone.getLat());
    cv.put(COL_LONG, zone.getLong());
    cv.put(COL_RADIUS, zone.getRadius());

    return cv;
  }

  public static GpsZone cursorToGpsZone(Cursor cursor) {
    GpsZone zone = new GpsZone(cursor.getString(1), cursor.getDouble(2), cursor.getDouble(3), cursor.getInt(4));
    return zone;
  }


  private GpsZoneDAO(Context context) {
    mDbHelper = SQLHelper.getInstance(context);
  }


  public synchronized void close() {
    mDbHelper.closeDatabase();
  }
}
