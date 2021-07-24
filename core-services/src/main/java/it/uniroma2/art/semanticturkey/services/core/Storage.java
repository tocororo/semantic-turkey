package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.config.Configuration;
import it.uniroma2.art.semanticturkey.config.ConfigurationManager;
import it.uniroma2.art.semanticturkey.config.ConfigurationNotFoundException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchConfigurationManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.STServiceContext;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.storage.DirectoryEntryInfo;
import it.uniroma2.art.semanticturkey.storage.StorageManager;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * This class provides services for storing content.
 */
@STService
public class Storage extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Storage.class);

	/**
	 * Lists the entries in a given directory
	 * @param dir a relative reference to the directory (e.g. sys:/a/b/c.txt)
	 * @return
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public Collection<DirectoryEntryInfo> list(String dir) {
		Reference ref = parseReference(dir);
		return StorageManager.list(ref);
	}

	/**
	 * Creates a new directory
	 * @param dir a relative reference to the directory (e.g. sys:/a/b/c.txt)
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void createDirectory(String dir) {
		Reference ref = parseReference(dir);
		StorageManager.createDirectory(ref);
	}

	/**
	 * Deletes a directory
	 * @param dir a relative reference to the directory (e.g. sys:/a/b/c.txt)
	 * @return
	 * @throws IOException
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void deleteDirectory(String dir) throws IOException {
		Reference ref = parseReference(dir);
		StorageManager.deleteDirectory(ref);
	}

	/**
	 * Creates a file. Fails if the file already exists, unless <code>overwrite</code> is <code>true</code>
	 * @param data the content of the file
	 * @param path a relative reference to the file (e.g. sys:/a/b/c.txt)
	 * @param overwrite
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void createFile(MultipartFile data, String path, @Optional  boolean overwrite) throws IOException {
		Reference ref = parseReference(path);
		try (InputStream is = data.getInputStream()) {
			StorageManager.createFile(is, ref, overwrite);
		}
	}

	/**
	 * Deletes a file
	 * @param path a relative reference to the file (e.g. sys:/a/b/c.txt)
	 * @return
	 */
	@STServiceOperation(method = RequestMethod.POST)
	@PreAuthorize("@auth.isAdmin()")
	public void deleteFile(String path) throws IOException {
		Reference ref = parseReference(path);
		StorageManager.deleteFile(ref);
	}

	/**
	 * Downloads a file
	 * @param oRes the response object to which the file will be written to
	 * @param path a relative reference to the file (e.g. sys:/a/b/c.txt)
	 * @return
	 */
	@STServiceOperation
	@PreAuthorize("@auth.isAdmin()")
	public void getFile(HttpServletResponse oRes, String path) throws IOException {
		Reference ref = parseReference(path);
		StorageManager.getFileContent(oRes.getOutputStream(), ref, oRes::setContentLength);
	}

}