package it.uniroma2.art.semanticturkey.extension.extpts.reformattingexporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;

/**
 * This class represents the output of a {@link ReformattingExporter}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ClosableFormattedResource implements AutoCloseable {

	private File backingFile;
	private String defaultFileExtension;
	private String mimeType;

	public ClosableFormattedResource(File backingFile, String defaultFileExtension, String mimeType) {
		this.backingFile = backingFile;
		this.defaultFileExtension = defaultFileExtension;
		this.mimeType = mimeType;
	}

	public String getDefaultFileExtension() {
		return defaultFileExtension;
	}

	public String getMIMEType() {
		return mimeType;
	}

	public void writeTo(OutputStream outputStream) throws IOException {
		FileUtils.copyFile(backingFile, outputStream);
	}

	public InputStream getInputStream() throws IOException {
		return new FileInputStream(backingFile);
	}

	@Override
	public void close() {
		if (backingFile != null) {
			backingFile.delete();
		}
	}

	public static ClosableFormattedResource build(Consumer<OutputStream> outputStreamConsumer,
			String defaultFileExtension, String mimeType) throws IOException {
		File backingFile = File.createTempFile("reformattingexporter", null);
		try (OutputStream os = new FileOutputStream(backingFile)) {
			outputStreamConsumer.accept(os);
		}
		return new ClosableFormattedResource(backingFile, defaultFileExtension, mimeType);
	}

}
