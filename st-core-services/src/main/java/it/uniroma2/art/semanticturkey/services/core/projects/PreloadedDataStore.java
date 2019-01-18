package it.uniroma2.art.semanticturkey.services.core.projects;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.Striped;

import it.uniroma2.art.semanticturkey.validation.ValidationUtilities.ThrowingConsumer;

/**
 * A singleton component that is in charge of storing preloaded data, while assuring that they will be
 * eventually deleted. The current implementation does not guarantee the deletion of the stored data in case
 * of abrupt termination of the program. For this case, it relies on the automatic clearing of the temporary
 * directory where this store places its data.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
@Component
public class PreloadedDataStore {

	private static final int PRELOADED_DATA_DELETION_DELAY = 30; // minutes
	private static final int LOCK_STRIPES = 5;
	private final String DIRECTORY_PREFIX = PreloadedDataStore.class.getName();
	private File storeDirectory;
	private AtomicLong counter;
	private ScheduledExecutorService deletionExecutor;
	private Striped<Lock> lockProvider = Striped.lock(LOCK_STRIPES);

	public PreloadedDataStore() throws IOException {
		storeDirectory = Files.createTempDirectory(DIRECTORY_PREFIX).toFile();
		deletionExecutor = Executors.newSingleThreadScheduledExecutor();
		counter = new AtomicLong();
	}

	public <T extends Exception> File preloadData(ThrowingConsumer<File, T> preloadingTask)
			throws T, IOException {
		File preloadedDataFile = new File(storeDirectory,
				counter.getAndIncrement() + "-" + UUID.randomUUID().toString() + ".tmp");
		preloadedDataFile.createNewFile();
		try {
			preloadingTask.accept(preloadedDataFile);
			deletionExecutor.schedule(fileDeletionTask(preloadedDataFile), PRELOADED_DATA_DELETION_DELAY,
					TimeUnit.MINUTES);
		} catch (Exception e) {
			FileUtils.deleteQuietly(preloadedDataFile);
			throw e;
		}

		return preloadedDataFile;
	}

	private Runnable fileDeletionTask(File preloadedDataFile) {
		return () -> {
			Lock lock = lockProvider.get(preloadedDataFile.getName());
			lock.lock();
			try {

				if (preloadedDataFile.exists()) {
					preloadedDataFile.delete();
				}
			} finally {
				lock.unlock();
			}
		};
	}

	public <T extends Exception> void consumePreloadedData(String preloadedDataFileName,
			ThrowingConsumer<File, T> consumerTask) throws T, IOException {
		File preloadedDataFile = startConsumingPreloadedData(preloadedDataFileName);
		boolean deleteFile = false;
		try {
			consumerTask.accept(preloadedDataFile);
			deleteFile = true;
		} finally {
			finishConsumingPreloadedData(preloadedDataFileName, deleteFile);
		}
	}

	public File startConsumingPreloadedData(String preloadedDataFileName) throws IOException {
		File preloadedDataFile = new File(storeDirectory, preloadedDataFileName);

		ReentrantLock lock = (ReentrantLock) lockProvider.get(preloadedDataFileName);
		lock.lock();
		try {
			if (!preloadedDataFile.exists()) {
				throw new FileNotFoundException(preloadedDataFileName
						+ ": not corresponds to any preloaded data file. Maybe it has been deleted because a "
						+ PRELOADED_DATA_DELETION_DELAY + " minutes delay since its creation is elapsed");
			}

			if (!preloadedDataFile.isFile()) {
				throw new IOException(preloadedDataFileName + ": not a normal file");
			}

			return preloadedDataFile;
		} catch (Exception e) {
			lock.unlock();
			throw e;
		}
	}

	public void finishConsumingPreloadedData(String preloadedDataFileName, boolean deleteFile) {
		File preloadedDataFile = new File(storeDirectory, preloadedDataFileName);

		ReentrantLock lock = (ReentrantLock) lockProvider.get(preloadedDataFileName);
		if (!lock.isHeldByCurrentThread()) {
			throw new IllegalStateException(
					"Current thread does not hold the lock on the preloaded data store");
		}
		try {
			if (deleteFile) {
				FileUtils.deleteQuietly(preloadedDataFile);
			}
		} finally {
			lock.unlock();
		}
	}

}
