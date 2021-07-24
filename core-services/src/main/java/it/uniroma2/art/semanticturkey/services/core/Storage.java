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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
	 * @param dir a relative reference to the directory (see {@link Reference#getRelativeReference()})
	 * @return
	 */
	@STServiceOperation
	public List<DirectoryEntryInfo> list(String dir) {
		Reference ref = parseReference(dir);
		return StorageManager.list(ref);
	}


	/**
	 * Creates a new directory
	 * @param dir a relative reference to the directory (see {@link Reference#getRelativeReference()})
	 * @return
	 */
	@STServiceOperation
	public void createDirectory(String dir) {
		throw new NotImplementedException();
	}

	/**
	 * Deletes a directory
	 * @param dir a relative reference to the directory (see {@link Reference#getRelativeReference()})
	 * @return
	 */
	@STServiceOperation
	public void deleteDirectory(String dir) {
		throw new NotImplementedException();
	}

	/**
	 * Creates a file
	 * @param data the content of the file
	 * @param path a relative reference to the file (see {@link Reference#getRelativeReference()})
	 * @return
	 */
	@STServiceOperation
	public void createFile(MultipartFile data, String path) {
		throw new NotImplementedException();
	}

	/**
	 * Deletes a file
	 * @param path a relative reference to the file (see {@link Reference#getRelativeReference()})
	 * @return
	 */
	@STServiceOperation
	public void deleteFile(MultipartFile data, String path) {
		throw new NotImplementedException();
	}

	/**
	 * Downloads a file
	 * @param oRes the response object to which the file will be written to
	 * @param path a relative reference to the file (see {@link Reference#getRelativeReference()})
	 * @return
	 */
	@STServiceOperation
	public void getFile(HttpServletResponse oRes, String path) {
		throw new NotImplementedException();
	}

}