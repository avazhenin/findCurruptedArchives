package findCurruptedArchives;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by anatoliyvazhenin on 10/27/14.
 */
public class Tools {

    int osType; /* 0 - linux; 1 - windows; 2 os x*/


    public Tools() {
        identifyOS();
    }

    void identifyOS() {
        osType = 0;
        String os_name = System.getProperty("os.name");
        if (os_name.toLowerCase().indexOf("linux") != -1) {
            osType = 0;
        }
        if (os_name.toLowerCase().indexOf("windows") != -1) {
            osType = 1;
        }
        if (os_name.toLowerCase().indexOf("mac os x") != -1) {
            osType = 2;
        }
    }

    String getCurrentDate(String format) {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat(format);
        Date date = new Date();
        return df.format(date).toString();
    }

    String getDateMinus(String format, int minute) {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat(format);

        // initiate Date variable
        Date date = new Date();

        // subtract certain amount of minutes
        date.setTime(date.getTime() - (minute * 60 * 1000));

        return df.format(date).toString();
    }

    void unzipFile(File file, String unzipDir, String password) {
        try {
            System.out.println(file.getAbsoluteFile());
            ZipFile zipFile = new ZipFile(file.getAbsoluteFile());
            zipFile.extractAll(unzipDir);
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    String[] execCommand(String command) {
        ArrayList<String> output = new ArrayList<String>();
        BufferedReader br = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "cmd.exe", "/c", command);
            builder.redirectErrorStream(true);
            Process p = builder.start();
            br = new BufferedReader(new InputStreamReader(p.getInputStream(), "cp866"));
            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                output.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toArray(new String[output.size()]);
    }

    ArrayList<dirObjectsProperties> getDirObjects(String path, String charset) {
        String cmd = null;
        ArrayList<dirObjectsProperties> objectProps = null;
        if (osType == 2) {
            cmd = "ls " + path;
        }
        if (osType == 1) {
            cmd = "c:\\windows\\system32\\cmd.exe /c dir /B " + path;
        }
        try {
            Runtime run = Runtime.getRuntime();
            Process p = run.exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), charset));
            objectProps = new ArrayList<dirObjectsProperties>();
            String line = br.readLine();
            while (line != null) {
                File object = new File(path + "\\" + line);
                objectProps.add(new dirObjectsProperties(object.getName(), ((object.isFile()) ? "file" : "dir"), (double) (object.length()), object.getAbsolutePath()));
                line = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return objectProps;
    }

    ArrayList<dirObjectsProperties> getArchiveContents(String path, String charset) {
        String cmd = null;
        ArrayList<dirObjectsProperties> objectProps = null;
        if (osType == 2) {
            cmd = "ls " + path;
        }
        if (osType == 1) {
            cmd = "c:\\windows\\system32\\cmd.exe /c unzip -Z -T " + path;
        }
        try {
            Runtime run = Runtime.getRuntime();
            Process p = run.exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), charset));
            objectProps = new ArrayList<dirObjectsProperties>();
            String line = br.readLine();
            ArrayList<String> archive_content_temp = new ArrayList<>();
            // read archive content into temporary variable for parsing
            while (line != null) {
                //System.out.println(line);
                archive_content_temp.add(line);
                line = br.readLine();
            }
            /* Parse content, remove header and total lines */
            for (int i = 1; i < archive_content_temp.size() - 1; i++) {
                String fixed_line = archive_content_temp.get(i);
                /* replace gaps with ; sign */
                for (int j = 10; j >= 0; j--) {
                    String gap = "";
                    for (int k = 0; k <= j; k++) {
                        gap += " ";
                    }
                    fixed_line = fixed_line.replace(gap, ";");
                }
                objectProps.add(new dirObjectsProperties(fixed_line, "file", 0, path));
//                System.out.println(fixed_line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return objectProps;
    }

    class dirObjectsProperties {

        String name, type, fullPath;
        double size;

        public dirObjectsProperties(String name, String type, double size, String fullPath) {
            this.name = name;
            this.type = type;
            this.size = size;
            this.fullPath = fullPath;
        }
    }

}
