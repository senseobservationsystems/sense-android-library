package nl.sense_os.service.motion;


public class AccelerationFilter {
  
  private float[] gravity = { 0, 0, 0 };
  
  /**
   * Calculates the linear acceleration of a raw accelerometer sample. Tries to determine the
   * gravity component by putting the signal through a first-order low-pass filter.
   * 
   * @param values
   *            Array with accelerometer values for the three axes.
   * @return The approximate linear acceleration of the sample.
   */
  public float[] calcLinAcc(float[] values) {

      // low-pass filter raw accelerometer data to approximate the gravity
      final float alpha = 0.85f; // filter constants should depend on sample rate
      gravity[0] = alpha * gravity[0] + (1 - alpha) * values[0];
      gravity[1] = alpha * gravity[1] + (1 - alpha) * values[1];
      gravity[2] = alpha * gravity[2] + (1 - alpha) * values[2];

      return new float[] { values[0] - gravity[0], values[1] - gravity[1], values[2] - gravity[2] };
  }
  
}
