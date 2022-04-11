package it.uniroma2.art.semanticturkey.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

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
    private long creationTimestamp;

    public String getName() {
        return name;
    }

    public EntryType getType() {
        return type;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public static DirectoryEntryInfo create(File file) {
        DirectoryEntryInfo info = new DirectoryEntryInfo();
        info.name = file.getName();
        info.type = file.isDirectory() ? EntryType.DIRECTORY : EntryType.FILE;

        try {
            BasicFileAttributes attributes = Files.readAttributes(Paths.get(file.toURI()), BasicFileAttributes.class);
            info.creationTimestamp = attributes.creationTime().toMillis();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return info;
    }
}
