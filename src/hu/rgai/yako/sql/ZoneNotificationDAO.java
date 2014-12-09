package hu.rgai.yako.sql;

import java.util.Set;
import java.util.TreeSet;

import hu.rgai.yako.beens.Account;
import hu.rgai.yako.beens.GpsZone;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class ZoneNotificationDAO {
  
  private static ZoneNotificationDAO instance = null;
  private SQLHelper mDbHelper = null;
  private Context mContext = null;
  
  
  public static final String TABLE_ZONE_NOTIFICATIONS = "zone_notifications";

  public static final String COL_ID = "_id";
  private static final String COL_ACCOUNT_ID = AccountDAO.TABLE_ACCOUNTS + AccountDAO.COL_ID;
  private static final String COL_ZONE_ID = GpsZoneDAO.TABLE_GPS_ZONES + GpsZoneDAO.COL_ID;
  private static final String COL_CHECKED = "is_checked";

  
  public static final String TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_ZONE_NOTIFICATIONS + "("
      + COL_ID + " INTEGER primary key autoincrement, "
      + COL_ACCOUNT_ID + " integer NOT NULL, "
      + COL_ZONE_ID + " integer NOT NULL,"
      + COL_CHECKED + " integer, "
      + " FOREIGN KEY (" + COL_ACCOUNT_ID + ") REFERENCES "
      + AccountDAO.TABLE_ACCOUNTS + "(" + AccountDAO.COL_ID + "),"
      + " FOREIGN KEY (" + COL_ZONE_ID + ") REFERENCES "
      + GpsZoneDAO.TABLE_GPS_ZONES + "(" + GpsZoneDAO.COL_ID + ")"
      + ");";
  
  private String[] allColumns = { COL_ID, COL_ACCOUNT_ID, COL_ZONE_ID, COL_CHECKED };
  
  public static synchronized ZoneNotificationDAO getInstance(Context context) {
    if (instance == null) {
      instance = new ZoneNotificationDAO(context);
    }
    return instance;
  }

  private ZoneNotificationDAO(Context context) {
    mContext = context;
    mDbHelper = SQLHelper.getInstance(context);
  }
  
  public synchronized void close() {
    mDbHelper.closeDatabase();
  }
  
  public void removeByZones(long zone) {
    mDbHelper.getDatabase().delete(TABLE_ZONE_NOTIFICATIONS, COL_ZONE_ID + " = ?", new String[] {Long.toString(zone)});
  }
  
  public void removeByAccounts(long account) {
    mDbHelper.getDatabase().delete(TABLE_ZONE_NOTIFICATIONS, COL_ACCOUNT_ID + " = ?", new String[] {Long.toString(account)});
  }
  
  public void removeByAccountAndZone(long account,long zone) {
    mDbHelper.getDatabase().delete(TABLE_ZONE_NOTIFICATIONS, COL_ACCOUNT_ID + " = ? AND " + COL_ZONE_ID + " = ?", new String[] {Long.toString(account), Long.toString(zone)});
  }
  
  private static ContentValues buildContentValues(long account,long zone, boolean isChecked) {
    ContentValues cv = new ContentValues();
    cv.put(COL_ACCOUNT_ID, account);
    cv.put(COL_ZONE_ID, zone);
    cv.put(COL_CHECKED, isChecked ? 1 : 0);
    return cv;
  }
  
  
  public void saveNotificationSettingByAccount(long account) {
    
    ContentValues cv = new ContentValues();
    cv.put(COL_ACCOUNT_ID, account);
    cv.put(COL_CHECKED, 1);
    
    Cursor c = GpsZoneDAO.getInstance(mContext).getAllZonesCursor();
    c.moveToFirst();
    while (!c.isAfterLast()) {
      cv.put(COL_ZONE_ID, c.getInt(0));
      mDbHelper.getDatabase().insert(TABLE_ZONE_NOTIFICATIONS, null, cv);
      c.moveToNext();
    }
    c.close();
    

  }
  
  public void saveNotificationSettingToZone(long account,long zone, boolean isChecked) {
    ContentValues cv = buildContentValues(account, zone, isChecked);
    System.out.println("amit betsz az egy : " + isChecked);
    mDbHelper.getDatabase().insert(TABLE_ZONE_NOTIFICATIONS, null, cv);
  }
  
  public void updateNotificationSettingToZone(long account,long zone, boolean isChecked) {
    ContentValues cv = buildContentValues(account,zone, isChecked);
    mDbHelper.getDatabase().update(TABLE_ZONE_NOTIFICATIONS, cv, COL_ACCOUNT_ID + " = ? AND " + COL_ZONE_ID + " = ?", new String[]{Long.toString(account), Long.toString(zone)});
  }
  
  public boolean getNotificationCheckedByZoneAndAccount(long zone, long account) {
    
    boolean checked = false;
    Cursor c = mDbHelper.getDatabase().query(TABLE_ZONE_NOTIFICATIONS, allColumns, COL_ACCOUNT_ID + " = ? AND " + COL_ZONE_ID + " = ?", new String[] {Long.toString(account), Long.toString(zone)}, null, null, null);
    c.moveToFirst();
    checked = (c.getInt(3) == 1) ? true : false ;
    c.close();
    return checked;
  }
  
  public Cursor getAllNotificationsToZoneCursor(long zone) {
    return mDbHelper.getDatabase().query(TABLE_ZONE_NOTIFICATIONS, allColumns, COL_ZONE_ID + " = ?", new String[] {Long.toString(zone)}, null, null, null);
  }
}
