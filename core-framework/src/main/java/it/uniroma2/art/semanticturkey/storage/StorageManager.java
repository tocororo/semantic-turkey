package it.uniroma2.art.semanticturkey.storage;

import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.user.STUser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages server-side storage of content
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public abstract class StorageManager {

    private static final String STORAGE_FOLDER = "storage";

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

    private static File getFile(Reference ref) {
        File storageDirectory = getStorageDirectory(ref);

        String path = ref.getIdentifier();
        String[] pathComponents;
        if (StringUtils.isAllBlank(path)) {
            pathComponents = new String[0];
        } else {
            pathComponents = path.split("/");
        }
        validatePathComponents(pathComponents);
        return FileUtils.getFile(storageDirectory, pathComponents);
    }

    private static void validatePathComponents(String[] pathComponents) {
        for (String s : pathComponents) {
            if (s.isEmpty()) {
                throw new IllegalArgumentException("Empty string not allowed as path component");
            } else if (s.startsWith(".")) {
                throw  new IllegalArgumentException("Path component not allowed to start with a dot");
            } else if (!s.matches("[a-z0-9_.]+")) {
                throw new IllegalArgumentException("Malformed path component");
            }
        }
    }

    /**
     * Lists the entries in a given directory
     * @param dir a relative reference to the directory (see {@link Reference#getRelativeReference()})
     * @return
     */
    public static List<DirectoryEntryInfo> list(Reference dir) {
        File dirFile = getFile(dir);
        return Arrays.stream(ArrayUtils.nullToEmpty(dirFile.listFiles(), File[].class))
                .map(DirectoryEntryInfo::create)
                .collect(Collectors.toList());
    }

}
