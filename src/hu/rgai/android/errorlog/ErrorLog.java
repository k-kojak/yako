package hu.rgai.android.errorlog;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Environment;
import android.util.Log;
import hu.rgai.android.config.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tamas Kojedzinszky
 */
public class ErrorLog {
  
  private static final String errorLogPath = "/mLogger/log";
  public enum Reason {FB_CONTACT_SYNC, OTHER};
  
  public static void dumpLogcat(Context context, Reason r) {
    dumpLogcat(context, r, -1, null, null);
  }
  
  public static void dumpLogcat(Context context, Reason r, String fname) {
    dumpLogcat(context, r, -1, fname, null);
  }
  
  public static void dumpLogcat(Context context, Reason r, String fname, String comm) {
    dumpLogcat(context, r, -1, fname, comm);
  }
  
  public static void dumpLogcat(Context context, Reason r, int lastNLines) {
    dumpLogcat(context, r, lastNLines, null, null);
  }
  
  public static void dumpLogcat(Context context, Reason r, int lastNLines, String fname) {
    dumpLogcat(context, r, lastNLines, fname, null);
  }
  
  public static void dumpLogcat(Context context, Reason r, int lastNLines, String fname, String comm) {
    if (Settings.DEBUG) {
      Boolean isSDPresent = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
  //    Boolean isSDPresent = false;
      File mainFilePath = null;
      if (isSDPresent) {
        File sdCard = Environment.getExternalStorageDirectory();
        mainFilePath = new File(sdCard.getAbsolutePath() + errorLogPath);
      } else {
        ContextWrapper cw = new ContextWrapper(context);
        mainFilePath = cw.getDir("media", Context.MODE_PRIVATE);
        mainFilePath = new File(mainFilePath, errorLogPath);
  //      mainFilePath.mkdirs();
      }
  //    Log.d("rgai", mainFilePath.getAbsolutePath());

      if (!mainFilePath.isDirectory()) {
        mainFilePath.mkdirs();
      }
  //    File dir = new File (sdCard.getAbsolutePath() + "/dir1/dir2");
      Date now = new Date();
      Timestamp ts = new Timestamp(now.getTime());
      String tss = new SimpleDateFormat("yyyy-MM-dd-HHmmss-SSSS").format(ts);
      String fileName = r.toString()
              + (fname != null && fname.length() > 0 ? "-" + fname : "")
              + "-" + tss;
      File file = new File(mainFilePath, fileName);
      try {
        file.createNewFile();
        FileOutputStream outs = new FileOutputStream(file);
        PrintWriter pw = new PrintWriter(outs);

        Process process = Runtime.getRuntime().exec("logcat -d -v time");
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        List<String> lines = new ArrayList<String>();
  //      StringBuilder log = new StringBuilder();
        String line = "";
        while ((line = bufferedReader.readLine()) != null) {
          lines.add(line);
        }
        if (comm != null && comm.length() > 0) {
          pw.println(comm);
        }
        int from = 0;
        if (lastNLines != -1) {
          from = Math.max(0, lines.size() - lastNLines);
        }
        for (; from < lines.size(); from++) {
          pw.println(lines.get(from));
        }
        pw.close();
        outs.close();
      } catch (FileNotFoundException ex) {
        Logger.getLogger(ErrorLog.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) { 
        Logger.getLogger(ErrorLog.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}
