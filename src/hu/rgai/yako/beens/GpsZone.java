package hu.rgai.yako.beens;

/**
 * Created by kojak on 9/23/2014.
 */
public class GpsZone {

  public enum Proximity {CLOSEST, NEAR, FAR};

  private final String mAlias;
  private final double mLat;
  private final double mLong;
  private final int mRadius;
  private Proximity mProximity = Proximity.FAR;

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
}
