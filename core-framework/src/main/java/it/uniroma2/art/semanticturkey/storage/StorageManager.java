package it.uniroma2.art.semanticturkey.storage;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.user.STUser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.servlet.ServletOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Manages server-side storage of content
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class StorageManager {

    private static final String STORAGE_FOLDER = "storage";

    public static final Pattern PATH_VALIDATION_PATTERN = Pattern.compile("^((/(?!\\.)[a-zA-Z0-9_\\-.,\\s]+)+|/)$");

    public static File getSystemStorageDirectory() {
        return FileUtils.getFile(Resources.getSystemDir(), STORAGE_FOLDER);
    }

    public static File getProjectStorageDirectory(Project project) {
        return FileUtils.getFile(Resources.getProjectsDir(), project.getName(), STORAGE_FOLDER);
    }

    public static File getUserStorageDirectory(STUser user) {
        return FileUtils.getFile(Resources.getUsersDir(), STUser.encodeUserIri(user.getIRI()), STORAGE_FOLDER);
    }

    public static File getPUStorageDirectory(Project project, STUser user) {
        return FileUtils.getFile(Resources.getProjectUserBindingsDir(), project.getName(), STUser.encodeUserIri(user.getIRI()), STORAGE_FOLDER);
    }

    public static File getStorageDirectory(Reference ref) {
        Project project = ref.getProject().orElse(null);
        STUser user = ref.getUser().orElse(null);

        File storageDirectory;
        if (project == null && user == null) {
            storageDirectory = getSystemStorageDirectory();
        } else if (project != null) {
            if (user != null) {
                storageDirectory = getPUStorageDirectory(project, user);
            } else {
                storageDirectory = getProjectStorageDirectory(project);
            }
        } else {
            storageDirectory = getUserStorageDirectory(user);
        }

        return storageDirectory;
    }

    public static File getFile(Reference ref) {
        File storageDirectory = getStorageDirectory(ref);

        String path = ref.getIdentifier();
        validatePath(path);

        return new File(storageDirectory, path.substring(1)); // removes the leading slash from the path
    }

    private static void validatePath(String path) {
        if (!PATH_VALIDATION_PATTERN.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

    }

    /**
     * Lists the entries in a given directory
     * @param dir a reference to the directory
     * @return
     */
    public static Collection<DirectoryEntryInfo> list(Reference dir) {
        File dirFile = getFile(dir);
        if (!dirFile.exists()) {
            return Collections.emptyList();
        }
        return Arrays.stream(ArrayUtils.nullToEmpty(dirFile.listFiles(), File[].class))
                .map(DirectoryEntryInfo::create)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new directory
     * @param dir a reference to the directory
     * @return
     */
    public static void createDirectory(Reference dir) {
        File dirFile = getFile(dir);
        dirFile.mkdirs();
    }

    /**
     * Creates a new directory, if it does now exist.
     * Returns true if the directory was created, false otherwise
     * @param dir a reference to the directory
     * @return true if the directory was created, false otherwise
     */
    public static boolean createDirectoryIfNotExisting(Reference dir) {
        File dirFile = getFile(dir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
            return true;
        }
        return false;

    }

    /**
     * Deletes a directory
     * @param dir a reference to the directory
     * @return
     */
    public static void deleteDirectory(Reference dir) throws IOException {
        File dirFile = getFile(dir);
        FileUtils.deleteDirectory(dirFile);
    }

    /**
     * Creates a file. Fails if the file already exists, unless <code>overwrite</code> is <code>true</code>
     * @param is an input stream for the content of the file
     * @param ref a reference to the file
     * @param overwrite
     * @return
     */
    public static void createFile(InputStream is, Reference ref, boolean overwrite) throws IOException {
        File file = getFile(ref);
        file.getParentFile().mkdirs();
        CopyOption[] options = overwrite ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING} : new CopyOption[0];
        Files.copy(is, file.toPath(), options);
    }


    /**
     * Deletes a file
     * @param ref a reference to the file
     * @return
     */
    public static void deleteFile(Reference ref) throws IOException {
        File file = getFile(ref);
        if (!file.isFile()) throw new IllegalArgumentException("Not a file: " + file);
        boolean rv = file.delete();
        if (!rv) throw new IOException("Could not delete the file: " + file);
    }

    /**
     * Returns the content of a file
     * @param os an output stream the which the content of the file will be written to
     * @param ref a reference to the file
     * @param setContentLength an optional (may be <code>null</code>) consumer that will be invoked to indicate the content size
     * @return
     */
    public static void getFileContent(OutputStream os, Reference ref, IntConsumer setContentLength) throws IOException {
        File file = getFile(ref);
        if (setContentLength != null) {
            setContentLength.accept(Math.toIntExact(file.length()));
        }

        FileUtils.copyFile(file, os);
    }

    /**
     * Returns the content of a file
     * @param os an output stream the which the content of the file will be written to
     * @param ref a reference to the file
     * @return
     */
    public static void getFileContent(OutputStream os, Reference ref) throws IOException {
        getFileContent(os, ref, null);
    }

    /**
     * Returns the content of a file
     * @param ref a reference to the file
     * @return
     */
    public static InputStream getFileContent(Reference ref) throws IOException {
        File file = getFile(ref);
        return new FileInputStream(file);
    }

    /**
     * Returns whether the referenced file exists
     * @param ref
     * @return
     */
    public static boolean exists(Reference ref) {
        File file = getFile(ref);
        return file.exists() && file.isFile();
    }
}
