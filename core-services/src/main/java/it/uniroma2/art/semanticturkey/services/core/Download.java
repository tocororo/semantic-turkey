package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.coda.converters.commons.DateTimeUtils;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.settings.download.DownloadProjectSettings;
import it.uniroma2.art.semanticturkey.settings.download.DownloadSettingsManager;
import it.uniroma2.art.semanticturkey.settings.download.SingleDownload;
import it.uniroma2.art.semanticturkey.storage.DirectoryEntryInfo;
import it.uniroma2.art.semanticturkey.storage.StorageManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */

@STService
public class Download  extends STServiceAdapter {

    private final String DOWNLOAD_DIR_NAME = "download";
    private final String PROJ = "proj:";

    private static Logger logger = LoggerFactory.getLogger(Download.class);

    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    @Read
    public void createDownload(String fileName, String localized, String lang, RDFFormat format,
                               @Optional(defaultValue = "true") boolean zipFile) throws IOException, NoSuchSettingsManager {
        // check if the DOWNLOAD_DIR_NAME exist in the project folder, if not, create it
        checkAndInCaseCreateFolder();

        String extSuffix = getExtensionFromFormat(format);

        // add the FILE_FORMAT_SUFFIX to the fileName, if not already present
        fileName = fileName.endsWith(extSuffix) ? fileName : fileName+extSuffix;

        Reference ref = parseReference(PROJ +"/"+DOWNLOAD_DIR_NAME+"/"+fileName);

        File file = StorageManager.getFile(ref);
        try (OutputStream out = new FileOutputStream(file)) {
            RDFHandler rdfHandler = Rio.createWriter(format, out);
            getManagedConnection().export(rdfHandler, getWorkingGraph());
        }

        File resultFile = file;
        // zip the file
        if (zipFile) {
            Reference refZip = parseReference(PROJ + "/" + DOWNLOAD_DIR_NAME + "/" + fileName + ".zip");
            File zippedFile = StorageManager.getFile(refZip);
            try (OutputStream out = new FileOutputStream(zippedFile)) {
                try (ZipOutputStream zout = new ZipOutputStream(out)) {
                    //zout.setLevel(9);
                    ZipEntry ze = new ZipEntry(file.getName());
                    zout.putNextEntry(ze);
                    FileInputStream fin = new FileInputStream(file);
                    byte[] buffer = new byte[4096];
                    for (int n; (n = fin.read(buffer)) > 0; ) {
                        zout.write(buffer, 0, n);
                    }
                    fin.close();
                }
            }
            resultFile = zippedFile;

            // delete the old file (so, only the zip version remain)
            file.delete();
        }

        // update the YAML file
        try {
            DownloadProjectSettings downloadProjectSettings = (DownloadProjectSettings) exptManager.getSettings(getProject(),
                    null, null, DownloadSettingsManager.class.getName(), Scope.PROJECT);
            if (downloadProjectSettings.fileNameToSingleDownloadMap == null ) {
                downloadProjectSettings.fileNameToSingleDownloadMap = new HashMap<>();
            }
            Map<String, SingleDownload> fileNameToSingleDownloadMap = downloadProjectSettings.fileNameToSingleDownloadMap;


            Map<String, String> langToLocalizedMap = new HashMap<>();
            if(!localized.isEmpty() && !lang.isEmpty()) {
                langToLocalizedMap.put(lang, localized);
            }
            String date = Long.toString(new Date().getTime());
            SingleDownload singleDownload = new SingleDownload();
            singleDownload.fileName = resultFile.getName();
            singleDownload.timestamp = date;
            singleDownload.langToLocalizedMap = langToLocalizedMap;
            singleDownload.format = format.getName();

            fileNameToSingleDownloadMap.put(resultFile.getName(), singleDownload);


            ObjectMapper mapper = new ObjectMapper();
            ObjectNode objectNode = mapper.valueToTree(downloadProjectSettings);

            exptManager.storeSettings(DownloadSettingsManager.class.getName(), getProject(), null, null,
                    Scope.PROJECT, objectNode);

        } catch (STPropertyAccessException | STPropertyUpdateException | WrongPropertiesException e) {
            throw new RuntimeException(e); // this should not happen
        }

    }

    @STServiceOperation
    @PreAuthorize("@auth.isAdmin()")
    public List<RDFFormat> getAvailableFormats() {
        List<RDFFormat> availableFormatsList = new ArrayList<>();

        // add the supported formats
        availableFormatsList.add(RDFFormat.RDFXML);
        availableFormatsList.add(RDFFormat.NTRIPLES);
        availableFormatsList.add(RDFFormat.N3);
        availableFormatsList.add(RDFFormat.NQUADS);

        return availableFormatsList;
    }

    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void removeDownload(String fileName) throws IOException, NoSuchSettingsManager {

        // remove the file fileName
        String filePath = PROJ +"/"+DOWNLOAD_DIR_NAME+"/"+fileName;
        //Reference ref = parseReference(filePath);
        //StorageManager.deleteFile(ref);
        deleteFile(filePath);

        // update the YAML file
        removeSettingsEntry(fileName);
        /*
        try {
            DownloadProjectSettings downloadProjectSettings = (DownloadProjectSettings) exptManager.getSettings(getProject(),
                    null, null, DownloadSettingsManager.class.getName(), Scope.PROJECT);
            if (downloadProjectSettings.fileNameToSingleDownloadMap == null ) {
                downloadProjectSettings.fileNameToSingleDownloadMap = new HashMap<>();
            }
            Map<String, SingleDownload> fileNameToSingleDownloadMap = downloadProjectSettings.fileNameToSingleDownloadMap;
            if (!fileNameToSingleDownloadMap.containsKey(fileName)) {
                // the desired fileName does not exist in the config file, so return;
                return;
            }

            fileNameToSingleDownloadMap.remove(fileName);

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode objectNode = mapper.valueToTree(downloadProjectSettings);

            exptManager.storeSettings(DownloadSettingsManager.class.getName(), getProject(), null, null,
                    Scope.PROJECT, objectNode);


        } catch (STPropertyAccessException | STPropertyUpdateException | WrongPropertiesException e) {
            throw new RuntimeException(e); // this should not happen
        }
         */

    }


    @STServiceOperation
    //@PreAuthorize("@auth.isAdmin()") // TODO decide the right @PreAuthorize
    @Read
    public DownloadProjectSettings getDownloadInfoList() throws NoSuchSettingsManager, IOException {
        // check if the DOWNLOAD_DIR_NAME exist in the project folder, if not, create it
        checkAndInCaseCreateFolder();


        // get the list of the download
        List<String> fileList = new ArrayList<>();
        Reference ref = parseReference(PROJ +"/"+DOWNLOAD_DIR_NAME);
        Collection<DirectoryEntryInfo> directoryEntryInfoCollection = StorageManager.list(ref);
        for(DirectoryEntryInfo directoryEntryInfo : directoryEntryInfoCollection) {
            String fileName = directoryEntryInfo.getName();
            fileList.add(fileName);
        }

        // get the list of fileName from the settings file
        List<String> fileInSettingList = new ArrayList<>();
        try {
            DownloadProjectSettings downloadProjectSettings = (DownloadProjectSettings) exptManager.getSettings(getProject(),
                    null, null, DownloadSettingsManager.class.getName(), Scope.PROJECT);
            if (downloadProjectSettings.fileNameToSingleDownloadMap == null ) {
                downloadProjectSettings.fileNameToSingleDownloadMap = new HashMap<>();
            }
            fileInSettingList.addAll(downloadProjectSettings.fileNameToSingleDownloadMap.keySet());
        } catch (STPropertyAccessException e) {
            throw new RuntimeException(e); // this should not happen
        }

        // do the clean up
        for (String fileName : fileList) {
            if(!fileInSettingList.contains(fileName)) {
                String filePath = PROJ +"/"+DOWNLOAD_DIR_NAME+"/"+fileName;
                deleteFile(filePath);
            }
        }
        for (String fileName : fileInSettingList) {
            if (!fileList.contains(fileName)) {
                removeSettingsEntry(fileName);
            }
        }


        try {
            DownloadProjectSettings downloadProjectSettings = (DownloadProjectSettings) exptManager.getSettings(getProject(),
                    null, null, DownloadSettingsManager.class.getName(), Scope.PROJECT);
            if (downloadProjectSettings.fileNameToSingleDownloadMap == null ) {
                downloadProjectSettings.fileNameToSingleDownloadMap = new HashMap<>();
            }

            return downloadProjectSettings;


        } catch (STPropertyAccessException e) {
            throw new RuntimeException(e); // this should not happen
        }

    }

    /**
     * Downloads a file
     * @param oRes the response object to which the file will be written to
     * @param fileName the name of the file to download from this project
     * @return
     */
    @STServiceOperation
    //@PreAuthorize("@auth.isAdmin()") // TODO decide the right @PreAuthorize
    @Read
    public void getFile(HttpServletResponse oRes, String fileName) throws IOException {
        String filePath = PROJ +"/"+DOWNLOAD_DIR_NAME+"/"+fileName;
        Reference ref = parseReference(filePath);
        StorageManager.getFileContent(oRes.getOutputStream(), ref, oRes::setContentLength);
    }


    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    @Read
    public void updateLocalized(String fileName, String localized, String lang) throws NoSuchSettingsManager {

        try {
            DownloadProjectSettings downloadProjectSettings = (DownloadProjectSettings) exptManager.getSettings(getProject(),
                    null, null, DownloadSettingsManager.class.getName(), Scope.PROJECT);

            if (downloadProjectSettings.fileNameToSingleDownloadMap == null ) {
                downloadProjectSettings.fileNameToSingleDownloadMap = new HashMap<>();
            }
            Map<String, SingleDownload> fileNameToSingleDownloadMap = downloadProjectSettings.fileNameToSingleDownloadMap;
            if (!fileNameToSingleDownloadMap.containsKey(fileName)) {
                // the desired fileName does not exist in the config file, so return;
                return;
            }
            SingleDownload singleDownload = fileNameToSingleDownloadMap.get(fileName);
            singleDownload.langToLocalizedMap.put(lang, localized);

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode objectNode = mapper.valueToTree(downloadProjectSettings);

            exptManager.storeSettings(DownloadSettingsManager.class.getName(), getProject(), null, null,
                    Scope.PROJECT, objectNode);


        } catch (STPropertyAccessException | STPropertyUpdateException | WrongPropertiesException e) {
            throw new RuntimeException(e); // this should not happen
        }

    }


    // this function check if there is the DOWNLOAD_DIR_NAME in the project folder, if, not, such directory is created
    private void checkAndInCaseCreateFolder(){
        Reference refDir = parseReference(PROJ +"/"+DOWNLOAD_DIR_NAME);
        boolean created = StorageManager.createDirectoryIfNotExisting(refDir);
        // TODO decide whether to use the information that the directory did not existed before or not
    }

    private String getExtensionFromFormat(RDFFormat format) {
        if (RDFFormat.RDFXML.equals(format)) {
            return ".rdf";
        } else if (RDFFormat.NTRIPLES.equals(format)) {
            return ".nt";
        } else if (RDFFormat.N3.equals(format)) {
            return ".n3";
        } else if (RDFFormat.NQUADS.equals(format)) {
            return ".nq";
        } else {
            return ".rdf";
        }
    }

    private void deleteFile(String filePath) throws IOException {
        Reference ref = parseReference(filePath);
        StorageManager.deleteFile(ref);
    }

    private void removeSettingsEntry(String fileName) throws NoSuchSettingsManager {
        try {
            DownloadProjectSettings downloadProjectSettings = (DownloadProjectSettings) exptManager.getSettings(getProject(),
                    null, null, DownloadSettingsManager.class.getName(), Scope.PROJECT);
            if (downloadProjectSettings.fileNameToSingleDownloadMap == null ) {
                downloadProjectSettings.fileNameToSingleDownloadMap = new HashMap<>();
            }
            Map<String, SingleDownload> fileNameToSingleDownloadMap = downloadProjectSettings.fileNameToSingleDownloadMap;
            if (!fileNameToSingleDownloadMap.containsKey(fileName)) {
                // the desired fileName does not exist in the config file, so return;
                return;
            }

            fileNameToSingleDownloadMap.remove(fileName);

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode objectNode = mapper.valueToTree(downloadProjectSettings);

            exptManager.storeSettings(DownloadSettingsManager.class.getName(), getProject(), null, null,
                    Scope.PROJECT, objectNode);


        } catch (STPropertyAccessException | STPropertyUpdateException | WrongPropertiesException e) {
            throw new RuntimeException(e); // this should not happen
        }
    }
}
