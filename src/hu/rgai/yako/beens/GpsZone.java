package hu.rgai.yako.beens;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kojak on 9/23/2014.
 */
public class GpsZone implements Parcelable {

  public enum Proximity {CLOSEST, NEAR, FAR, UNKNOWN};

  private final String mAlias;
  private final double mLat;
  private final double mLong;
  private final int mRadius;

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
    this(in.readString(), in.readDouble(), in.readDouble(), in.readInt());

    mDistance = in.readInt();
    mProximity = Proximity.valueOf(in.readString());
  }

  public GpsZone(String mAlias, double mLat, double mLong, int mRadius) {
    this.mAlias = mAlias;
    this.mLat = mLat;
    this.mLong = mLong;
    this.mRadius = mRadius;
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
}
