package findCurruptedArchives;

import static findCurruptedArchives.Logging.getLogFileName;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jxl.*;
import jxl.write.*;

/**
 *
 * @author vazhenin
 *
 * args[0] : cmd.exe location args[1] : command itself args[2] : external file
 * with value to be collected
 *
 */
public class FindCurruptedArchives {

    Tools tools;
    String mailHost, username, password;
    String from, to, cc = null, bcc = null, bodyText = new String();
    String directoryToScan, logFile, timeShift;
    File outFile, database;
    FileWriter databaseFw;
    private String searchFolderMask;
    private ArrayList<String> databaseMem = new ArrayList<>();
    WritableWorkbook workbook;
    WritableSheet sheet;
    SheetSettings sheetSettings;
    WritableCellFormat DATE_FORMAT = new WritableCellFormat(new DateFormat("dd.MM.yyyy HH:mm:ss"));
    WritableCellFormat cellFormat;
    int excelRowNum = 0, excelSheetNum = 0;
    CellView autoSizeCellView = new CellView();

    public FindCurruptedArchives(String[] args) {
        loadParameters(args[0]);
//        System.out.println(mailHost);
//        System.out.println(username);
//        System.out.println(password);
//        System.out.println(from);
//        System.out.println(to);
//        System.out.println(cc);
//        System.out.println(bcc);
//        System.out.println(directoryToScan);
//        System.out.println(outFile);
    }

    private static ArrayList<String> curruptedArchives = new ArrayList<>();

    public static void setCurruptedArchives(String curruptedArchiveName) {
        FindCurruptedArchives.curruptedArchives.add(curruptedArchiveName);
    }

    public static ArrayList<String> getCurruptedArchives() {
        return curruptedArchives;
    }

    public void run() {

        try {
            Logging.setLogFileName(this.logFile);
            this.tools = new Tools();
            sendMailSMTP sendMail = new sendMailSMTP(username, password);
            ArrayList<Tools.dirObjectsProperties> dirObjects = new ArrayList<>(); // directory content
            workbook = Workbook.createWorkbook(new File(this.outFile.getAbsolutePath()));
            sheet = workbook.createSheet("Sheet №" + excelSheetNum, excelSheetNum);
            sheetSettings = sheet.getSettings();
            sheetSettings.setVerticalFreeze(1);
            autoSizeCellView.setAutosize(true);
            cellFormat = new WritableCellFormat(new WritableCellFormat(new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD)));
            cellFormat.setAlignment(jxl.format.Alignment.CENTRE);
            cellFormat.setBackground(jxl.format.Colour.AQUA);
            writeTopSheet();
//             
//             initiate file writer session 
//             
            databaseFw = new FileWriter(this.database, true);

            dirObjects = tools.getDirObjects(this.directoryToScan, "cp866");

            Logging.put_log("Scannig directories has began");

            recursiveDirScan(this.directoryToScan, 0);

            Logging.put_log("Scannig directories has finished");

            Logging.put_log("form list of found currupted archives");
//
//             form list of found currupted archives
//
            ArrayList<String> currupted = getCurruptedArchives();
            bodyText = "Check attached file for futher information \n" + bodyText;

            Logging.put_log("Send mail");
//                            
//                close file writers threads
//                       
            databaseFw.flush();
            databaseFw.close();
            // Auto size all columns position
            sheet.setColumnView(0, autoSizeCellView);
            sheet.setColumnView(1, autoSizeCellView);
            sheet.setColumnView(2, autoSizeCellView);
            sheet.setColumnView(3, autoSizeCellView);
            sheet.setColumnView(4, autoSizeCellView);
            // All sheets and cells added. Now write out the workbook            
            workbook.write();
            workbook.close();

            String zipFile = outFile.getAbsolutePath().replace(".xls", "_" + new SimpleDateFormat("dd.MM.yyyy").format(new Date()).toString() + ".zip");
            tools.execCommand("zip -9 " + zipFile + " " + outFile.getAbsolutePath());
            sendMail.sendSMTPMessage(bodyText, from, to, cc, bcc, mailHost, new File(zipFile), "CDR Verifiction");

        } catch (Exception e) {
            e.printStackTrace();
            Logging.put_log(e);
        } finally {
            try {
//                this.outFile.delete();
            } catch (Exception e) {
                Logging.put_log(e);
            }
        }

    }

    void recursiveDirScan(String absolutePath, int Thread) {
        ArrayList<Tools.dirObjectsProperties> archiveObjects = new ArrayList<>(); // archive content
        ArrayList<Tools.dirObjectsProperties> dirFiles = tools.getDirObjects(absolutePath, "cp866");

        for (int i = 0; i < dirFiles.size(); i++) {
            /* if we found directory, we recursively go inside it, and so on */
            if (dirFiles.get(i).type == "dir") {
                recursiveDirScan(dirFiles.get(i).fullPath, 1);
            }/* 
             if we found a file, then check if it's archive of not, 
             by default we check all folders where name contains current year string
             */ else if (dirFiles.get(i).name.indexOf(new SimpleDateFormat("yyyy").format(new Date()).toString()) != -1 && dirFiles.get(i).name.trim().length() > 4) {
                String filename = dirFiles.get(i).name;
                String ext = filename.substring(filename.length() - 4, filename.length());
                if (ext.equals(".zip")) {
                    try {
                        /* if archive is currupted, we register this file */
                        if (isCurrupted(dirFiles.get(i).fullPath)) {
                            bodyText += "\n \n Corrupted files are:\n " + dirFiles.get(i).fullPath + "\n";
                        }/* otherwise we check archive's contents */ else {
                            /* get archive file content */
                            archiveObjects = tools.getArchiveContents(dirFiles.get(i).fullPath, "cp866");
                            for (int j = 0; j < archiveObjects.size(); j++) {
                                /* find archive file archivation date */
                                String temp = archiveObjects.get(j).name;
                                for (int k = 0; k < 6; k++) {
                                    temp = temp.substring(temp.indexOf(";") + 1, temp.length());
                                }
                                Date archiveDate = new SimpleDateFormat("yyyyMMdd.HHmmss").parse(temp.substring(0, temp.indexOf(';')));

                                /* find archive file name */
                                temp = archiveObjects.get(j).name;
                                for (int k = 0; k < 7; k++) {
                                    temp = temp.substring(temp.indexOf(";") + 1, temp.length());
                                }
                                String archiveFileName = temp;

                                /* we process only files we know what their names look like */
//                                    String archiveFileName = archiveFileName.substring(0, 10);
                                if (archiveFileName.toLowerCase().indexOf("p_date") != -1) {
                                    Date cdr_file_date = new SimpleDateFormat("yyyyMMdd_HHmmss").parse(archiveFileName.substring("P_DATA_R9_727_".length(), "P_DATA_R9_727_20150205_000000".length()));
                                    long timeDiff = TimeUnit.MINUTES.convert(archiveDate.getTime() - cdr_file_date.getTime(), TimeUnit.MILLISECONDS);
                                    if (timeDiff > Integer.valueOf(this.timeShift)) {
                                        excelPutRow(archiveObjects.get(j).fullPath, archiveFileName, archiveDate, cdr_file_date, timeDiff);
                                    }
                                } else if (archiveFileName.toLowerCase().indexOf("gsmvis") != -1) {
                                    Date cdr_file_date = new SimpleDateFormat("yyyyMMdd_HHmmss").parse(archiveFileName.substring("gsmvisit_727_".length(), "gsmvisit_727_20150211_000116".length()));
                                    long timeDiff = TimeUnit.MINUTES.convert(archiveDate.getTime() - cdr_file_date.getTime(), TimeUnit.MILLISECONDS);
                                    if (timeDiff > Integer.valueOf(this.timeShift)) {
                                        excelPutRow(archiveObjects.get(j).fullPath, archiveFileName, archiveDate, cdr_file_date, timeDiff);
                                    }
                                } else if (archiveFileName.toLowerCase().indexOf("data_r") != -1) {
                                    Date cdr_file_date = new SimpleDateFormat("yyyyMMdd_HHmmss").parse(archiveFileName.substring("DATA_R7_727_".length(), "DATA_R7_727_20150201_212838".length()));
                                    long timeDiff = TimeUnit.MINUTES.convert(archiveDate.getTime() - cdr_file_date.getTime(), TimeUnit.MILLISECONDS);
                                    if (timeDiff > Integer.valueOf(this.timeShift)) {
                                        excelPutRow(archiveObjects.get(j).fullPath, archiveFileName, archiveDate, cdr_file_date, timeDiff);
                                    }
                                } else if (archiveFileName.toLowerCase().indexOf("_kz_v5") != -1) {
                                    Date cdr_file_date = new SimpleDateFormat("yyyyMMddHHmmss").parse(archiveFileName.substring("ALM_KZ_V5.".length(), "ALM_KZ_V5.20150211000012".length()));
                                    long timeDiff = TimeUnit.MINUTES.convert(archiveDate.getTime() - cdr_file_date.getTime(), TimeUnit.MILLISECONDS);
                                    if (timeDiff > Integer.valueOf(this.timeShift)) {
                                        excelPutRow(archiveObjects.get(j).fullPath, archiveFileName, archiveDate, cdr_file_date, timeDiff);
                                    }
                                } else if (archiveFileName.toLowerCase().indexOf("karaga") != -1) {
                                    Date cdr_file_date = new SimpleDateFormat("yyyyMMddHHmm").parse(archiveFileName.substring("Karaganda_".length(), "Karaganda_201502110407".length()));
                                    long timeDiff = TimeUnit.MINUTES.convert(archiveDate.getTime() - cdr_file_date.getTime(), TimeUnit.MILLISECONDS);
                                    if (timeDiff > Integer.valueOf(this.timeShift)) {
                                        excelPutRow(archiveObjects.get(j).fullPath, archiveFileName, archiveDate, cdr_file_date, timeDiff);
                                    }
                                } else if (archiveFileName.toLowerCase().indexOf("scp06_") != -1) {
                                    Date cdr_file_date = new SimpleDateFormat("yyMMdd_HHmmss").parse(archiveFileName.substring("scp06_".length(), "scp06_110215_011236".length()));
                                    long timeDiff = TimeUnit.MINUTES.convert(archiveDate.getTime() - cdr_file_date.getTime(), TimeUnit.MILLISECONDS);
                                    if (timeDiff > Integer.valueOf(this.timeShift)) {
                                        excelPutRow(archiveObjects.get(j).fullPath, archiveFileName, archiveDate, cdr_file_date, timeDiff);
                                    }
                                } else if (archiveFileName.toLowerCase().indexOf("normal") != -1) {
                                    Date cdr_file_date = new SimpleDateFormat("yyyyMMddHHmm").parse(archiveFileName.substring("Normal_".length(), "Normal_201502010000".length()));
                                    long timeDiff = TimeUnit.MINUTES.convert(archiveDate.getTime() - cdr_file_date.getTime(), TimeUnit.MILLISECONDS);
                                    if (timeDiff > Integer.valueOf(this.timeShift)) {
                                        excelPutRow(archiveObjects.get(j).fullPath, archiveFileName, archiveDate, cdr_file_date, timeDiff);
                                    }
                                } else if (archiveFileName.toLowerCase().indexOf("gsmvoice_727") != -1) {
                                    Date cdr_file_date = new SimpleDateFormat("yyMMdd_HHmmss").parse(archiveFileName.substring("gsmvoice_727_".length(), "gsmvoice_727_20150201_000041".length()));
                                    long timeDiff = TimeUnit.MINUTES.convert(archiveDate.getTime() - cdr_file_date.getTime(), TimeUnit.MILLISECONDS);
                                    if (timeDiff > Integer.valueOf(this.timeShift)) {
                                        excelPutRow(archiveObjects.get(j).fullPath, archiveFileName, archiveDate, cdr_file_date, timeDiff);
                                    }
                                } else {
                                    Logging.put_log("Unkown files in archive " + dirFiles.get(i).fullPath);
                                    break;
                                }
                            }
                            Logging.put_log(dirFiles.get(i).fullPath + " has been processed");

                        }
                    } catch (Exception e) {
                        Logging.put_log("Error processing file " + filename);
                        //e.printStackTrace();
                    } finally {
                    }
                }
            }

        }
    }

    /*
     this procedure checks wether archive is currupted or not
     */
    boolean isCurrupted(String absolutePath) {
        String[] out;
        try {
            out = tools.execCommand("unzip -l " + absolutePath);
            for (int i = 0; i < out.length; i++) {
                String out1 = out[i];
                if (out1.toLowerCase().indexOf("error") != -1) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    class archiveInfo {

        String fullpath, archiveFile, archiveDate, fileDate, dateDiff;

        public archiveInfo(String fullpath, String archiveFile, String archiveDate, String fileDate, String dateDiff) {
            this.fullpath = fullpath;
            this.archiveFile = archiveFile;
            this.archiveDate = archiveDate;
            this.fileDate = fileDate;
            this.dateDiff = dateDiff;
        }
    }

    void loadParameters(String parameterFile) {
        ParseXMLUtilities util = new ParseXMLUtilities(parameterFile);
        util.initiate();
        this.mailHost = util.getNodeValue(util.getChildNodes("parameters"), "mailHost");
        this.username = util.getNodeValue(util.getChildNodes("parameters"), "mailUser");
        this.password = util.getNodeValue(util.getChildNodes("parameters"), "mailPassword");
        this.from = util.getNodeValue(util.getChildNodes("parameters"), "from");
        this.to = util.getNodeValue(util.getChildNodes("parameters"), "to");
        this.cc = util.getNodeValue(util.getChildNodes("parameters"), "cc");
        this.bcc = util.getNodeValue(util.getChildNodes("parameters"), "bcc");
        this.directoryToScan = util.getNodeValue(util.getChildNodes("parameters"), "scanDir");
        this.outFile = new File(util.getNodeValue(util.getChildNodes("parameters"), "outFile"));
        this.logFile = util.getNodeValue(util.getChildNodes("parameters"), "logFile");
        this.timeShift = util.getNodeValue(util.getChildNodes("parameters"), "timeShift");
        this.searchFolderMask = util.getNodeValue(util.getChildNodes("parameters"), "searchFolderMask");
        this.database = new File(util.getNodeValue(util.getChildNodes("parameters"), "database"));

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(database)));
            String line;
            while ((line = br.readLine()) != null) {
                this.databaseMem.add(line);
            }
        } catch (Exception e) {
        }
    }

    boolean isCDRCheckedBefore(String filename) {
        for (int i = 0; i < databaseMem.size(); i++) {
            String get = databaseMem.get(i);
            if (get.toLowerCase().indexOf(filename.toLowerCase()) != -1) {
                return true;
            }
        }
        try {
//            if file wasn't found, we add it to out known database CDR files, so that next time we find it, we consider it known
            this.databaseFw.append(filename + "\n");
        } catch (Exception e) {
        }
        return false;
    }

    void excelPutRow(String fullPath, String archiveFileName, Date archiveDate, Date cdr_file_date, long timeDiff) {
        try {
            /**
             * if CDR file is found in our database it means that it's been
             * processed before and sent via email to list of the recipients
             * Otherwise we check this CDR file
             */
            if (!isCDRCheckedBefore(archiveFileName)) {
                if (excelRowNum == 65000) {
                    excelSheetNum++;
                    excelRowNum = 0;
                    sheet = workbook.createSheet("Sheet №" + excelSheetNum, excelSheetNum);
                    writeTopSheet();
                }
                sheet.addCell(new Label(0, excelRowNum, archiveFileName));
                sheet.addCell(new jxl.write.DateTime(1, excelRowNum, cdr_file_date, DATE_FORMAT));
                sheet.addCell(new jxl.write.DateTime(2, excelRowNum, archiveDate, DATE_FORMAT));
                sheet.addCell(new jxl.write.Number(3, excelRowNum, timeDiff));
                sheet.addCell(new Label(4, excelRowNum, fullPath));
                excelRowNum++;
            }
        } catch (Exception e) {
        }
    }

    void writeTopSheet() {
        try {

            sheet.addCell(new Label(0, excelRowNum, "Имя СДР файла", cellFormat));
            sheet.addCell(new Label(1, excelRowNum, "Дата формирования СДР", cellFormat));
            sheet.addCell(new Label(2, excelRowNum, "Дата архивирования файла", cellFormat));
            sheet.addCell(new Label(3, excelRowNum, "Отставание по времени(минуты)", cellFormat));
            sheet.addCell(new Label(4, excelRowNum, "Полный путь", cellFormat));

            excelRowNum++;
        } catch (Exception e) {
        }
    }
}
