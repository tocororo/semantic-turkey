package it.uniroma2.art.semanticturkey.storage;

import java.io.File;

/**
 * Information about a directory entry
 *
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class DirectoryEntryInfo {
    public enum EntryType {
        FILE, DIRECTORY
    }
    private String name;
    private EntryType type;

    public String getName() {
        return name;
    }

    public EntryType getType() {
        return type;
    }

    public static DirectoryEntryInfo create(File file) {
        DirectoryEntryInfo info = new DirectoryEntryInfo();
        info.name = file.getName();
        info.type = file.isDirectory() ? EntryType.DIRECTORY : EntryType.FILE;
        return info;
    }
}
