
package hu.rgai.android.tools;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class Utils {

  private static final String randStrings = "abcdefghijlkmnopqrstuvwxyzABCDEFGHIJLKMNOPQRSTUVWXYZ0123456789_";
  
  public static String generateString(int length) {
    StringBuilder sb = new StringBuilder();
    int sl = randStrings.length();
    for (int i = 0; i < length; i++) {
      sb.append(randStrings.indexOf((int)(Math.random() * sl)));
    }
    
    
    return sb.toString();
  }
  
}
