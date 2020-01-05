package findCurruptedArchives;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author vazhenin
 */
public class Logging {

    private static String logFileName = "-1";
    private static FileWriter fw;

    public Logging(String logFileName) {
        setLogFileName(logFileName);
    }

    public static String getLogFileName() {
        return logFileName;
    }

    public static void setLogFileName(String logFileName) {
        Logging.logFileName = logFileName;
    }

    static void put_log(String message) {
        if (Logging.logFileName == "-1") {
            try {
                throw new Exception("logFileName is not set");
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        String newLine = "\r\n";
        try {
            fw = new FileWriter(new File(getLogFileName()), true);
            fw.append(new SimpleDateFormat("dd.mm.yyyy hh:mm:ss").format(new Date()) + " : " + message + newLine);
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    static void put_log(Exception execption) {
        if (Logging.logFileName == "-1") {
            try {
                throw new Exception("logFileName is not set");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        String newLine = "\r\n";
        try {
            fw = new FileWriter(new File(getLogFileName()), true);
            for (int i = 0; i < execption.getStackTrace().length; i++) {
                fw.append(new SimpleDateFormat("dd.mm.yyyy hh:mm:ss").format(new Date()) + " : " + execption.getStackTrace()[i].toString() + newLine);
            }            
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }    
    
    static void writeFile(String filePath, String data) {
        try {
            File f = new File(filePath);
            FileWriter fr = new FileWriter(f);
            fr.write(data);
            fr.close();
        } catch (Exception e) {
            Logging.put_log(e.getMessage());
            System.exit(1);
        }
    }

}
