package hu.rgai.yako.beens;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kojak on 9/23/2014.
 */
public class GpsZone implements Parcelable {

  public enum Proximity {CLOSEST, NEAR, FAR};

  private final String mAlias;
  private final double mLat;
  private final double mLong;
  private final int mRadius;

  private int mDistance = -1;
  private Proximity mProximity = Proximity.FAR;

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

  public String getAlias() {
    return mAlias;
  }

  public double getLat() {
    return mLat;
  }

  public double getLong() {
    return mLong;
  }

  public int getRadius() {
    return mRadius;
  }

  public Proximity getProximity() {
    return mProximity;
  }

  public void setProximity(Proximity proximity) {
    this.mProximity = proximity;
  }

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

}
