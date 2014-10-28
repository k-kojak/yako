package hu.rgai.yako.beens;

import android.graphics.drawable.ColorDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import hu.rgai.android.test.R;

import java.util.List;

public class GpsZone implements Parcelable, Comparable<GpsZone> {

  public enum Proximity {CLOSEST, NEAR, FAR, UNKNOWN};
  public enum ZoneType {
    WORK("Work", R.drawable.ic_important_work, 0x630000),
    REST("Rest", R.drawable.ic_important_home, 0x635241),
    SILENT("Silent", R.drawable.ic_important_silence, 0x003340)
    ;

    private final String mDisplayName;
    private final int mDrawable;
    private final int mColor;

    private ZoneType(String displayName, int drawable, int color) {
      mDisplayName = displayName;
      mDrawable = drawable;
      mColor = color;
    }

    public String getDisplayName() {
      return mDisplayName;
    }

    public int getDrawable() {
      return mDrawable;
    }

    public int getColor() {
      return mColor;
    }

  };

  private final String mAlias;
  private final double mLat;
  private final double mLong;
  private final int mRadius;
  private final ZoneType mZoneType;

  private int mDistance = -1;
  private Proximity mProximity = Proximity.UNKNOWN;


  public static final Parcelable.Creator<GpsZone> CREATOR = new Parcelable.Creator<GpsZone>() {
    public GpsZone createFromParcel(Parcel in) {
      return new GpsZone(in);
    }

    public GpsZone[] newArray(int size) {
      return new GpsZone[size];
    }
  };

  public GpsZone(Parcel in) {
    this(in.readString(), in.readDouble(), in.readDouble(), in.readInt(), ZoneType.valueOf(in.readString()));

    mDistance = in.readInt();
    mProximity = Proximity.valueOf(in.readString());
  }

  public GpsZone(String mAlias, double mLat, double mLong, int mRadius, ZoneType zoneType) {
    this.mAlias = mAlias;
    this.mLat = mLat;
    this.mLong = mLong;
    this.mRadius = mRadius;
    this.mZoneType = zoneType;
  }

  /**
   * Returns the name of the zone, alias is unique.
   * @return the alias of the zone
   */
  public String getAlias() {
    return mAlias;
  }

  public double getLat() {
    return mLat;
  }

  public double getLong() {
    return mLong;
  }

  /**
   * Returns the radius (sensitivity) of the location.
   * If current location is within this radius, this zone is considered at least Proximity.NEAR.
   * @return radius in meters
   */
  public int getRadius() {
    return mRadius;
  }

  /**
   * Proximity of zone in 4 categories.<br/>
   * <code>FAR</code>: zone is not in range<br/>
   * <code>NEAR</code>: zone is within range, but not the closest one<br/>
   * <code>CLOSEST</code>: the closest zone to actual position<br/>
   * <code>UNKNOWN</code>: in this case we do not have a valid current location so we know nothing about the proximity
   * @return the proximity of zone
   */
  public Proximity getProximity() {
    return mProximity;
  }

  public void setProximity(Proximity proximity) {
    this.mProximity = proximity;
  }

  /**
   * Returns the distance from current position in meters.
   * If value is <code>-1</code> that means there is no valid current position and distance cannot be calculated.
   * @return the distance from current position in meters, returns <code>-1</code> if no valid distance exists
   */
  public int getDistance() {
    return mDistance;
  }

  public void setDistance(int distance) {
    this.mDistance = distance;
  }

  /**
   * Returns the zone type of this gps zone.
   * Zone types can be: WORK, REST, SILENT.
   * Of course it should be editable and 1 location should be able to own multiple zone types ...
   * probably in a later release...
   * @return the zone type of the saved GpsZone
   */
  public ZoneType getZoneType() {
    return mZoneType;
  }

  @Override
  public int compareTo(GpsZone another) {
    return mAlias.compareTo(another.getAlias());

  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(mAlias);
    dest.writeDouble(mLat);
    dest.writeDouble(mLong);
    dest.writeInt(mRadius);
    dest.writeString(mZoneType.toString());
    dest.writeInt(mDistance);
    dest.writeString(mProximity.toString());
  }

  @Override
  public String toString() {
    return "GpsZone{" +
            "mAlias='" + mAlias + '\'' +
            ", mLat=" + mLat +
            ", mLong=" + mLong +
            ", mRadius=" + mRadius +
            ", mDistance=" + mDistance +
            ", mProximity=" + mProximity +
            '}';
  }

  public static GpsZone getClosest(List<GpsZone> zones) {
    GpsZone closest = null;
    for (GpsZone z : zones) {
      if (z.getProximity().equals(GpsZone.Proximity.CLOSEST)) {
        closest = z;
        break;
      }
    }
    return closest;
  }

}
