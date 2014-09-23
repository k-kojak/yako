package hu.rgai.yako.beens;

/**
 * Created by kojak on 9/23/2014.
 */
public class GpsZone {

  private final String mAlias;
  private final double mLat;
  private final double mLong;
  private final int mRadius;

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
}
