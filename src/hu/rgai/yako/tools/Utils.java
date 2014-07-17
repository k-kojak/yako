
package hu.rgai.yako.tools;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.ocpsoft.prettytime.PrettyTime;

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

  public static String joinString(String[] values, String glue) {
    return joinString(new ArrayList<String>(Arrays.asList(values)), glue);
  }

  public static String joinString(Collection<String> coll, String glue) {
    if (coll == null) {
      return null;
    } else if (coll.size() == 0) {
      return "";
    } else {
      StringBuilder sb = new StringBuilder();
      int i = 0;
      for (String s : coll) {
        if (i > 0) {
          sb.append(glue);
        }
        sb.append(s);
        i++;
      }

      return sb.toString();
    }
  }
  
  public static String getPrettyFileSize(long bytes) {
    String[] units = {"kB", "MB", "GB"};
    String value;
    double size = bytes / 1024.0;
    int i;
    for (i = 0; i < units.length; i++) {
      if (size > 1024) {
        size /= 1024.0;
      } else {
        break;
      }
    }
    if (i >= units.length) {
      i = units.length - 1;
    }
    
    DecimalFormat df = new DecimalFormat("####.#");
  
    value = df.format(size) + " " + units[i];
    
    return value;
  }
  
  public static String getPrettyTime(Date date) {
    PrettyTime pt = new PrettyTime();
    return pt.format(date);
  }
  
  public static String getSimplifiedTime(Date date) {
    Calendar now = Calendar.getInstance();
    Calendar comp = new GregorianCalendar();
    comp.setTime(date);
    
    int Y = Calendar.YEAR;
    int M = Calendar.MONTH;
    int D = Calendar.DAY_OF_MONTH;
    int H = Calendar.HOUR_OF_DAY;
    int I = Calendar.MINUTE;
    
    String s = "";
    String time = add0(comp.get(H))+":"+add0(comp.get(I));
    if (now.get(Y) == comp.get(Y)) {
      if (now.get(M) == comp.get(M)) {
        if (now.get(D) == comp.get(D)) {
          s = "Today, " + time;
        } else {
          s = new SimpleDateFormat("MMM dd").format(date) + ", " + time;
        }
      } else {
        s = new SimpleDateFormat("MMM dd").format(date) + ", " + time;
      }
    } else {
      s = new SimpleDateFormat("y MMM dd").format(date) + ", " + time;
    }
    
    return s;
  }
  
  private static String add0(int i) {
    if (i > 9) {
      return i+"";
    } else {
      return "0"+i;
    }
  }
  
}
