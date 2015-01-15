package nl.sense_os.service.debug;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.os.Environment;

public class OutputUtils {
  
//TODO:remove before release
  public static void appendLog(String text)
  {       
     File logFile = new File( Environment.getExternalStorageDirectory(),"SenseLibLog.txt");
     if (!logFile.exists())
     {
        try
        {
           logFile.createNewFile();
        } 
        catch (IOException e)
        {
           e.printStackTrace();
        }
     }
     try
     {
        //BufferedWriter for performance, true to set append to file flag
        BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
        
        buf.append( getFormattedDateMonthYearHourMinutes( System.currentTimeMillis() ) + " : " +  text);
        buf.newLine();
        buf.close();
     }
     catch (IOException e)
     {
        e.printStackTrace();
     }
  }
  
  public static String getFormattedDateMonthYearHourMinutes( long stamp ){
    Calendar c = getCalendar( stamp );
    SimpleDateFormat dateformat = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
    
    return dateformat.format(c.getTime());
  }
  
  private static synchronized Calendar getCalendar( long stamp ) {
    Calendar cal = new GregorianCalendar();
    cal.setFirstDayOfWeek( Calendar.MONDAY );
    cal.setTimeInMillis( stamp );
    return cal;
  }
  
}
