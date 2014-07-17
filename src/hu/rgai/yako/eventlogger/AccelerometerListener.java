package hu.rgai.yako.eventlogger;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class AccelerometerListener implements SensorEventListener {

  private boolean mInitialized = false;
  private final float NOISE = (float) 1.0;
  private float mLastX, mLastY, mLastZ;


  public void onAccuracyChanged(Sensor sensor, int accuracy) {

  }

  public void onSensorChanged(SensorEvent event) {

    // check sensor type
    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

      // assign directions
      float x = event.values[0];
      float y = event.values[1];
      float z = event.values[2];


      if (!mInitialized) {
        mLastX = x;
        mLastY = y;
        mLastZ = z;
        mInitialized = true;

      } else {

        StringBuilder sb = new StringBuilder("Sensor ");
        float deltaX = Math.abs(mLastX - x);
        float deltaY = Math.abs(mLastY - y);
        float deltaZ = Math.abs(mLastZ - z);
        boolean hasAccData = false;


        if (deltaX > NOISE) {
          sb.append("X : ");
          sb.append(deltaX);
          sb.append(" ");
          hasAccData = true;
        }

        if (deltaY > NOISE) {
          sb.append("Y : ");
          sb.append(deltaY);
          sb.append(" ");
          hasAccData = true;
          ;
        }

        if (deltaZ > NOISE) {
          sb.append("Z : ");
          sb.append(deltaZ);
          sb.append(" ");
          hasAccData = true;
          ;
        }

        mLastX = x;
        mLastY = y;
        mLastZ = z;

        if (hasAccData == true) {
          EventLogger.INSTANCE.writeToLogFile(sb.toString(), true);
        }
      }
    }
  }
}
